// chỉ lo tổng hợp dữ liệu cho dashboard tổng quan admin
package shop.shop.admin.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import shop.shop.admin.dto.response.AdminNewOrderOverview;
import shop.shop.admin.dto.response.AdminOrderOverview;
import shop.shop.admin.dto.response.AdminOverviewRepone;
import shop.shop.admin.dto.response.AdminProductOverview;
import shop.shop.admin.dto.response.AdminRevenueIn7day;
import shop.shop.admin.dto.response.AdminRevenueOverview;
import shop.shop.admin.dto.response.AdminUserOverview;
import shop.shop.common.OrderStatus;
import shop.shop.common.PaymentMethod;
import shop.shop.common.dto.response.ApiResponse;
import shop.shop.order.repo.OrderRepository;
import shop.shop.order.service.RevenueReportQueryService;
import shop.shop.product.repository.ProductRepository;
import shop.shop.user.repos.UserRepo;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminOverviewQueryService {
    UserRepo userRepo;
    ProductRepository productRepository;
    OrderRepository orderRepository;
    RevenueReportQueryService revenueReportQueryService;

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
        BigDecimal lastWeekRevenue = revenueReportQueryService.getLastWeekRevenue();

        // Doanh thu tuần hiện tại.
        BigDecimal weekRevenue = revenueReportQueryService.getWeekRevenue();

        // Mức tăng trưởng doanh thu của tuần hiện tại so với tuần vừa qua.
        BigDecimal growth = revenueReportQueryService.calculateGrowth(weekRevenue, lastWeekRevenue);

        // Doanh thu trong 7 ngày.
        List<AdminRevenueIn7day> listAdminRevenueIn7day = revenueReportQueryService.getRevenueIn7Days();

        AdminRevenueOverview adminRevenueOverview = new AdminRevenueOverview(weekRevenue, growth,
                listAdminRevenueIn7day);

        AdminOverviewRepone adminOverviewRepone = new AdminOverviewRepone(adminUserOverview,
                adminProductOverview, adminOrderOverview, adminRevenueOverview);

        return ApiResponse.success("lấy doanh thu trong tuần thành công", adminOverviewRepone);
    }
}