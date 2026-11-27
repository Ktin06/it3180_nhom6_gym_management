package vn.edu.hust.nhom6.gym.feature.groupclass.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vn.edu.hust.nhom6.gym.common.exception.BusinessRuleException;
import vn.edu.hust.nhom6.gym.common.exception.ConflictException;
import vn.edu.hust.nhom6.gym.common.exception.ForbiddenException;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.groupclass.dto.ClassDtos.Session;
import vn.edu.hust.nhom6.gym.feature.groupclass.dto.ClassDtos.SessionTimeRequest;
import vn.edu.hust.nhom6.gym.feature.groupclass.event.ClassSessionChanged;
import vn.edu.hust.nhom6.gym.feature.groupclass.event.ClassSessionChanged.Kind;
import vn.edu.hust.nhom6.gym.feature.groupclass.event.ClassSessionChanged.Slot;
import vn.edu.hust.nhom6.gym.feature.groupclass.repository.ClassRepository;
import vn.edu.hust.nhom6.gym.feature.staff.service.StaffService;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ClassSessionService {
    static final Set<String> OPEN = Set.of("SCHEDULED", "RESCHEDULED");

    private final ClassRepository classes;
    private final StaffService staff;
    private final VnTime time;
    private final ApplicationEventPublisher events;

    public Session changeTime(CurrentUser actor, long id, SessionTimeRequest request) {
        Session session = lockForManagement(actor, id);
        OffsetDateTime now = time.now();
        requireUpcoming(session, now);
        if (!request.startAt().isAfter(now) || !request.endAt().isAfter(request.startAt())) {
            throw new BusinessRuleException("Giờ bắt đầu phải ở tương lai và giờ kết thúc phải sau giờ bắt đầu.");
        }
        if (request.startAt().isEqual(session.startAt()) && request.endAt().isEqual(session.endAt())) {
            return session;
        }
        requireResourcesFree(session, session.roomId(), request.startAt(), request.endAt());
        classes.reschedule(id, request.startAt(), request.endAt());
        return changed(Kind.TIME_CHANGED, session, actor, now, classes.bookedMemberIds(id));
    }

    public Session changeRoom(CurrentUser actor, long id, long roomId) {
        Session session = lockForManagement(actor, id);
        OffsetDateTime now = time.now();
        requireUpcoming(session, now);
        if (session.roomId() == roomId) {
            return session;
        }
        var room = classes.room(roomId);
        if (!room.branchId().equals(session.branchId())) {
            throw new ForbiddenException("Phòng không thuộc chi nhánh của buổi học.");
        }
        if (!"ACTIVE".equals(room.status()) || room.capacity() < session.capacity()) {
            throw new BusinessRuleException("Phòng phải hoạt động và đủ sức chứa cho buổi học.");
        }
        requireResourcesFree(session, roomId, session.startAt(), session.endAt());
        classes.changeRoom(id, roomId);
        return changed(Kind.ROOM_CHANGED, session, actor, now, classes.bookedMemberIds(id));
    }

    public Session cancel(CurrentUser actor, long id) {
        Session session = lockForManagement(actor, id);
        if ("CANCELLED".equals(session.status())) {
            return session;
        }
        OffsetDateTime now = time.now();
        requireUpcoming(session, now);
        var members = classes.bookedMemberIds(id);
        classes.cancelSession(id, now);
        return changed(Kind.CANCELLED, session, actor, now, members);
    }

    private Session lockForManagement(CurrentUser actor, long id) {
        Session observed = classes.session(id, false);
        if (!actor.isOwner() && !(actor.is(UserRole.BRANCH_MANAGER)
                && staff.requireBranchId(actor.userId()).equals(observed.branchId()))) {
            throw new ForbiddenException("Bạn không có quyền quản lý buổi học ở chi nhánh này.");
        }
        // Cùng thứ tự với sinh buổi và đặt buổi PT (NF6): branches -> staff -> class_sessions.
        classes.lockBranch(observed.branchId());
        classes.lockTrainer(observed.trainerId());
        return classes.session(id, true);
    }

    private static void requireUpcoming(Session session, OffsetDateTime now) {
        if (!OPEN.contains(session.status()) || !session.startAt().isAfter(now)) {
            throw new BusinessRuleException("Chỉ buổi chưa bắt đầu mới có thể thay đổi hoặc hủy.");
        }
    }

    private void requireResourcesFree(Session session, long roomId, OffsetDateTime start, OffsetDateTime end) {
        if (classes.resourceConflict(session.trainerId(), roomId, start, end, session.id())) {
            throw new ConflictException("Trùng lịch phòng hoặc huấn luyện viên với buổi lớp hoặc buổi PT.");
        }
    }

    private Session changed(Kind kind, Session before, CurrentUser actor, OffsetDateTime now, List<Long> members) {
        Session after = classes.session(before.id(), false);
        var event = new ClassSessionChanged(kind, after.id(), after.branchId(), after.trainerId(), actor.userId(),
            now, slot(before), slot(after), members);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    events.publishEvent(event);
                } catch (RuntimeException ex) {
                    log.error("Không xử lý được sự kiện {} của buổi {}", kind, after.id(), ex);
                }
            }
        });
        return after;
    }

    private static Slot slot(Session session) {
        return new Slot(session.roomId(), session.startAt(), session.endAt());
    }
}
