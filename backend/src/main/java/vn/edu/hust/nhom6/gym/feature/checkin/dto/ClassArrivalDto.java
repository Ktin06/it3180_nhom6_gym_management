package vn.edu.hust.nhom6.gym.feature.checkin.dto;

import java.time.OffsetDateTime;

import vn.edu.hust.nhom6.gym.feature.checkin.repository.CheckinLookupRepository.ClassRow;

/** Lớp nhóm của hội viên để lễ tân nhắc, ví dụ "lớp Yoga tại Phòng 201 lúc 18:00". {@code startTime} là UTC. */
public record ClassArrivalDto(Long bookingId, Long sessionId, String className, String roomName,
        OffsetDateTime startTime) {

    public static ClassArrivalDto of(ClassRow row) {
        return new ClassArrivalDto(row.bookingId(), row.sessionId(), row.className(), row.roomName(), row.startAt());
    }
}
