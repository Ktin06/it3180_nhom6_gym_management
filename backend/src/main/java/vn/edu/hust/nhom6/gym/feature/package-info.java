/**
 * Mã nghiệp vụ, đóng gói theo chức năng (xem docs/decisions/0007-cau-truc-ma-nguon.md).
 *
 * <p>Mỗi chức năng một gói con, ví dụ {@code auth} (F1), {@code member} (F2), {@code membership} (F3),
 * {@code checkin} (F4), {@code payment} (F5), {@code dashboard} (F6), {@code booking} (F8),
 * {@code trainer} (F9), {@code notification} (F10)... Bên trong mỗi gói chia theo tầng:
 *
 * <pre>
 * feature/member/
 * ├── controller/   REST controller, tiền tố /api/v1, chỉ nhận request và gọi service
 * ├── service/      quy tắc nghiệp vụ, giao dịch (@Transactional); mỗi quy tắc có kiểm thử JUnit
 * ├── repository/   Spring Data JPA repository
 * ├── entity/       entity JPA, kế thừa common.entity.BaseEntity, trạng thái dùng @Enumerated(EnumType.STRING)
 * ├── dto/          request (có Bean Validation) và response
 * └── mapper/       chuyển entity sang dto và ngược lại
 * </pre>
 *
 * <p>Gói chức năng chỉ gọi gói khác qua service, không gọi thẳng repository của chức năng khác.
 */
package vn.edu.hust.nhom6.gym.feature;
