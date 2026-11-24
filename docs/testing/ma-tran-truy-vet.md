# Ma trận truy vết UC → US → AC → TC

> **File sinh tự động, không sửa tay.** Chạy `node scripts/export-truy-vet.cjs` ở gốc repo để sinh lại.
> Nguồn: mã UC, US, AC, BR trong `docs/requirements/srs/`; mã TC trong tên kiểm thử backend (JUnit `@DisplayName`)
> và frontend (Vitest `it`, `test`, `describe`). Quy ước và cách dùng: [README](README.md), [ADR 0012](../decisions/0012-quy-uoc-ma-truy-vet.md).

## 1. Tổng quan theo chức năng

| Chức năng | UC | US | AC | AC đã có TC | AC chưa có TC | TC |
| --- | --- | --- | --- | --- | --- | --- |
| F1 | 6 | 0 | 0 | 0 | 0 | 0 |
| F2 | 5 | 0 | 0 | 0 | 0 | 0 |
| F3 | 5 | 6 | 45 | 43 | 2 | 106 |
| F4 | 5 | 0 | 0 | 0 | 0 | 0 |
| F5 | 5 | 0 | 0 | 0 | 0 | 0 |
| F6 | 5 | 0 | 0 | 0 | 0 | 0 |
| F7 | 5 | 0 | 0 | 0 | 0 | 0 |
| F8 | 9 | 0 | 0 | 0 | 0 | 0 |
| F9 | 7 | 0 | 0 | 0 | 0 | 0 |
| F10 | 5 | 0 | 0 | 0 | 0 | 0 |
| **Tổng** | **57** | **6** | **45** | **43** | **2** | **106** |

Chức năng có US = 0 là chức năng chưa viết lại SRS theo ADR 0012 (chưa có mã US, AC).

## 2. Lỗi cần sửa

### 2.1. TC trỏ tới AC không tồn tại hoặc đã bỏ

Không có.

### 2.2. Mã AC bị trùng trong SRS

Không có.

### 2.3. US không nằm dưới UC nào

Không có.

## 3. AC chưa có TC

Một US chỉ đủ điều kiện nghiệm thu và đóng khi không còn AC nào của nó trong bảng này ([CONTRIBUTING.md, mục 5](../../CONTRIBUTING.md#5-định-nghĩa-hoàn-thành)).

| AC | Tên | US |
| --- | --- | --- |
| AC-F3.1.1-03 | Lễ tân chỉ xem, không có thao tác quản lý trên giao diện | US-F3.1.1 |
| AC-F3.5.1-09 | Hóa đơn tại quầy không tự hủy | US-F3.5.1 |

## 4. Cây xuôi: chức năng → UC → US → AC → TC

### F3

- **UC-F3.1** Xem danh mục gói tập
  - **US-F3.1.1** Xem danh mục gói (nội bộ) (#129)
    - AC-F3.1.1-01 Nhân viên chi nhánh chỉ thấy gói của chi nhánh mình và gói toàn chuỗi (BR-F3-01)
      - TC-F3.1.1-01a `PackageCatalogIntegrationTest#scopesRowsAndCountAndIgnoresClientBranch`
    - AC-F3.1.1-02 Lọc, sắp xếp và phân trang
      - TC-F3.1.1-02a `PackageCatalogIntegrationTest#defaultActiveAndOrderingMatchContract`
      - TC-F3.1.1-02b `PackageCatalogIntegrationTest#inactiveFilterAndEmptyResults`
      - TC-F3.1.1-02c `PackageCatalogIntegrationTest#maximumSizeAndPagePastEndAreValid`
      - TC-F3.1.1-02d `PackageControllerTest#defaultContract`
      - TC-F3.1.1-02e `features/packages/pages/PackagesPage.test.tsx › TC-F3.1.1-02e: mặc định lấy gói đang bán; chọn loại PT giữ n`
      - TC-F3.1.1-02f `features/packages/api/packages.test.ts › TC-F3.1.1-02f: gửi bộ lọc và phân trang đúng endpoint đã đặc`
    - AC-F3.1.1-03 Lễ tân chỉ xem, không có thao tác quản lý trên giao diện ❌ chưa có TC
    - AC-F3.1.1-04 Chủ chuỗi xem gói mọi chi nhánh
      - TC-F3.1.1-04a `PackageCatalogIntegrationTest#ownerSeesOtherBranchesWithoutStaff`
      - TC-F3.1.1-04b `PackageCatalogServiceTest#ownerDoesNotRequireStaffAssignment`
    - AC-F3.1.1-05 Từ chối người không được xem
      - TC-F3.1.1-05a `PackageCatalogIntegrationTest#inactiveOrMissingStaffIsForbidden`
      - TC-F3.1.1-05b `PackageControllerTest#rejectsDeniedRole`
      - TC-F3.1.1-05c `PackageControllerTest#missingAndInvalidBearerAreUnauthorized`
      - TC-F3.1.1-05d `PackageCatalogServiceTest#missingAssignmentNeverFallsBackToAllBranches`
    - AC-F3.1.1-06 Tham số lọc sai
      - TC-F3.1.1-06a `PackageControllerTest#rejectsInvalidQueryWithFieldErrors`
  - **US-F3.1.2** Bảng giá công khai (#130) ✅ mọi AC có TC
    - AC-F3.1.2-01 Khách xem bảng giá không cần đăng nhập (BR-F3-01)
      - TC-F3.1.2-01a `PublicPackageIntegrationTest#guestSeesActiveBranchAndGlobalPackagesInDisplayOrder`
      - TC-F3.1.2-01b `features/packages/pages/PublicPricingPage.test.tsx › TC-F3.1.2-01b: hiện giá từ server, thời hạn hoặc số buổi, qu`
      - TC-F3.1.2-01c `features/packages/pages/PublicPricingPage.test.tsx › TC-F3.1.2-01c: gọi API công khai không kèm access token và k`
      - TC-F3.1.2-01d `PublicPackageIntegrationTest#staleOrForgedTokenIsIgnoredOnPublicRead`
    - AC-F3.1.2-02 Không hiện gói ngừng bán và gói chi nhánh khác (BR-F3-01)
      - TC-F3.1.2-02a `PublicPackageIntegrationTest#hidesInactiveAndOtherBranchPackages`
    - AC-F3.1.2-03 Không lộ trường nội bộ
      - TC-F3.1.2-03a `PublicPackageIntegrationTest#exposesOnlyDisplayFields`
    - AC-F3.1.2-04 Chưa có gói đang bán
      - TC-F3.1.2-04a `PublicPackageIntegrationTest#emptyCatalogReturnsEmptyList`
      - TC-F3.1.2-04b `features/packages/pages/PublicPricingPage.test.tsx › TC-F3.1.2-04b: chưa có gói đang bán thì báo trống, không hiệ`
    - AC-F3.1.2-05 Lỗi tải bảng giá
      - TC-F3.1.2-05a `features/packages/pages/PublicPricingPage.test.tsx › TC-F3.1.2-05a: lỗi tải thì báo lỗi, không hiện giá; bấm Thử `
    - AC-F3.1.2-06 Chi nhánh mặc định ngừng hoạt động
      - TC-F3.1.2-06a `PublicPackageIntegrationTest#inactiveDefaultBranchReturnsEmptyList`
- **UC-F3.2** Thêm gói tập mới
  - **US-F3.2.1** Thêm gói tập (#131) ✅ mọi AC có TC
    - AC-F3.2.1-01 Thêm gói hợp lệ của từng loại (BR-F3-02, BR-F3-03)
      - TC-F3.2.1-01a `PackageCreateIntegrationTest#createsAllTypesWithServerBranchAuditAndReadVisibility`
    - AC-F3.2.1-02 Số buổi đúng theo loại gói (BR-F3-02)
      - TC-F3.2.1-02a `PackageCreateIntegrationTest#sessionsAreRequiredAndPositive`
    - AC-F3.2.1-03 Dữ liệu sai bị từ chối, giá trị biên được nhận
      - TC-F3.2.1-03a `PackageCreateIntegrationTest#invalidInputsReturnFieldErrorsWithoutWrites`
      - TC-F3.2.1-03b `PackageCreateIntegrationTest#missingFieldsMalformedJsonAndLengthLimitsAreRejected`
      - TC-F3.2.1-03c `PackageCreateIntegrationTest#acceptsZeroPriceAndMaximumLengthsAndIntegers`
    - AC-F3.2.1-04 Không nhận trường do server quản lý
      - TC-F3.2.1-04a `PackageCreateIntegrationTest#rejectsServerManagedAndUnknownFields`
    - AC-F3.2.1-05 Mã gói không trùng
      - TC-F3.2.1-05a `PackageCreateIntegrationTest#duplicateInactiveGlobalCodeIsConflict`
      - TC-F3.2.1-05b `PackageCreateIntegrationTest#concurrentDuplicateCodeCreatesOneAndRollsBackOther`
    - AC-F3.2.1-06 Chỉ Quản lý chi nhánh được thêm gói
      - TC-F3.2.1-06a `PackageCreateIntegrationTest#onlyManagerCanCreate`
      - TC-F3.2.1-06b `PackageCreateIntegrationTest#missingTokenOrInactiveStaffCannotCreate`
- **UC-F3.3** Sửa thông tin gói tập
  - **US-F3.3.1** Sửa thông tin gói tập (#132) ✅ mọi AC có TC
    - AC-F3.3.1-01 Sửa giá; gói đã bán không đổi (BR-F3-06)
      - TC-F3.3.1-01a `PackageUpdateIntegrationTest#priceOnlyKeepsOmittedFieldsAndAudit`
      - TC-F3.3.1-01b `PackageUpdateIntegrationTest#historyIncludingPendingPurchaseRemainsIdentical`
    - AC-F3.3.1-02 Sửa gói chi nhánh khác hoặc gói toàn chuỗi bị từ chối 403 (BR-F3-07)
      - TC-F3.3.1-02a `PackageUpdateIntegrationTest#outsideBranchAndGlobalAre403WithoutMutation`
    - AC-F3.3.1-03 Xóa mô tả và gửi body rỗng
      - TC-F3.3.1-03a `PackageUpdateIntegrationTest#explicitNullClearsDescriptionAndEmptyBodyDoesNotChangeAudit`
    - AC-F3.3.1-04 Không đổi mã, loại và không xóa trường bắt buộc (BR-F3-05, BR-F3-02)
      - TC-F3.3.1-04a `PackageUpdateIntegrationTest#refusesImmutableAndUnknownFields`
      - TC-F3.3.1-04b `PackageUpdateIntegrationTest#sessionsMergeWithExistingTypeAndCannotBeCleared`
    - AC-F3.3.1-05 Dữ liệu sai thì không đổi gì
      - TC-F3.3.1-05a `PackageUpdateIntegrationTest#invalidUpdateIsAtomicAndReturnsFieldError`
      - TC-F3.3.1-05b `PackageUpdateIntegrationTest#invalidIdIsBadRequest`
      - TC-F3.3.1-05c `PackageUpdateIntegrationTest#malformedBodyIsRejected`
    - AC-F3.3.1-06 Sửa gói đã ngừng bán không làm gói bán lại
      - TC-F3.3.1-06a `PackageUpdateIntegrationTest#acceptsBoundariesAndCanEditInactiveWithoutReactivation`
    - AC-F3.3.1-07 Gói không tồn tại
      - TC-F3.3.1-07a `PackageUpdateIntegrationTest#missingPackageIs404WithoutMutation`
    - AC-F3.3.1-08 Chỉ Quản lý chi nhánh được sửa
      - TC-F3.3.1-08a `PackageUpdateIntegrationTest#deniedRolesCannotUpdate`
      - TC-F3.3.1-08b `PackageUpdateIntegrationTest#noTokenOrInactiveMissingStaffCannotUpdate`
- **UC-F3.4** Ngừng bán gói tập
  - **US-F3.4.1** Ngừng bán gói tập (#133) ✅ mọi AC có TC
    - AC-F3.4.1-01 Ngừng bán gói đang bán (BR-F3-08)
      - TC-F3.4.1-01a `PackageDeactivateIntegrationTest#deactivatesWithNoBodyOrEmptyJsonWithoutChangingOtherFields`
    - AC-F3.4.1-02 Gói đã bán vẫn dùng được (BR-F3-08)
      - TC-F3.4.1-02a `PackageDeactivateIntegrationTest#preservesAllHistoricalRows`
    - AC-F3.4.1-03 Gói ngừng bán không bán mới được nhưng vẫn tra cứu được (BR-F3-09)
      - TC-F3.4.1-03a `PackageCatalogIntegrationTest#inactiveFilterAndEmptyResults`
    - AC-F3.4.1-04 Ngừng bán lần nữa bị từ chối
      - TC-F3.4.1-04a `PackageDeactivateIntegrationTest#repeatIs422WithoutFurtherAuditChange`
      - TC-F3.4.1-04b `PackageDeactivateIntegrationTest#simultaneousDeactivationHasOneSuccessAndOne422`
    - AC-F3.4.1-05 Sửa cùng lúc không làm gói bán lại
      - TC-F3.4.1-05a `PackageDeactivateIntegrationTest#concurrentEditCannotReactivatePackage`
    - AC-F3.4.1-06 Từ chối gói ngoài chi nhánh, gói không tồn tại và vai trò khác (BR-F3-07)
      - TC-F3.4.1-06a `PackageDeactivateIntegrationTest#outsideBranchAndGlobalAre403EvenIfAlreadyInactive`
      - TC-F3.4.1-06b `PackageDeactivateIntegrationTest#deniedRolesCannotDeactivate`
      - TC-F3.4.1-06c `PackageDeactivateIntegrationTest#invalidIdIs400`
      - TC-F3.4.1-06d `PackageDeactivateIntegrationTest#missingAndBadTokenAre401`
      - TC-F3.4.1-06e `PackageDeactivateIntegrationTest#inactiveAndMissingStaffAre403`
      - TC-F3.4.1-06f `PackageDeactivateIntegrationTest#missingPackageIs404WithEmptyJsonBodyWithoutMutation`
- **UC-F3.5** Bán gói tập tại quầy (đăng ký mới / gia hạn)
  - **US-F3.5.1** Bán và gia hạn gói tại quầy (#134)
    - AC-F3.5.1-01 Bán gói tạo hóa đơn chờ và gói chờ, giá lấy ở server (BR-F3-04, BR-F3-13)
      - TC-F3.5.1-01a `InvoiceSaleIntegrationTest#sellCreatesPendingInvoiceAndMembershipWithServerPrice`
      - TC-F3.5.1-01b `InvoiceSaleIntegrationTest#sessionPackageCopiesSessionCount`
      - TC-F3.5.1-01c `InvoiceSaleIntegrationTest#membershipFailureRollsBackEntireSale`
    - AC-F3.5.1-02 Gói chỉ hiệu lực sau khi hóa đơn PAID; ngày hết hạn tính đúng (BR-F3-12)
      - TC-F3.5.1-02a `MembershipListIntegrationTest#pendingSaleIsNotUsableBeforePayment`
    - AC-F3.5.1-03 Gia hạn khi gói cũ còn hiệu lực (BR-F3-11)
      - TC-F3.5.1-03a `InvoiceSaleIntegrationTest#renewalStartsDayAfterOldTimePackage`
      - TC-F3.5.1-03b `MembershipServiceTest#newOrTimeRenewal`
    - AC-F3.5.1-04 Gia hạn khi gói SESSION cũ đã hết buổi (BR-F3-11)
      - TC-F3.5.1-04a `InvoiceSaleIntegrationTest#renewalAfterEmptySessionPackageStartsTomorrow`
      - TC-F3.5.1-04b `InvoiceSaleIntegrationTest#renewalAfterSessionPackageWithSessionsLeftStartsAfterEndDate`
      - TC-F3.5.1-04c `MembershipServiceTest#sessionRenewal`
    - AC-F3.5.1-05 Giữ đồng thời nhiều loại gói (BR-F3-10)
      - TC-F3.5.1-05a `InvoiceSaleIntegrationTest#differentPackageTypesKeepExistingMembershipIntact`
    - AC-F3.5.1-06 Chỉ một gói chờ cho mỗi loại (BR-F3-11)
      - TC-F3.5.1-06a `InvoiceSaleIntegrationTest#secondWaitingPackageOfSameTypeIsRejected`
      - TC-F3.5.1-06b `InvoiceSaleIntegrationTest#pendingWithoutActiveAlsoBlocks`
      - TC-F3.5.1-06c `InvoiceSaleIntegrationTest#waitingPackageOfOtherTypeDoesNotBlock`
    - AC-F3.5.1-07 Hai lễ tân bán cùng lúc cho một hội viên
      - TC-F3.5.1-07a `InvoiceSaleConcurrencyIntegrationTest#twoReceptionistsSellingAtOnceCreateExactlyOne`
    - AC-F3.5.1-08 Hội viên hoặc gói không hợp lệ (BR-F3-09)
      - TC-F3.5.1-08a `InvoiceSaleIntegrationTest#inactiveMemberIsRejected`
      - TC-F3.5.1-08b `InvoiceSaleIntegrationTest#inactivePackageIsRejected`
      - TC-F3.5.1-08c `InvoiceSaleIntegrationTest#otherBranchPackageIsRejected`
      - TC-F3.5.1-08d `InvoiceSaleIntegrationTest#unknownMemberOrPackageIsNotFound`
      - TC-F3.5.1-08e `InvoiceSaleIntegrationTest#memberOfOtherBranchIsHidden`
      - TC-F3.5.1-08f `InvoiceSaleConcurrencyIntegrationTest#saleWaitsForCatalogChangeAndReadsCommittedValues`
    - AC-F3.5.1-09 Hóa đơn tại quầy không tự hủy (BR-F3-14) ❌ chưa có TC
    - AC-F3.5.1-10 Tự kích hoạt gói nối tiếp đúng ngày (BR-F3-11)
      - TC-F3.5.1-10a `MembershipLifecycleIntegrationTest#expiresOldBeforeActivatingNext`
      - TC-F3.5.1-10b `MembershipLifecycleIntegrationTest#lastDeductionTodayRetainsAccess`
      - TC-F3.5.1-10c `MembershipLifecycleIntegrationTest#zeroSessionsFromYesterdayExpire`
      - TC-F3.5.1-10d `MembershipLifecycleIntegrationTest#repeatedRunDoesNotMutateAgain`
      - TC-F3.5.1-10e `MembershipLifecycleIntegrationTest#catchesUpAfterSeveralMissedDays`
      - TC-F3.5.1-10f `MembershipLifecycleIntegrationTest#endDateIsInclusive`
      - TC-F3.5.1-10g `MembershipLifecycleIntegrationTest#ignoresOtherStates`
      - TC-F3.5.1-10h `MembershipLifecycleIntegrationTest#frozenBlocksWithoutChangingWaitingPackage`
      - TC-F3.5.1-10i `MembershipLifecycleIntegrationTest#chainAndBranchAreIndependentGroups`
      - TC-F3.5.1-10j `MembershipLifecycleIntegrationTest#expiredPtKeepsRemainingSessions`
      - TC-F3.5.1-10k `MembershipLifecycleIntegrationTest#activationFailureRollsBackExpiration`
      - TC-F3.5.1-10l `MembershipLifecycleJobTest#annotations`
      - TC-F3.5.1-10m `MembershipLifecycleJobTest#passesVietnamDate`
      - TC-F3.5.1-10n `MembershipLifecycleJobTest#catchUpGoesThroughScheduler`
      - TC-F3.5.1-10o `MembershipLifecycleJobTest#retriesEveryFiveMinutesUntilSuccess`
      - TC-F3.5.1-10p `MembershipLifecycleJobTest#stopsRetryingOnNewDay`
    - AC-F3.5.1-11 Dữ liệu sai và vai trò khác bị từ chối
      - TC-F3.5.1-11a `InvoiceSaleIntegrationTest#invalidBodyIsBadRequest`
      - TC-F3.5.1-11b `InvoiceSaleIntegrationTest#otherRolesAreForbidden`
      - TC-F3.5.1-11c `InvoiceSaleIntegrationTest#missingTokenIsUnauthorized`
      - TC-F3.5.1-11d `InvoiceSaleIntegrationTest#missingFieldsAreNamed`
    - AC-F3.5.1-12 Xem gói đang giữ và ngày gia hạn dự kiến (BR-F3-11)
      - TC-F3.5.1-12a `MembershipListIntegrationTest#listsHeldMembershipsWithRenewalStartDate`
      - TC-F3.5.1-12b `MembershipListIntegrationTest#renewalStartDateMatchesSale`
      - TC-F3.5.1-12c `MembershipListIntegrationTest#scopeAndRoles`
      - TC-F3.5.1-12d `MembershipListIntegrationTest#invalidMemberIdIsRejected`
      - TC-F3.5.1-12e `MembershipServiceTest#neverBeforeToday`
    - AC-F3.5.1-13 Mã hóa đơn tự sinh và duy nhất
      - TC-F3.5.1-13a `InvoiceSaleIntegrationTest#invoiceCodesAreSequential`
      - TC-F3.5.1-13b `InvoiceCodeMigrationIntegrationTest#upgradesExistingDatabaseWithoutChangingInvoiceHistory`

## 5. Bảng ngược: TC → AC → US → UC → BR

Dùng khi một kiểm thử báo lỗi: tìm mã TC để biết nó vi phạm AC, US và quy tắc nghiệp vụ nào.

| TC | AC | US | UC | BR | Kiểm thử |
| --- | --- | --- | --- | --- | --- |
| TC-F3.1.1-01a | AC-F3.1.1-01 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `PackageCatalogIntegrationTest#scopesRowsAndCountAndIgnoresClientBranch` |
| TC-F3.1.1-02a | AC-F3.1.1-02 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `PackageCatalogIntegrationTest#defaultActiveAndOrderingMatchContract` |
| TC-F3.1.1-02b | AC-F3.1.1-02 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `PackageCatalogIntegrationTest#inactiveFilterAndEmptyResults` |
| TC-F3.1.1-02c | AC-F3.1.1-02 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `PackageCatalogIntegrationTest#maximumSizeAndPagePastEndAreValid` |
| TC-F3.1.1-02d | AC-F3.1.1-02 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `PackageControllerTest#defaultContract` |
| TC-F3.1.1-02e | AC-F3.1.1-02 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `features/packages/pages/PackagesPage.test.tsx › TC-F3.1.1-02e: mặc định lấy gói đang bán; chọn loại PT giữ n` |
| TC-F3.1.1-02f | AC-F3.1.1-02 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `features/packages/api/packages.test.ts › TC-F3.1.1-02f: gửi bộ lọc và phân trang đúng endpoint đã đặc` |
| TC-F3.1.1-04a | AC-F3.1.1-04 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `PackageCatalogIntegrationTest#ownerSeesOtherBranchesWithoutStaff` |
| TC-F3.1.1-04b | AC-F3.1.1-04 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `PackageCatalogServiceTest#ownerDoesNotRequireStaffAssignment` |
| TC-F3.1.1-05a | AC-F3.1.1-05 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `PackageCatalogIntegrationTest#inactiveOrMissingStaffIsForbidden` |
| TC-F3.1.1-05b | AC-F3.1.1-05 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `PackageControllerTest#rejectsDeniedRole` |
| TC-F3.1.1-05c | AC-F3.1.1-05 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `PackageControllerTest#missingAndInvalidBearerAreUnauthorized` |
| TC-F3.1.1-05d | AC-F3.1.1-05 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `PackageCatalogServiceTest#missingAssignmentNeverFallsBackToAllBranches` |
| TC-F3.1.1-06a | AC-F3.1.1-06 | US-F3.1.1 | UC-F3.1 | BR-F3-01 | `PackageControllerTest#rejectsInvalidQueryWithFieldErrors` |
| TC-F3.1.2-01a | AC-F3.1.2-01 | US-F3.1.2 | UC-F3.1 | BR-F3-01 | `PublicPackageIntegrationTest#guestSeesActiveBranchAndGlobalPackagesInDisplayOrder` |
| TC-F3.1.2-01b | AC-F3.1.2-01 | US-F3.1.2 | UC-F3.1 | BR-F3-01 | `features/packages/pages/PublicPricingPage.test.tsx › TC-F3.1.2-01b: hiện giá từ server, thời hạn hoặc số buổi, qu` |
| TC-F3.1.2-01c | AC-F3.1.2-01 | US-F3.1.2 | UC-F3.1 | BR-F3-01 | `features/packages/pages/PublicPricingPage.test.tsx › TC-F3.1.2-01c: gọi API công khai không kèm access token và k` |
| TC-F3.1.2-01d | AC-F3.1.2-01 | US-F3.1.2 | UC-F3.1 | BR-F3-01 | `PublicPackageIntegrationTest#staleOrForgedTokenIsIgnoredOnPublicRead` |
| TC-F3.1.2-02a | AC-F3.1.2-02 | US-F3.1.2 | UC-F3.1 | BR-F3-01 | `PublicPackageIntegrationTest#hidesInactiveAndOtherBranchPackages` |
| TC-F3.1.2-03a | AC-F3.1.2-03 | US-F3.1.2 | UC-F3.1 | BR-F3-01 | `PublicPackageIntegrationTest#exposesOnlyDisplayFields` |
| TC-F3.1.2-04a | AC-F3.1.2-04 | US-F3.1.2 | UC-F3.1 | BR-F3-01 | `PublicPackageIntegrationTest#emptyCatalogReturnsEmptyList` |
| TC-F3.1.2-04b | AC-F3.1.2-04 | US-F3.1.2 | UC-F3.1 | BR-F3-01 | `features/packages/pages/PublicPricingPage.test.tsx › TC-F3.1.2-04b: chưa có gói đang bán thì báo trống, không hiệ` |
| TC-F3.1.2-05a | AC-F3.1.2-05 | US-F3.1.2 | UC-F3.1 | BR-F3-01 | `features/packages/pages/PublicPricingPage.test.tsx › TC-F3.1.2-05a: lỗi tải thì báo lỗi, không hiện giá; bấm Thử ` |
| TC-F3.1.2-06a | AC-F3.1.2-06 | US-F3.1.2 | UC-F3.1 | BR-F3-01 | `PublicPackageIntegrationTest#inactiveDefaultBranchReturnsEmptyList` |
| TC-F3.2.1-01a | AC-F3.2.1-01 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `PackageCreateIntegrationTest#createsAllTypesWithServerBranchAuditAndReadVisibility` |
| TC-F3.2.1-02a | AC-F3.2.1-02 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `PackageCreateIntegrationTest#sessionsAreRequiredAndPositive` |
| TC-F3.2.1-03a | AC-F3.2.1-03 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `PackageCreateIntegrationTest#invalidInputsReturnFieldErrorsWithoutWrites` |
| TC-F3.2.1-03b | AC-F3.2.1-03 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `PackageCreateIntegrationTest#missingFieldsMalformedJsonAndLengthLimitsAreRejected` |
| TC-F3.2.1-03c | AC-F3.2.1-03 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `PackageCreateIntegrationTest#acceptsZeroPriceAndMaximumLengthsAndIntegers` |
| TC-F3.2.1-04a | AC-F3.2.1-04 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `PackageCreateIntegrationTest#rejectsServerManagedAndUnknownFields` |
| TC-F3.2.1-05a | AC-F3.2.1-05 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `PackageCreateIntegrationTest#duplicateInactiveGlobalCodeIsConflict` |
| TC-F3.2.1-05b | AC-F3.2.1-05 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `PackageCreateIntegrationTest#concurrentDuplicateCodeCreatesOneAndRollsBackOther` |
| TC-F3.2.1-06a | AC-F3.2.1-06 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `PackageCreateIntegrationTest#onlyManagerCanCreate` |
| TC-F3.2.1-06b | AC-F3.2.1-06 | US-F3.2.1 | UC-F3.2 | BR-F3-02, BR-F3-03, BR-F3-04 | `PackageCreateIntegrationTest#missingTokenOrInactiveStaffCannotCreate` |
| TC-F3.3.1-01a | AC-F3.3.1-01 | US-F3.3.1 | UC-F3.3 | BR-F3-06, BR-F3-05, BR-F3-07 | `PackageUpdateIntegrationTest#priceOnlyKeepsOmittedFieldsAndAudit` |
| TC-F3.3.1-01b | AC-F3.3.1-01 | US-F3.3.1 | UC-F3.3 | BR-F3-06, BR-F3-05, BR-F3-07 | `PackageUpdateIntegrationTest#historyIncludingPendingPurchaseRemainsIdentical` |
| TC-F3.3.1-02a | AC-F3.3.1-02 | US-F3.3.1 | UC-F3.3 | BR-F3-07, BR-F3-05, BR-F3-06 | `PackageUpdateIntegrationTest#outsideBranchAndGlobalAre403WithoutMutation` |
| TC-F3.3.1-03a | AC-F3.3.1-03 | US-F3.3.1 | UC-F3.3 | BR-F3-05, BR-F3-06, BR-F3-07 | `PackageUpdateIntegrationTest#explicitNullClearsDescriptionAndEmptyBodyDoesNotChangeAudit` |
| TC-F3.3.1-04a | AC-F3.3.1-04 | US-F3.3.1 | UC-F3.3 | BR-F3-05, BR-F3-02, BR-F3-06, BR-F3-07 | `PackageUpdateIntegrationTest#refusesImmutableAndUnknownFields` |
| TC-F3.3.1-04b | AC-F3.3.1-04 | US-F3.3.1 | UC-F3.3 | BR-F3-05, BR-F3-02, BR-F3-06, BR-F3-07 | `PackageUpdateIntegrationTest#sessionsMergeWithExistingTypeAndCannotBeCleared` |
| TC-F3.3.1-05a | AC-F3.3.1-05 | US-F3.3.1 | UC-F3.3 | BR-F3-05, BR-F3-06, BR-F3-07 | `PackageUpdateIntegrationTest#invalidUpdateIsAtomicAndReturnsFieldError` |
| TC-F3.3.1-05b | AC-F3.3.1-05 | US-F3.3.1 | UC-F3.3 | BR-F3-05, BR-F3-06, BR-F3-07 | `PackageUpdateIntegrationTest#invalidIdIsBadRequest` |
| TC-F3.3.1-05c | AC-F3.3.1-05 | US-F3.3.1 | UC-F3.3 | BR-F3-05, BR-F3-06, BR-F3-07 | `PackageUpdateIntegrationTest#malformedBodyIsRejected` |
| TC-F3.3.1-06a | AC-F3.3.1-06 | US-F3.3.1 | UC-F3.3 | BR-F3-05, BR-F3-06, BR-F3-07 | `PackageUpdateIntegrationTest#acceptsBoundariesAndCanEditInactiveWithoutReactivation` |
| TC-F3.3.1-07a | AC-F3.3.1-07 | US-F3.3.1 | UC-F3.3 | BR-F3-05, BR-F3-06, BR-F3-07 | `PackageUpdateIntegrationTest#missingPackageIs404WithoutMutation` |
| TC-F3.3.1-08a | AC-F3.3.1-08 | US-F3.3.1 | UC-F3.3 | BR-F3-05, BR-F3-06, BR-F3-07 | `PackageUpdateIntegrationTest#deniedRolesCannotUpdate` |
| TC-F3.3.1-08b | AC-F3.3.1-08 | US-F3.3.1 | UC-F3.3 | BR-F3-05, BR-F3-06, BR-F3-07 | `PackageUpdateIntegrationTest#noTokenOrInactiveMissingStaffCannotUpdate` |
| TC-F3.4.1-01a | AC-F3.4.1-01 | US-F3.4.1 | UC-F3.4 | BR-F3-08, BR-F3-07, BR-F3-09 | `PackageDeactivateIntegrationTest#deactivatesWithNoBodyOrEmptyJsonWithoutChangingOtherFields` |
| TC-F3.4.1-02a | AC-F3.4.1-02 | US-F3.4.1 | UC-F3.4 | BR-F3-08, BR-F3-07, BR-F3-09 | `PackageDeactivateIntegrationTest#preservesAllHistoricalRows` |
| TC-F3.4.1-03a | AC-F3.4.1-03 | US-F3.4.1 | UC-F3.4 | BR-F3-09, BR-F3-07, BR-F3-08 | `PackageCatalogIntegrationTest#inactiveFilterAndEmptyResults` |
| TC-F3.4.1-04a | AC-F3.4.1-04 | US-F3.4.1 | UC-F3.4 | BR-F3-07, BR-F3-08, BR-F3-09 | `PackageDeactivateIntegrationTest#repeatIs422WithoutFurtherAuditChange` |
| TC-F3.4.1-04b | AC-F3.4.1-04 | US-F3.4.1 | UC-F3.4 | BR-F3-07, BR-F3-08, BR-F3-09 | `PackageDeactivateIntegrationTest#simultaneousDeactivationHasOneSuccessAndOne422` |
| TC-F3.4.1-05a | AC-F3.4.1-05 | US-F3.4.1 | UC-F3.4 | BR-F3-07, BR-F3-08, BR-F3-09 | `PackageDeactivateIntegrationTest#concurrentEditCannotReactivatePackage` |
| TC-F3.4.1-06a | AC-F3.4.1-06 | US-F3.4.1 | UC-F3.4 | BR-F3-07, BR-F3-08, BR-F3-09 | `PackageDeactivateIntegrationTest#outsideBranchAndGlobalAre403EvenIfAlreadyInactive` |
| TC-F3.4.1-06b | AC-F3.4.1-06 | US-F3.4.1 | UC-F3.4 | BR-F3-07, BR-F3-08, BR-F3-09 | `PackageDeactivateIntegrationTest#deniedRolesCannotDeactivate` |
| TC-F3.4.1-06c | AC-F3.4.1-06 | US-F3.4.1 | UC-F3.4 | BR-F3-07, BR-F3-08, BR-F3-09 | `PackageDeactivateIntegrationTest#invalidIdIs400` |
| TC-F3.4.1-06d | AC-F3.4.1-06 | US-F3.4.1 | UC-F3.4 | BR-F3-07, BR-F3-08, BR-F3-09 | `PackageDeactivateIntegrationTest#missingAndBadTokenAre401` |
| TC-F3.4.1-06e | AC-F3.4.1-06 | US-F3.4.1 | UC-F3.4 | BR-F3-07, BR-F3-08, BR-F3-09 | `PackageDeactivateIntegrationTest#inactiveAndMissingStaffAre403` |
| TC-F3.4.1-06f | AC-F3.4.1-06 | US-F3.4.1 | UC-F3.4 | BR-F3-07, BR-F3-08, BR-F3-09 | `PackageDeactivateIntegrationTest#missingPackageIs404WithEmptyJsonBodyWithoutMutation` |
| TC-F3.5.1-01a | AC-F3.5.1-01 | US-F3.5.1 | UC-F3.5 | BR-F3-04, BR-F3-13, BR-F3-09, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-14 | `InvoiceSaleIntegrationTest#sellCreatesPendingInvoiceAndMembershipWithServerPrice` |
| TC-F3.5.1-01b | AC-F3.5.1-01 | US-F3.5.1 | UC-F3.5 | BR-F3-04, BR-F3-13, BR-F3-09, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-14 | `InvoiceSaleIntegrationTest#sessionPackageCopiesSessionCount` |
| TC-F3.5.1-01c | AC-F3.5.1-01 | US-F3.5.1 | UC-F3.5 | BR-F3-04, BR-F3-13, BR-F3-09, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-14 | `InvoiceSaleIntegrationTest#membershipFailureRollsBackEntireSale` |
| TC-F3.5.1-02a | AC-F3.5.1-02 | US-F3.5.1 | UC-F3.5 | BR-F3-12, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-11, BR-F3-13, BR-F3-14 | `MembershipListIntegrationTest#pendingSaleIsNotUsableBeforePayment` |
| TC-F3.5.1-03a | AC-F3.5.1-03 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#renewalStartsDayAfterOldTimePackage` |
| TC-F3.5.1-03b | AC-F3.5.1-03 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipServiceTest#newOrTimeRenewal` |
| TC-F3.5.1-04a | AC-F3.5.1-04 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#renewalAfterEmptySessionPackageStartsTomorrow` |
| TC-F3.5.1-04b | AC-F3.5.1-04 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#renewalAfterSessionPackageWithSessionsLeftStartsAfterEndDate` |
| TC-F3.5.1-04c | AC-F3.5.1-04 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipServiceTest#sessionRenewal` |
| TC-F3.5.1-05a | AC-F3.5.1-05 | US-F3.5.1 | UC-F3.5 | BR-F3-10, BR-F3-04, BR-F3-09, BR-F3-11, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#differentPackageTypesKeepExistingMembershipIntact` |
| TC-F3.5.1-06a | AC-F3.5.1-06 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#secondWaitingPackageOfSameTypeIsRejected` |
| TC-F3.5.1-06b | AC-F3.5.1-06 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#pendingWithoutActiveAlsoBlocks` |
| TC-F3.5.1-06c | AC-F3.5.1-06 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#waitingPackageOfOtherTypeDoesNotBlock` |
| TC-F3.5.1-07a | AC-F3.5.1-07 | US-F3.5.1 | UC-F3.5 | BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleConcurrencyIntegrationTest#twoReceptionistsSellingAtOnceCreateExactlyOne` |
| TC-F3.5.1-08a | AC-F3.5.1-08 | US-F3.5.1 | UC-F3.5 | BR-F3-09, BR-F3-04, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#inactiveMemberIsRejected` |
| TC-F3.5.1-08b | AC-F3.5.1-08 | US-F3.5.1 | UC-F3.5 | BR-F3-09, BR-F3-04, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#inactivePackageIsRejected` |
| TC-F3.5.1-08c | AC-F3.5.1-08 | US-F3.5.1 | UC-F3.5 | BR-F3-09, BR-F3-04, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#otherBranchPackageIsRejected` |
| TC-F3.5.1-08d | AC-F3.5.1-08 | US-F3.5.1 | UC-F3.5 | BR-F3-09, BR-F3-04, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#unknownMemberOrPackageIsNotFound` |
| TC-F3.5.1-08e | AC-F3.5.1-08 | US-F3.5.1 | UC-F3.5 | BR-F3-09, BR-F3-04, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#memberOfOtherBranchIsHidden` |
| TC-F3.5.1-08f | AC-F3.5.1-08 | US-F3.5.1 | UC-F3.5 | BR-F3-09, BR-F3-04, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleConcurrencyIntegrationTest#saleWaitsForCatalogChangeAndReadsCommittedValues` |
| TC-F3.5.1-10a | AC-F3.5.1-10 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipLifecycleIntegrationTest#expiresOldBeforeActivatingNext` |
| TC-F3.5.1-10b | AC-F3.5.1-10 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipLifecycleIntegrationTest#lastDeductionTodayRetainsAccess` |
| TC-F3.5.1-10c | AC-F3.5.1-10 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipLifecycleIntegrationTest#zeroSessionsFromYesterdayExpire` |
| TC-F3.5.1-10d | AC-F3.5.1-10 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipLifecycleIntegrationTest#repeatedRunDoesNotMutateAgain` |
| TC-F3.5.1-10e | AC-F3.5.1-10 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipLifecycleIntegrationTest#catchesUpAfterSeveralMissedDays` |
| TC-F3.5.1-10f | AC-F3.5.1-10 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipLifecycleIntegrationTest#endDateIsInclusive` |
| TC-F3.5.1-10g | AC-F3.5.1-10 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipLifecycleIntegrationTest#ignoresOtherStates` |
| TC-F3.5.1-10h | AC-F3.5.1-10 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipLifecycleIntegrationTest#frozenBlocksWithoutChangingWaitingPackage` |
| TC-F3.5.1-10i | AC-F3.5.1-10 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipLifecycleIntegrationTest#chainAndBranchAreIndependentGroups` |
| TC-F3.5.1-10j | AC-F3.5.1-10 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipLifecycleIntegrationTest#expiredPtKeepsRemainingSessions` |
| TC-F3.5.1-10k | AC-F3.5.1-10 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipLifecycleIntegrationTest#activationFailureRollsBackExpiration` |
| TC-F3.5.1-10l | AC-F3.5.1-10 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipLifecycleJobTest#annotations` |
| TC-F3.5.1-10m | AC-F3.5.1-10 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipLifecycleJobTest#passesVietnamDate` |
| TC-F3.5.1-10n | AC-F3.5.1-10 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipLifecycleJobTest#catchUpGoesThroughScheduler` |
| TC-F3.5.1-10o | AC-F3.5.1-10 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipLifecycleJobTest#retriesEveryFiveMinutesUntilSuccess` |
| TC-F3.5.1-10p | AC-F3.5.1-10 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipLifecycleJobTest#stopsRetryingOnNewDay` |
| TC-F3.5.1-11a | AC-F3.5.1-11 | US-F3.5.1 | UC-F3.5 | BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#invalidBodyIsBadRequest` |
| TC-F3.5.1-11b | AC-F3.5.1-11 | US-F3.5.1 | UC-F3.5 | BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#otherRolesAreForbidden` |
| TC-F3.5.1-11c | AC-F3.5.1-11 | US-F3.5.1 | UC-F3.5 | BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#missingTokenIsUnauthorized` |
| TC-F3.5.1-11d | AC-F3.5.1-11 | US-F3.5.1 | UC-F3.5 | BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#missingFieldsAreNamed` |
| TC-F3.5.1-12a | AC-F3.5.1-12 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipListIntegrationTest#listsHeldMembershipsWithRenewalStartDate` |
| TC-F3.5.1-12b | AC-F3.5.1-12 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipListIntegrationTest#renewalStartDateMatchesSale` |
| TC-F3.5.1-12c | AC-F3.5.1-12 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipListIntegrationTest#scopeAndRoles` |
| TC-F3.5.1-12d | AC-F3.5.1-12 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipListIntegrationTest#invalidMemberIdIsRejected` |
| TC-F3.5.1-12e | AC-F3.5.1-12 | US-F3.5.1 | UC-F3.5 | BR-F3-11, BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-12, BR-F3-13, BR-F3-14 | `MembershipServiceTest#neverBeforeToday` |
| TC-F3.5.1-13a | AC-F3.5.1-13 | US-F3.5.1 | UC-F3.5 | BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceSaleIntegrationTest#invoiceCodesAreSequential` |
| TC-F3.5.1-13b | AC-F3.5.1-13 | US-F3.5.1 | UC-F3.5 | BR-F3-04, BR-F3-09, BR-F3-10, BR-F3-11, BR-F3-12, BR-F3-13, BR-F3-14 | `InvoiceCodeMigrationIntegrationTest#upgradesExistingDatabaseWithoutChangingInvoiceHistory` |

## 6. Kiểm thử chưa mang mã TC

Có 267 kiểm thử chưa mang mã TC. Gồm kiểm thử thuần kỹ thuật (JWT, cấu hình, ràng buộc database; được phép ghi mã `S…` hay `BR-…` thay mã TC) và kiểm thử của chức năng chưa gắn mã.

| File | Số kiểm thử |
| --- | --- |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/common/exception/GlobalExceptionHandlerTest.java` | 9 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/common/mail/EmailSenderTest.java` | 4 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/common/scheduling/SchedulingConfigTest.java` | 4 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/common/security/CurrentUserTest.java` | 6 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/common/time/VnTimeTest.java` | 5 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/common/util/PhoneUtilsTest.java` | 2 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/DatabaseCleanerTest.java` | 3 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/DatabaseConstraintsTest.java` | 21 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/auth/AccountServiceTest.java` | 2 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/auth/AuthControllerTest.java` | 18 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/auth/AuthServiceTest.java` | 17 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/auth/AuthSessionConcurrencyIntegrationTest.java` | 3 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/auth/JwtSecurityIntegrationTest.java` | 9 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/auth/JwtTokenServiceTest.java` | 5 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/invoice/InvoiceSaleIntegrationTest.java` | 2 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/member/F1RegistrationIntegrationTest.java` | 13 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/member/MemberApiIntegrationTest.java` | 25 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/member/MemberPhoneSupportTest.java` | 6 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/member/MemberServiceTest.java` | 11 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/member/StaffBranchIntegrationTest.java` | 8 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/membership/MembershipListIntegrationTest.java` | 1 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/packagecatalog/PackageCatalogServiceTest.java` | 1 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/packagecatalog/PackageMapperTest.java` | 1 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/packagecatalog/PublicPackageIntegrationTest.java` | 1 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/staff/StaffServiceTest.java` | 4 |
| `backend/src/test/java/vn/edu/hust/nhom6/gym/feature/systemconfig/SystemConfigServiceTest.java` | 4 |
| `frontend/src/app/router.test.ts` | 3 |
| `frontend/src/auth/account-change.test.tsx` | 1 |
| `frontend/src/auth/AuthProvider.test.tsx` | 9 |
| `frontend/src/auth/cookie-operation-queue.test.ts` | 1 |
| `frontend/src/auth/redirect.test.ts` | 7 |
| `frontend/src/auth/session-store.test.ts` | 3 |
| `frontend/src/features/auth/api/auth-api.test.ts` | 4 |
| `frontend/src/features/auth/api/validation.test.ts` | 3 |
| `frontend/src/features/auth/pages/LoginPage.test.tsx` | 6 |
| `frontend/src/features/auth/pages/RegisterPage.test.tsx` | 5 |
| `frontend/src/features/members/api/members.test.ts` | 4 |
| `frontend/src/features/members/components/MemberFormModal.test.tsx` | 3 |
| `frontend/src/features/members/pages/MembersPage.test.tsx` | 6 |
| `frontend/src/features/packages/api/packages.test.ts` | 1 |
| `frontend/src/features/shared/use-remote-query.test.ts` | 2 |
| `frontend/src/layouts/member-menu.test.ts` | 2 |
| `frontend/src/layouts/staff-menu.test.ts` | 2 |
| `frontend/src/layouts/UserMenu.test.tsx` | 1 |
| `frontend/src/routes/ProtectedRoute.test.tsx` | 5 |
| `frontend/src/routes/route-access.test.ts` | 5 |
| `frontend/src/services/http.test.ts` | 9 |
