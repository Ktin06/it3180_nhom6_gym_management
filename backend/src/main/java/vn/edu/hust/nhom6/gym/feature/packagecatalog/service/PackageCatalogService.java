package vn.edu.hust.nhom6.gym.feature.packagecatalog.service;

import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.annotation.Validated;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.nhom6.gym.common.dto.PageResponse;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.common.exception.ConflictException;
import vn.edu.hust.nhom6.gym.common.exception.NotFoundException;
import vn.edu.hust.nhom6.gym.common.exception.BusinessRuleException;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.dto.PackageDeactivationDto;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.dto.UpdatePackageRequest;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.dto.CreatePackageRequest;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.dto.PackageDetailDto;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.GymPackage;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.dto.PackageSummaryDto;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageStatus;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.mapper.PackageMapper;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.repository.PackageRepository;
import vn.edu.hust.nhom6.gym.feature.staff.service.StaffService;

@Service
@RequiredArgsConstructor
@Validated
@Transactional(readOnly = true)
public class PackageCatalogService {
    private final PackageRepository packages;
    private final StaffService staff;
    private final Validator validator;

    @Transactional
    public PackageDeactivationDto deactivate(CurrentUser me, @NotNull @Positive Long id) {
        if (!me.is(UserRole.BRANCH_MANAGER)) {
            throw new AccessDeniedException("Bạn không có quyền thực hiện thao tác này.");
        }
        Long branchId = staff.requireBranchId(me.userId());
        var item = findOwnedPackageForUpdate(id, branchId);
        if (item.getStatus() == PackageStatus.INACTIVE) {
            throw new BusinessRuleException("Gói tập này đã ở trạng thái ngừng bán.");
        }
        item.deactivate();
        packages.flush();
        return PackageMapper.deactivation(item);
    }

    @Transactional
    public PackageDetailDto update(CurrentUser me, @Positive Long id, @NotNull @Valid UpdatePackageRequest request) {
        if (!me.is(UserRole.BRANCH_MANAGER)) {
            throw new AccessDeniedException("Bạn không có quyền thực hiện thao tác này.");
        }
        Long branchId = staff.requireBranchId(me.userId());
        var item = findOwnedPackageForUpdate(id, branchId);
        var merged = new CreatePackageRequest(item.getCode(),
            request.has("name") ? request.getName() : item.getName(), item.getType(),
            request.has("durationDays") ? request.getDurationDays() : item.getDurationDays(),
            request.has("sessionCount") ? request.getSessionCount() : item.getSessionCount(),
            request.has("price") ? request.getPrice() : item.getPrice(),
            request.has("description") ? request.getDescription() : item.getDescription());
        var errors = validator.validate(merged);
        if (!errors.isEmpty()) { throw new ConstraintViolationException(errors); }
        item.updateDetails(merged.name(), merged.durationDays(), merged.sessionCount(), merged.price(), merged.description());
        // Hibernate dirty checking: body rỗng/giá trị như cũ không phát UPDATE hoặc đổi audit.
        packages.flush();
        return PackageMapper.detail(item);
    }

    public PageResponse<PackageSummaryDto> list(CurrentUser me, PackageType type,
            PackageStatus status, int page, int size) {
        boolean allBranches = me.isOwner();
        if (!allBranches && !me.is(UserRole.BRANCH_MANAGER) && !me.is(UserRole.RECEPTIONIST)) {
            throw new AccessDeniedException("Bạn không có quyền thực hiện thao tác này.");
        }
        Long branchId = allBranches ? null : staff.requireBranchId(me.userId());
        return PageResponse.of(packages.search(allBranches, branchId, type, status,
            PageRequest.of(page, size, Sort.by("type", "name", "id"))), PackageMapper::summary);
    }

    @Transactional
    public PackageDetailDto create(CurrentUser me, @NotNull @Valid CreatePackageRequest request) {
        if (!me.is(UserRole.BRANCH_MANAGER)) {
            throw new AccessDeniedException("Bạn không có quyền thực hiện thao tác này.");
        }
        Long branchId = staff.requireBranchId(me.userId());
        if (packages.existsByCode(request.code())) { throw duplicateCode(request.code()); }
        var item = GymPackage.create(branchId, request.code(), request.name(), request.type(),
            request.durationDays(), request.sessionCount(), request.price(), request.description());
        try {
            return PackageMapper.detail(packages.saveAndFlush(item));
        } catch (DataIntegrityViolationException ex) {
            // Flush để bắt UNIQUE ngay trong service; không truy vấn/thử lại trong transaction đã lỗi.
            for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
                if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                        && "uq_packages_code".equals(violation.getConstraintName())) {
                    throw duplicateCode(request.code());
                }
            }
            throw ex;
        }
    }

    private GymPackage findOwnedPackageForUpdate(Long id, Long branchId) {
        var item = packages.findForUpdate(id)
            .orElseThrow(() -> new NotFoundException("Gói tập không tồn tại trên hệ thống."));
        if (!branchId.equals(item.getBranchId())) {
            throw new AccessDeniedException("Bạn không có quyền thực hiện thao tác này.");
        }
        return item;
    }

    private ConflictException duplicateCode(String code) {
        return new ConflictException("Mã gói '" + code + "' đã tồn tại trên hệ thống.");
    }
}
