package shop.shop.payment.repo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import shop.shop.admin.Projection.PaymentStatsProjection;
import shop.shop.common.PaymentStatus;
import shop.shop.payment.entity.PaymentEntity;

@Repository
public interface PaymentRepo extends JpaRepository<PaymentEntity, Long> {
    Optional<PaymentEntity> findByTransactionRef(String transactionRef);

    // Lấy payment gắn với một order để bổ sung thông tin thanh toán cho response.
    Optional<PaymentEntity> findByOrderId(Long orderId);

    @Query("""
                SELECT p
                FROM PaymentEntity p
                JOIN FETCH p.order o
                WHERE (:status IS NULL OR p.status = :status)
                AND (:fromDt IS NULL OR p.createdAt >= :fromDt)
                AND (:toDt   IS NULL OR p.createdAt <= :toDt)
                AND (
                    :search IS NULL
                    OR LOWER(o.orderCode)       LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(p.transactionRef)  LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(p.referenceCode)   LIKE LOWER(CONCAT('%', :search, '%'))
                )
                ORDER BY p.createdAt DESC
            """)
    List<PaymentEntity> findAdminPayments(
            @Param("search") String search,
            @Param("status") PaymentStatus status,
            @Param("fromDt") LocalDateTime fromDt,
            @Param("toDt") LocalDateTime toDt);

    @Query("""
                SELECT
                    COUNT(p) as total,
                    SUM(CASE WHEN p.status = 'PENDING' THEN 1 ELSE 0 END) as pending,
                    SUM(CASE WHEN p.status = 'PAID' THEN 1 ELSE 0 END) as paid,
                    SUM(CASE WHEN p.status = 'FAILED' THEN 1 ELSE 0 END) as failed,
                    SUM(CASE WHEN p.status = 'PAID_LATE' THEN 1 ELSE 0 END) as paidLate
                FROM PaymentEntity p
            """)
    PaymentStatsProjection getStats();
}
