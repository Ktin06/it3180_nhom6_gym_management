package vn.edu.hust.nhom6.gym.feature.groupclass.event;

import java.time.OffsetDateTime;
import java.util.List;

public record ClassSessionChanged(
    Kind kind, long sessionId, long branchId, long trainerId, long actorUserId,
    OffsetDateTime changedAt, Slot before, Slot after, List<Long> affectedMemberIds
) {
    public ClassSessionChanged {
        affectedMemberIds = List.copyOf(affectedMemberIds);
    }

    public enum Kind { TIME_CHANGED, ROOM_CHANGED, CANCELLED }

    public record Slot(long roomId, OffsetDateTime startAt, OffsetDateTime endAt) {}
}
