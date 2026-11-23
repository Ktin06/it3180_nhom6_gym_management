package vn.edu.hust.nhom6.gym.feature.systemconfig.service;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.hust.nhom6.gym.feature.systemconfig.repository.SystemConfigRepository;

/**
 * Đọc tham số cấu hình (quy tắc mã 15: tham số có dấu * không viết cứng).
 *
 * <p>Thứ tự tìm: dòng của chi nhánh {@code branchId} trước, không có thì dòng toàn chuỗi
 * ({@code branch_id} rỗng). Thiếu cả hai, hoặc giá trị sai kiểu, là lỗi cấu hình của hệ thống
 * (không phải lỗi người dùng) nên ném {@link IllegalStateException}, trả 500.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SystemConfigService {
    private final SystemConfigRepository configs;

    /**
     * Giá trị dạng chuỗi.
     *
     * @param branchId chi nhánh đang xét; {@code null} thì chỉ đọc giá trị toàn chuỗi
     * @param key tên khóa, dùng hằng trong {@link SystemConfigKeys}
     * @throws IllegalStateException khi cả dòng chi nhánh lẫn dòng toàn chuỗi đều không có
     */
    public String getString(Long branchId, String key) {
        Optional<String> branchValue = branchId == null ? Optional.empty() : configs.findBranchValue(branchId, key);
        return branchValue
            .or(() -> configs.findChainValue(key))
            .orElseThrow(() -> new IllegalStateException("Thiếu tham số cấu hình " + key));
    }

    /**
     * Giá trị dạng số nguyên (số ngày, số phút, số lần, tiền VND).
     *
     * @throws IllegalStateException khi thiếu khóa hoặc giá trị không phải số nguyên
     */
    public int getInt(Long branchId, String key) {
        String value = getString(branchId, key);
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Tham số cấu hình " + key + " không phải số nguyên: " + value, e);
        }
    }
}
