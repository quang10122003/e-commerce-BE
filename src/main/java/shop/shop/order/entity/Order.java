package shop.shop.order.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import shop.shop.common.CancelledBy;
import shop.shop.common.OrderStatus;
import shop.shop.common.PaymentMethod;
import shop.shop.user.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  @OnDelete(action = OnDeleteAction.SET_NULL)
  User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  OrderStatus status;

  // Người thực hiện hủy đơn hàng.
  @Enumerated(EnumType.STRING)
  @Column(name = "cancelled_by", length = 10)
  CancelledBy cancelledBy;

  @Column(name = "shipping_name", nullable = false)
  String shippingName;

  @Column(name = "order_code", unique = true, length = 50)
  String orderCode;

  @Column(name = "shipping_phone", nullable = false)
  String shippingPhone;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_method")
  PaymentMethod paymentMethod;

  @Column(name = "shipping_address", columnDefinition = "TEXT", nullable = false)
  String shippingAddress;

  @Builder.Default
  @Column(name = "total_amount", precision = 12)
  BigDecimal totalAmount = BigDecimal.ZERO;

  @Builder.Default
  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  List<OrderItem> items = new ArrayList<>();

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  LocalDateTime updatedAt;
}