// class đảm nhiệm chỉ đọc dữ liệu phục vụ báo cáo tổng quan danh mục cho admin dashboard
package shop.shop.category.service;

import org.springframework.stereotype.Service;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import shop.shop.admin.dto.response.AdminCatagoryOverviewRepone;
import shop.shop.category.mapper.CategoryMapper;
import shop.shop.category.repository.CategoryRepository;
import shop.shop.common.dto.response.ApiResponse;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryOverviewQueryService {
    CategoryRepository categoryRepository;
    CategoryMapper categoryMapper;

    public ApiResponse<AdminCatagoryOverviewRepone> getOverviewCategory() {
        Long totalCatagory = categoryRepository.count();
        String topCategory = categoryRepository.findTopCategoryNameByProductCount();
        Long emptyCategories = categoryRepository.countEmptyCategories();
        var listNewCategory = categoryRepository.findTop5ByOrderByCreatedAtDesc()
                .stream()
                .map(categoryMapper::toAdminListNewCategory)
                .toList();

        AdminCatagoryOverviewRepone adminCatagoryOverviewRepone = new AdminCatagoryOverviewRepone(
                totalCatagory,
                topCategory,
                emptyCategories,
                listNewCategory);

        return ApiResponse.success("lấy data overview danh mục thành công", adminCatagoryOverviewRepone);
    }
}