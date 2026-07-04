package shop.shop.cart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import shop.shop.cart.entity.Cart;
import shop.shop.cart.entity.CartLineItem;
import shop.shop.product.entity.Product;

import java.util.List;
import java.util.Optional;

public interface CartLineItemRepository extends JpaRepository<CartLineItem, Long> {

  @Query("""
      SELECT ci
      FROM CartLineItem ci
      JOIN FETCH ci.product p
      JOIN FETCH ci.cart c
      WHERE c.user.id = :userId
        AND p.status = shop.shop.common.ProductStatus.ACTIVE
      ORDER BY ci.createdAt DESC, ci.id DESC
      """)
  List<CartLineItem> findByUserId(@Param("userId") Long userId);

  Optional<CartLineItem> findByCartAndProduct(Cart cart, Product product);

  // Tìm sản phẩm trong giỏ của user hiện tại để xóa đúng phạm vi sở hữu.
  Optional<CartLineItem> findByCart_User_IdAndProduct_Id(Long userId, Long productId);

  // Xóa các sản phẩm đã đặt hàng khỏi giỏ của đúng user hiện tại.
  @Modifying
  @Query("""
      DELETE FROM CartLineItem ci
      WHERE ci.cart.user.id = :userId
        AND ci.product.id IN :productIds
      """)
  void deleteByUserIdAndProductIds(
      @Param("userId") Long userId,
      @Param("productIds") List<Long> productIds);

  void deleteByProduct_Id(Long productId);
}
