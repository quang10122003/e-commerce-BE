package shop.shop.product.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import shop.shop.common.ProductStatus;
import shop.shop.product.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @EntityGraph(attributePaths = "category")
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Product> findByStatusAndCategory_Id(ProductStatus status, Long categoryId, Pageable pageable);

    // Tìm sản phẩm đang bán theo danh mục và từ khóa người dùng nhập.
    @EntityGraph(attributePaths = "category")
    @Query("""
            SELECT p FROM Product p
            WHERE p.status = shop.shop.common.ProductStatus.ACTIVE
            AND (:categoryId IS NULL OR p.category.id = :categoryId)
            AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Product> findActiveProducts(
            @Param("categoryId") Long categoryId,
            @Param("search") String search,
            Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "category")
    Page<Product> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "category")
    @Query("""
            SELECT p FROM Product p
            WHERE (:catagoryId IS NULL OR p.category.id = :catagoryId)
            AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))
            AND (:status IS NULL OR p.status = :status)
            """)
    Page<Product> findAdminProducts(
            @Param("catagoryId") Long catagoryId,
            @Param("search") String search,
            @Param("status") ProductStatus status,
            Pageable pageable);

    // Lấy ảnh cho đúng nhóm sản phẩm đã phân trang để tránh fetch collection trong query pageable.
    @EntityGraph(attributePaths = {"category", "images"})
    @Query("SELECT DISTINCT p FROM Product p WHERE p.id IN :productIds")
    List<Product> findAdminProductsWithImagesByIdIn(@Param("productIds") List<Long> productIds);

    @EntityGraph(attributePaths = "category")
    List<Product> findTop6ByStatusOrderByPurchasesDescCreatedAtDesc(ProductStatus status);

    @EntityGraph(attributePaths = {"category", "images"})
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findDetailById(@Param("id") Long id);

    @EntityGraph(attributePaths = "images")
    List<Product> findByCategory_Id(Long categoryId);

    // Lấy sản phẩm đang bán theo danh sách id để tính tiền thanh toán.
    @Query("""
            SELECT p
            FROM Product p
            WHERE p.id IN :productIds
              AND p.status = shop.shop.common.ProductStatus.ACTIVE
            """)
    List<Product> findActiveByIds(@Param("productIds") List<Long> productIds);

    @Query("select count(*) from Product")
    long countTotalProducts();

    @Query("select count(p) from Product p where p.status = shop.shop.common.ProductStatus.ACTIVE")
    long countProductsActive();
}
