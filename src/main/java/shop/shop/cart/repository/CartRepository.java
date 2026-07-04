package shop.shop.cart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import shop.shop.cart.entity.Cart;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserId(Long userId);
}
