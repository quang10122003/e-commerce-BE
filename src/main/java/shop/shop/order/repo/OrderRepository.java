package shop.shop.order.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import shop.shop.admin.Projection.DailyRevenueProjection;
import shop.shop.admin.dto.response.AdminNewOrderOverviewProjection;
import shop.shop.common.OrderStatus;
import shop.shop.order.entity.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

        boolean existsByOrderCode(String orderCode);

        Optional<Order> findByOrderCode(String orderCode);

        @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.user.id = :userId ORDER BY o.createdAt DESC")
        List<Order> findAllByUserIdWithItems(@Param("userId") Long userId);

        // Lấy đơn hàng trong ngày hôm nay.
        @Query("SELECT COUNT(o) FROM Order o WHERE FUNCTION('DATE', o.createdAt) = CURRENT_DATE")
        long countTodayOrderCount();

        // Lấy danh sách đơn hàng cho trang admin theo bộ lọc.
        @Query("""
                        SELECT DISTINCT o
                        FROM Order o
                        LEFT JOIN FETCH o.user u
                        LEFT JOIN FETCH o.items i
                        WHERE (:status IS NULL OR o.status = :status)
                        AND (:fromDt IS NULL OR o.createdAt >= :fromDt)
                        AND (:toDt IS NULL OR o.createdAt <= :toDt)
                        AND (
                            :search IS NULL
                            OR LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :search, '%'))
                            OR LOWER(o.shippingPhone) LIKE LOWER(CONCAT('%', :search, '%'))
                            OR LOWER(o.shippingName) LIKE LOWER(CONCAT('%', :search, '%'))
                        )
                        ORDER BY o.createdAt DESC
                        """)
        List<Order> findAdminOrders(
                        @Param("search") String search,
                        @Param("status") OrderStatus status,
                        @Param("fromDt") LocalDateTime fromDt,
                        @Param("toDt") LocalDateTime toDt);

        // Đếm số đơn theo trạng thái để hiển thị thống kê admin.
        long countByStatus(OrderStatus status);

        // Đếm tổng đơn trong 7 ngày gần nhất theo trạng thái.
        long countByStatusAndCreatedAtGreaterThanEqual(OrderStatus status, LocalDateTime fromDt);

        // Lấy một số thông tin của 5 order mới nhất kèm MethodPayment mới nhất của
        // payment thuộc order đó.
        @Query(value = """
                        SELECT
                            o.id AS id,
                            o.created_at AS createdAt,
                            o.shipping_name AS shippingName,
                            o.total_amount AS totalAmount,
                            p.method AS methodPayment,
                            o.status AS statusOrder
                        FROM orders o
                        LEFT JOIN payments p
                            ON p.order_id = o.id
                            AND p.id = (
                                SELECT MAX(p2.id)
                                FROM payments p2
                                WHERE p2.order_id = o.id
                            )
                        ORDER BY o.created_at DESC
                        LIMIT 5
                        """, nativeQuery = true)
        List<AdminNewOrderOverviewProjection> findTop5NewOrderOverview();

        // Lấy doanh thu từng ngày.
        @Query("select COALESCE(SUM(o.totalAmount),0) from Order o where o.status =  shop.shop.common.OrderStatus.COMPLETED and o.createdAt >= :startday and o.createdAt < :endday")
        BigDecimal getRevenueByDay(@Param(value = "startday") LocalDateTime startday,
                        @Param(value = "endday") LocalDateTime endday);

        // Tổng doanh thu theo status và khoảng thời gian
        @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = :status AND o.createdAt BETWEEN :from AND :to")
        BigDecimal sumTotalAmountByStatusAndDateRange(@Param("status") OrderStatus status,
                        @Param("from") LocalDateTime from,
                        @Param("to") LocalDateTime to);

        // Lấy danh sách đơn hàng theo status và khoảng thời gian (dùng cho trend)
        List<Order> findByStatusAndCreatedAtBetween(OrderStatus status, LocalDateTime from, LocalDateTime to);

        // Lấy doanh thu theo ngày - sử dụng Projection
        @Query("SELECT FUNCTION('DATE', o.createdAt) as date, SUM(o.totalAmount) as total " +
                        "FROM Order o WHERE o.status = :status AND o.createdAt BETWEEN :from AND :to " +
                        "GROUP BY FUNCTION('DATE', o.createdAt) ORDER BY date ASC")
        List<DailyRevenueProjection> getDailyRevenueByStatusAndDateRange(@Param("status") OrderStatus status,
                        @Param("from") LocalDateTime from,
                        @Param("to") LocalDateTime to);
}
