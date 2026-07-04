package shop.shop.admin.controller;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import shop.shop.admin.dto.request.AdminCreateCategoriRequest;
import shop.shop.admin.dto.request.AdminCreateProductRequest;
import shop.shop.admin.dto.request.AdminProductStatusRequest;
import shop.shop.admin.dto.request.AdminUpdateProductRequest;
import shop.shop.admin.dto.response.AdminCatagoryOverviewRepone;
import shop.shop.admin.dto.response.AdminOrderItemRepone;
import shop.shop.admin.dto.response.AdminOverviewRepone;
import shop.shop.admin.dto.response.AdminOrdersRepone;
import shop.shop.admin.dto.response.AdminPaymentsRepone;
import shop.shop.admin.dto.response.AdminProductListResponse;
import shop.shop.admin.dto.response.AdminProductStatusResponse;
import shop.shop.admin.dto.response.AdminProductSummaryResponse;
import shop.shop.admin.dto.response.AdminRevenueRepone;
import shop.shop.admin.dto.response.AdminUserDetailResponse;
import shop.shop.admin.dto.response.AdminUserListResponse;
import shop.shop.admin.dto.request.AdminUpdateCategoriRequest;
import shop.shop.admin.dto.request.AdminUserLockRequest;
import shop.shop.admin.dto.response.AdminUserLockResponse;
import shop.shop.admin.dto.response.RoleRepone;
import shop.shop.admin.service.AdminService;
import shop.shop.category.dto.response.CategorySummaryResponse;
import shop.shop.admin.dto.request.AdminUserUpdateRequest;
import shop.shop.common.PeriodType;
import shop.shop.common.dto.response.ApiResponse;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminController {
    AdminService adminService;

    // Lay danh sach user.
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminUserListResponse>> getAdminUsers(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "status", required = false) String status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(adminService.getAdminUsers(search, role, status, pageable));
    }

    @GetMapping("/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminProductListResponse>> getAdminProducts(
            @RequestParam(name = "catagoryId", required = false) Long catagoryId,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) String status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(adminService.getAdminProducts(catagoryId, search, status, pageable));
    }

    @PostMapping(value = "/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminProductSummaryResponse>> createProduct(
            @RequestPart(name = "thumbnail") MultipartFile thumbnail,
            @RequestPart(name = "images", required = false) List<MultipartFile> images,
            @Valid @RequestPart("data") AdminCreateProductRequest data) {
        return ResponseEntity.status(201).body(adminService.createProduct(data, thumbnail, images));
    }

    @PutMapping(value = "/products/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminProductSummaryResponse>> updateProduct(
            @PathVariable(name = "productId") Long productId,
            @Valid @ModelAttribute AdminUpdateProductRequest request) {
        return ResponseEntity.ok(adminService.updateProduct(productId, request));
    }

    @DeleteMapping("/products/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable(name = "productId") Long productId) {
        return ResponseEntity.ok(adminService.deleteProduct(productId));
    }

    @PatchMapping("/products/{productId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminProductStatusResponse>> updateProductStatus(
            @PathVariable(name = "productId") Long productId,
            @Valid @RequestBody AdminProductStatusRequest request) {
        return ResponseEntity.ok(adminService.updateProductStatus(productId, request.getStatus()));
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> getAdminUserById(
            @PathVariable(name = "userId") Long userId) {
        return ResponseEntity.ok(adminService.getAdminUserById(userId));
    }

    @PatchMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> updateAdminUser(
            @PathVariable(name = "userId") Long userId,
            @Valid @RequestBody AdminUserUpdateRequest request) {
        return ResponseEntity.ok(adminService.updateAdminUser(userId, request));
    }

    @PatchMapping("/users/{userId}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminUserLockResponse>> updateUserLockStatus(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserLockRequest request) {
        return ResponseEntity.ok(adminService.updateUserLockStatus(userId, request.isLocked()));
    }

    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable(name = "userId") Long id) {
        return ResponseEntity.ok(
                adminService.deleteUser(id));
    }

    @GetMapping("overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminOverviewRepone>> overview() {
        return ResponseEntity.status(200).body(
                adminService.getOverview());
    }

    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<RoleRepone>>> getRole() {
        return ResponseEntity.status(200).body(
                adminService.getRole());
    }

    @GetMapping("/categories/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminCatagoryOverviewRepone>> getOverviewCategory() {
        return ResponseEntity.status(200).body(adminService.getOverviewCategory());
    }

    @GetMapping("/categorie")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<CategorySummaryResponse>>> getAllCategories() {
        return ResponseEntity.status(200).body(adminService.getAllCategories());
    }

    @PostMapping(value = "/categori", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategorySummaryResponse>> createCategori(
            @RequestPart(name = "file") MultipartFile file,
            @Valid @RequestPart("data") AdminCreateCategoriRequest data) {
        return ResponseEntity.status(201).body(adminService.createCategory(data, file));
    }

    @PatchMapping(value = "/categori/{categoriId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategorySummaryResponse>> updateCategori(@PathVariable("categoriId") Long id,
            @RequestPart(required = false, name = "file") MultipartFile file,
            @Valid @RequestPart("data") AdminUpdateCategoriRequest data) {
        return ResponseEntity.status(200).body(adminService.updateCategory(id, data, file));
    }

    @DeleteMapping("/categori/{categoriId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCategori(@PathVariable("categoriId") Long id) {
        return ResponseEntity.ok(adminService.deleteCategory(id));
    }

    @GetMapping("/payments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminPaymentsRepone>> getPayments(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(adminService.getPayments(search, status, from, to));
    }

    @GetMapping("/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminOrdersRepone>> getOrders(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(adminService.getOrders(search, status, from, to));
    }

    // Cập nhật trạng thái đơn hàng từ màn hình quản trị.
    @PostMapping("/orders/{orderId}/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminOrderItemRepone>> updateOrderStatus(
            @PathVariable(name = "orderId") Long orderId,
            @PathVariable(name = "status") String status) {
        return ResponseEntity.ok(adminService.updateOrderStatus(orderId, status));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<AdminRevenueRepone>> getRevenue(@RequestParam("type") PeriodType type,
            @RequestParam("year") Integer year,
            @RequestParam(value = "week", required = false) Integer week,
            @RequestParam(value = "month", required = false) Integer month) {
        return ResponseEntity.status(200).body(ApiResponse.success("lấy data biểu đồ thành công",
                adminService.getRevenueData(type, year, week, month)));
    }
}
