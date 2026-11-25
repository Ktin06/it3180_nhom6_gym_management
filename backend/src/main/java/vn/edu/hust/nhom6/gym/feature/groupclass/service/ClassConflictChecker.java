package vn.edu.hust.nhom6.gym.feature.groupclass.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.groupclass.entity.ClassSchedule;
import vn.edu.hust.nhom6.gym.feature.groupclass.entity.ClassSession;
import vn.edu.hust.nhom6.gym.feature.groupclass.entity.ScheduleSlot;
import vn.edu.hust.nhom6.gym.feature.groupclass.repository.ClassScheduleRepository;
import vn.edu.hust.nhom6.gym.feature.groupclass.repository.ClassSessionRepository;
import vn.edu.hust.nhom6.gym.feature.groupclass.repository.GroupClassLookupRepository;
import vn.edu.hust.nhom6.gym.feature.groupclass.repository.GroupClassLookupRepository.PtSessionRow;

/**
 * Tìm xung đột phòng và huấn luyện viên (BR-F8.7-06): chỉ chặn khi có <b>buổi thực tế</b> chồng giờ, gồm lịch ACTIVE
 * khác, buổi lớp đã sinh chưa hủy và buổi PT đã đặt (NF6). Chỉ xét từ hiện tại trở đi; trùng trong quá khứ bỏ qua.
 * Trả câu mô tả xung đột đầu tiên (để nêu trong lỗi 409 hoặc lý do bỏ qua khi sinh buổi), rỗng nếu không trùng.
 */
@Component
@RequiredArgsConstructor
class ClassConflictChecker {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    /** Lịch không có ngày kết thúc thì tìm buổi đến mốc rất xa; buổi tương lai thực tế chỉ có trong vài tuần. */
    private static final int OPEN_END_YEARS = 100;
    private static final long NO_SCHEDULE = -1L;
    /** Không xét phòng hoặc người dạy này trong {@link #forSession} (không có id nào âm). */
    static final Long NONE = -1L;

    private final ClassScheduleRepository schedules;
    private final ClassSessionRepository sessions;
    private final GroupClassLookupRepository lookup;
    private final VnTime vnTime;

    /**
     * Xung đột của lịch lặp {@code slot} khi tạo ({@code scheduleId} rỗng) hoặc sửa lịch {@code scheduleId}.
     * Khi sửa, bỏ qua chính lịch đó và buổi đã sinh của nó: buổi cũ giữ nguyên, lịch mới không sinh lại ngày đã có.
     */
    Optional<String> forSchedule(ScheduleSlot slot, Long scheduleId) {
        LocalDate from = later(slot.validFrom(), vnTime.today());
        if (slot.validTo() != null && slot.validTo().isBefore(from)) {
            return Optional.empty();
        }
        long exclude = scheduleId == null ? NO_SCHEDULE : scheduleId;
        OffsetDateTime windowStart = vnTime.now();
        OffsetDateTime windowEnd = slot.validTo() == null
            ? windowStart.plusYears(OPEN_END_YEARS) : vnTime.dayEnd(slot.validTo());

        Stream<String> withSchedules = schedules
            .findActiveSameDayUsing(slot.dayOfWeek(), slot.roomId(), slot.trainerId(), exclude).stream()
            .filter(other -> slot.timeOverlaps(other.getStartTime(), other.getEndTime()))
            .filter(other -> shareAWeekday(slot, from, other))
            .map(other -> describe(slot, other));
        Stream<String> withSessions = sessions
            .findBusy(slot.roomId(), slot.trainerId(), exclude, windowStart, windowEnd).stream()
            .filter(session -> hitsOccurrence(slot, from, scheduleId, session.getStartAt(), session.getEndAt()))
            .map(session -> describe(slot, session));
        Stream<String> withPt = lookup.findBookedPtSessions(slot.trainerId(), windowStart, windowEnd).stream()
            .filter(pt -> hitsOccurrence(slot, from, scheduleId, pt.startAt(), pt.endAt()))
            .map(this::describe);
        return Stream.of(withSchedules, withSessions, withPt).flatMap(s -> s).findFirst();
    }

    /**
     * Xung đột của một buổi sắp sinh {@code [start, end)} của lịch {@code schedule} với buổi lớp khác chưa hủy và buổi
     * PT đã đặt (AC-F8.8.1-05).
     */
    Optional<String> forOccurrence(ClassSchedule schedule, OffsetDateTime start, OffsetDateTime end) {
        ScheduleSlot slot = schedule.slot();
        Optional<String> session = sessions
            .findBusy(slot.roomId(), slot.trainerId(), schedule.getId(), start, end).stream()
            .findFirst().map(s -> describe(slot, s));
        if (session.isPresent()) {
            return session;
        }
        return lookup.findBookedPtSessions(slot.trainerId(), start, end).stream().findFirst().map(this::describe);
    }

    /**
     * Xung đột nếu buổi {@code sessionId} dùng phòng {@code roomId}, người dạy {@code trainerId} trong {@code [start, end)}
     * (đổi giờ, đổi phòng một buổi; BR-F8.7-06, AC-F8.7.1-09, 10). Xét theo thứ tự:
     * <ol>
     *   <li>buổi lớp khác chưa hủy (bỏ qua chính buổi này);</li>
     *   <li>lịch ACTIVE có lần lặp chồng giờ vào một ngày Việt Nam từ ngày bắt đầu đến ngày kết thúc của giờ mới mà
     *       <b>chưa sinh buổi</b> cho ngày đó (đã sinh thì bước 1 đã xét buổi thật, kể cả khi buổi đó đã đổi giờ hoặc
     *       đã hủy);</li>
     *   <li>buổi PT đã đặt của người dạy (NF6).</li>
     * </ol>
     * Truyền {@link #NONE} cho phòng hoặc người dạy không cần xét (đổi phòng chỉ xét phòng).
     */
    Optional<String> forSession(Long sessionId, Long roomId, Long trainerId, OffsetDateTime start,
            OffsetDateTime end) {
        Stream<String> withSessions = sessions.findBusyExceptSession(roomId, trainerId, sessionId, start, end).stream()
            .map(other -> describe(roomId, other));
        Stream<String> withSchedules = vnTime.toVnDate(start).datesUntil(vnTime.toVnDate(end).plusDays(1))
            .flatMap(day -> schedules
                .findActiveSameDayUsing(day.getDayOfWeek().getValue(), roomId, trainerId, NO_SCHEDULE).stream()
                .filter(other -> other.slot().occursOn(day))
                .filter(other -> vnTime.at(day, other.getStartTime()).isBefore(end)
                    && start.isBefore(vnTime.at(day, other.getEndTime())))
                .filter(other -> !sessions.existsByClassScheduleIdAndOccurrenceDate(other.getId(), day))
                .map(other -> describe(roomId, other)));
        Stream<String> withPt = NONE.equals(trainerId) ? Stream.empty()
            : lookup.findBookedPtSessions(trainerId, start, end).stream().map(this::describe);
        return Stream.of(withSessions, withSchedules, withPt).flatMap(s -> s).findFirst();
    }

    /**
     * Giờ dạy lớp nhóm của huấn luyện viên {@code trainerId} chồng với {@code [start, end)} (F9 đặt buổi PT, BR-F9.3):
     * buổi lớp chưa hủy được phân công cho người này, hoặc lần lặp của lịch ACTIVE <b>ngoài cửa sổ sinh buổi</b> (từ hôm
     * nay + {@link ClassSessionGenerationService#DAYS_AHEAD} trở đi) chưa sinh buổi cho ngày đó. Trong cửa sổ, tác vụ
     * sinh đã chạy: ngày không có buổi là ngày bị bỏ qua (phòng ngừng, trùng...), lớp không diễn ra nên không chặn.
     * Không xét buổi PT (F9 tự xét). Chỉ đọc, không khóa {@code class_sessions}: nơi gọi đã giữ khóa dòng
     * {@code staff}, mà đổi giờ buổi lớp khóa {@code staff} trước khi kiểm tra buổi PT, nên hai luồng không lọt nhau và
     * không khóa chéo.
     */
    Optional<String> forTrainerTeaching(Long trainerId, OffsetDateTime start, OffsetDateTime end) {
        Stream<String> withSessions = sessions.findBusyExceptSession(NONE, trainerId, NONE, start, end).stream()
            .map(other -> describe(NONE, other));
        LocalDate firstUngenerated = vnTime.today().plusDays(ClassSessionGenerationService.DAYS_AHEAD);
        Stream<String> withSchedules = vnTime.toVnDate(start).datesUntil(vnTime.toVnDate(end).plusDays(1))
            .filter(day -> !day.isBefore(firstUngenerated))
            .flatMap(day -> schedules
                .findActiveSameDayUsing(day.getDayOfWeek().getValue(), NONE, trainerId, NO_SCHEDULE).stream()
                .filter(other -> other.slot().occursOn(day))
                .filter(other -> vnTime.at(day, other.getStartTime()).isBefore(end)
                    && start.isBefore(vnTime.at(day, other.getEndTime())))
                .filter(other -> !sessions.existsByClassScheduleIdAndOccurrenceDate(other.getId(), day))
                .map(other -> describe(NONE, other)));
        return Stream.of(withSessions, withSchedules).flatMap(s -> s).findFirst();
    }

    private String describe(Long roomId, ClassSession other) {
        return "Trùng %s với buổi #%d (%s).".formatted(roomId.equals(other.getRoomId()) ? "phòng" : "huấn luyện viên",
            other.getId(), span(other.getStartAt(), other.getEndAt()));
    }

    private static String describe(Long roomId, ClassSchedule other) {
        return "Trùng %s với lịch #%d (%s %s–%s).".formatted(
            roomId.equals(other.getRoomId()) ? "phòng" : "huấn luyện viên", other.getId(),
            dayName(other.getDayOfWeek()), hhmm(other.getStartTime()), hhmm(other.getEndTime()));
    }

    /** Hai lịch cùng thứ có ít nhất một ngày chung trong phần hiệu lực từ {@code from} trở đi. */
    private static boolean shareAWeekday(ScheduleSlot slot, LocalDate from, ClassSchedule other) {
        LocalDate start = later(from, other.getValidFrom());
        LocalDate end = earlier(slot.validTo(), other.getValidTo());
        return end == null || !slot.firstWeekdayOnOrAfter(start).isAfter(end);
    }

    /**
     * Buổi {@code [start, end)} chồng với lần lặp của lịch vào ngày Việt Nam của nó (xét cả ngày bắt đầu và kết thúc).
     * Khi sửa lịch {@code ownScheduleId}, bỏ qua ngày lịch đó đã có buổi: buổi cũ giữ nguyên, giờ mới không áp dụng.
     */
    private boolean hitsOccurrence(ScheduleSlot slot, LocalDate from, Long ownScheduleId, OffsetDateTime start,
            OffsetDateTime end) {
        return Stream.of(vnTime.toVnDate(start), vnTime.toVnDate(end)).distinct()
            .filter(day -> !day.isBefore(from) && slot.occursOn(day))
            .filter(day -> ownScheduleId == null
                || !sessions.existsByClassScheduleIdAndOccurrenceDate(ownScheduleId, day))
            .anyMatch(day -> vnTime.at(day, slot.startTime()).isBefore(end)
                && start.isBefore(vnTime.at(day, slot.endTime())));
    }

    private static String describe(ScheduleSlot slot, ClassSchedule other) {
        return "Trùng %s với lịch #%d (%s %s–%s).".formatted(resource(slot, other.getRoomId()), other.getId(),
            dayName(other.getDayOfWeek()), hhmm(other.getStartTime()), hhmm(other.getEndTime()));
    }

    /** Thứ ISO sang cách gọi tiếng Việt: 1 → "thứ 2" … 6 → "thứ 7", 7 → "Chủ nhật". */
    static String dayName(int isoDayOfWeek) {
        return isoDayOfWeek == 7 ? "Chủ nhật" : "thứ " + (isoDayOfWeek + 1);
    }

    private String describe(ScheduleSlot slot, ClassSession other) {
        return "Trùng %s với buổi #%d (%s).".formatted(resource(slot, other.getRoomId()), other.getId(),
            span(other.getStartAt(), other.getEndAt()));
    }

    private String describe(PtSessionRow pt) {
        return "Trùng buổi PT #%d của huấn luyện viên (%s).".formatted(pt.id(), span(pt.startAt(), pt.endAt()));
    }

    private static String resource(ScheduleSlot slot, Long otherRoomId) {
        return slot.roomId().equals(otherRoomId) ? "phòng" : "huấn luyện viên";
    }

    private String span(OffsetDateTime start, OffsetDateTime end) {
        var vnStart = start.atZoneSameInstant(VnTime.ZONE);
        var vnEnd = end.atZoneSameInstant(VnTime.ZONE);
        return vnStart.format(DATE) + " " + vnStart.format(TIME) + "–" + vnEnd.format(TIME);
    }

    private static String hhmm(LocalTime time) {
        return time.format(TIME);
    }

    private static LocalDate later(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    /** Ngày sớm hơn; rỗng nghĩa là không giới hạn nên bị bên kia thay. */
    private static LocalDate earlier(LocalDate a, LocalDate b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.isBefore(b) ? a : b;
    }
}
