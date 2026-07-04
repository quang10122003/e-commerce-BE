package shop.shop.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import shop.shop.Role.service.RoleService;
import shop.shop.admin.dto.request.AdminCreateCategoriRequest;
import shop.shop.admin.dto.request.AdminCreateProductRequest;
import shop.shop.admin.dto.request.AdminUpdateCategoriRequest;
import shop.shop.admin.dto.request.AdminUpdateProductRequest;
import shop.shop.admin.dto.request.AdminUserUpdateRequest;
import shop.shop.admin.dto.response.AdminCatagoryOverviewRepone;
import shop.shop.admin.dto.response.AdminNewOrderOverview;
import shop.shop.admin.dto.response.AdminOrderItemRepone;
import shop.shop.admin.dto.response.AdminOrderOverview;
import shop.shop.admin.dto.response.AdminOrdersRepone;
import shop.shop.admin.dto.response.AdminOverviewRepone;
import shop.shop.admin.dto.response.AdminPaymentsRepone;
import shop.shop.admin.dto.response.AdminProductListResponse;
import shop.shop.admin.dto.response.AdminProductOverview;
import shop.shop.admin.dto.response.AdminProductStatusResponse;
import shop.shop.admin.dto.response.AdminProductSummaryResponse;
import shop.shop.admin.dto.response.AdminRevenueIn7day;
import shop.shop.admin.dto.response.AdminRevenueOverview;
import shop.shop.admin.dto.response.AdminRevenueRepone;
import shop.shop.admin.dto.response.AdminUserDetailResponse;
import shop.shop.admin.dto.response.AdminUserListResponse;
import shop.shop.admin.dto.response.AdminUserLockResponse;
import shop.shop.admin.dto.response.AdminUserOverview;
import shop.shop.admin.dto.response.RoleRepone;
import shop.shop.category.dto.response.CategorySummaryResponse;
import shop.shop.category.service.CategoryService;
import shop.shop.common.OrderStatus;
import shop.shop.common.PaymentMethod;
import shop.shop.common.PeriodType;
import shop.shop.common.ProductStatus;
import shop.shop.common.dto.response.ApiResponse;
import shop.shop.order.repo.OrderRepository;
import shop.shop.order.service.OrderService;
import shop.shop.payment.service.PaymentService;
import shop.shop.product.repository.ProductRepository;
import shop.shop.product.service.ProductService;
import shop.shop.user.repos.UserRepo;
import shop.shop.user.service.UserService;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminService {
        RoleService roleService;
        UserService userService;
        UserRepo userRepo;
        ProductRepository productRepository;
        ProductService productService;
        OrderRepository orderRepository;
        OrderService orderService;
        CategoryService categoryService;
        PaymentService paymentService;

        public ApiResponse<List<RoleRepone>> getRole() {
                return roleService.getRole();
        }

        public ApiResponse<AdminUserDetailResponse> getAdminUserById(Long userId) {
                return userService.getAdminUserById(userId);
        }

        public ApiResponse<AdminUserListResponse> getAdminUsers(String search, String role, String status,
                        Pageable pageable) {
                return userService.getAdminUsers(search, role, status, pageable);
        }

        public ApiResponse<AdminProductListResponse> getAdminProducts(Long catagoryId, String search, String status,
                        Pageable pageable) {
                return productService.getAdminProducts(catagoryId, search, status, pageable);
        }

        public ApiResponse<AdminUserDetailResponse> updateAdminUser(Long userId, AdminUserUpdateRequest request) {
                return userService.updateAdminUser(userId, request);
        }

        public ApiResponse<AdminUserLockResponse> updateUserLockStatus(Long userId, boolean locked) {
                return userService.updateUserLockStatus(userId, locked);
        }

        public ApiResponse<AdminProductStatusResponse> updateProductStatus(Long productId, ProductStatus status) {
                return productService.updateProductStatus(productId, status);
        }

        public ApiResponse<Void> deleteUser(Long userId) {
                return userService.deleteUser(userId);
        }

        public ApiResponse<Void> deleteProduct(Long productId) {
                return productService.deleteProduct(productId);
        }

        public ApiResponse<AdminProductSummaryResponse> createProduct(AdminCreateProductRequest data,
                        MultipartFile thumbnail, List<MultipartFile> images) {
                return productService.createProduct(data, thumbnail, images);
        }

        public ApiResponse<AdminProductSummaryResponse> updateProduct(Long productId,
                        AdminUpdateProductRequest request) {
                return productService.updateProduct(productId, request);
        }

        public ApiResponse<AdminOverviewRepone> getOverview() {

                AdminUserOverview adminUserOverview = new AdminUserOverview(userRepo.countAllUsersForAdmin(),
                                userRepo.countNewUsersInLast7Days());

                AdminProductOverview adminProductOverview = new AdminProductOverview(
                                productRepository.countTotalProducts(), productRepository.countProductsActive());

                // Map interface projection về DTO.
                List<AdminNewOrderOverview> newOrders = orderRepository.findTop5NewOrderOverview()
                                .stream()
                                .map(item -> new AdminNewOrderOverview(
                                                item.getId(),
                                                item.getCreatedAt(),
                                                item.getShippingName(),
                                                item.getTotalAmount(),
                                                item.getMethodPayment() == null ? null
                                                                : PaymentMethod.valueOf(item.getMethodPayment()),
                                                OrderStatus.valueOf(item.getStatusOrder())))
                                .toList();

                AdminOrderOverview adminOrderOverview = new AdminOrderOverview(orderRepository.countTodayOrderCount(),
                                orderRepository.countByStatus(OrderStatus.PENDING), newOrders);

                // Doanh thu tuần trước.
                BigDecimal LastWeekRevenue = orderService.getLastWeekRevenue();

                // Doanh thu tuần hiện tại.
                BigDecimal weekRevenue = orderService.getWeekRevenue();

                // Mức tăng trưởng doanh thu của tuần hiện tại so với tuần vừa qua.
                BigDecimal growth = orderService.calculateGrowth(weekRevenue, LastWeekRevenue);

                // Doanh thu trong 7 ngày.
                List<AdminRevenueIn7day> listAdminRevenueIn7day = orderService.getRevenueIn7Days();

                AdminRevenueOverview adminRevenueOverview = new AdminRevenueOverview(weekRevenue, growth,
                                listAdminRevenueIn7day);

                AdminOverviewRepone adminOverviewRepone = new AdminOverviewRepone(adminUserOverview,
                                adminProductOverview, adminOrderOverview, adminRevenueOverview);

                return ApiResponse.success("lấy doanh thu trong tuần thành công", adminOverviewRepone);
        }

        public ApiResponse<List<CategorySummaryResponse>> getAllCategories() {
                return categoryService.getAllCategories();
        }

        public ApiResponse<CategorySummaryResponse> createCategory(AdminCreateCategoriRequest data,
                        MultipartFile file) {
                return categoryService.createCategori(data, file);
        }

        public ApiResponse<CategorySummaryResponse> updateCategory(Long id, AdminUpdateCategoriRequest data,
                        MultipartFile file) {
                return categoryService.updateCategori(id, data, file);
        }

        public ApiResponse<Void> deleteCategory(Long id) {
                return categoryService.deleteCategori(id);
        }

        public ApiResponse<AdminCatagoryOverviewRepone> getOverviewCategory() {
                return categoryService.getOverviewCategory();
        }

        public ApiResponse<AdminPaymentsRepone> getPayments(String search, String status, LocalDate from,
                        LocalDate to) {
                return paymentService.getPayment(search, status, from, to);
        }

        public ApiResponse<AdminOrdersRepone> getOrders(String search, String status, LocalDate from,
                        LocalDate to) {
                return orderService.getAdminOrders(search, status, from, to);
        }

        public ApiResponse<AdminOrderItemRepone> updateOrderStatus(Long orderId, String status) {
                return orderService.updateAdminOrderStatus(orderId, status);
        }
        public AdminRevenueRepone getRevenueData(PeriodType type, Integer year, Integer week, Integer month){
                return orderService.getRevenueData( type,  year,  week,  month);
        }
}
