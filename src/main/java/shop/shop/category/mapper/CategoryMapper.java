package shop.shop.category.mapper;

import org.mapstruct.Mapper;
import shop.shop.admin.dto.response.AdminListNewCategory;
import shop.shop.category.dto.response.CategorySummaryResponse;
import shop.shop.category.entity.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategorySummaryResponse toSummary(Category category);

    AdminListNewCategory toAdminListNewCategory(Category category);
}
