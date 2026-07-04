package shop.shop.category.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import shop.shop.category.entity.Category;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Kiểm tra trùng tên.
    @Query("select count(c) > 0 from Category c where lower(trim(c.name)) = lower(:name)")
    boolean existsByNormalizedName(@Param("name") String name);

    // Lấy tên danh mục có nhiều sản phẩm nhất.
    @Query(value = "select c.name from categories c left join products p on p.category_id = c.id group by c.id, c.name order by count(p.id) desc limit 1 ",nativeQuery = true)
    String findTopCategoryNameByProductCount();

    // Lấy count danh mục không có sản phẩm.
    @Query("""
                select count(c)
                from Category c
                where c.products is empty
            """)
    Long countEmptyCategories();

    // Lấy ra 5 danh mục mới được tạo.
    List<Category> findTop5ByOrderByCreatedAtDesc();
    
}
