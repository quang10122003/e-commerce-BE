// class chỉ lo nghiệp vụ crud cho cart 
package shop.shop.category.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import shop.shop.admin.dto.request.AdminCreateCategoriRequest;
import shop.shop.admin.dto.request.AdminUpdateCategoriRequest;
import shop.shop.category.dto.response.CategorySummaryResponse;
import shop.shop.category.entity.Category;
import shop.shop.category.mapper.CategoryMapper;
import shop.shop.category.repository.CategoryRepository;
import shop.shop.common.cache.CacheKeys;
import shop.shop.common.dto.response.ApiResponse;
import shop.shop.common.error.ApiError;
import shop.shop.common.error.ErrorCode;
import shop.shop.common.until.CurrentUserProvider;
import shop.shop.integration.cloudinary.DTO.CloudinaryImage;
import shop.shop.integration.cloudinary.service.interfaces.IMediaStorage;
import shop.shop.integration.cloudinary.service.TransactionalMediaCleanup;
import shop.shop.integration.redis.service.CacheInvalidationService;
import shop.shop.integration.redis.service.interfaces.ICacheService;
import shop.shop.product.service.ProductAdminService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.core.type.TypeReference;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryService {
    CurrentUserProvider currentUserProvider;
    Logger logger = LoggerFactory.getLogger(this.getClass());
    CategoryRepository categoryRepository;
    CategoryMapper categoryMapper;
    IMediaStorage iMediaStorage;
    ProductAdminService productAdminService;
    ICacheService cacheService;
    CacheInvalidationService cacheInvalidationService;
    TransactionalMediaCleanup transactionalMediaCleanup;

    // Lấy toàn bộ danh mục dùng chung cho user/admin
    public ApiResponse<List<CategorySummaryResponse>> getAllCategories() {
        String publicCacheKey = CacheKeys.categoriesAll();

        List<CategorySummaryResponse> cachedCategories = cacheService.getPayload(publicCacheKey,
                new TypeReference<List<CategorySummaryResponse>>() {
                });

        if (cachedCategories != null) {
            return ApiResponse.success("lay danh list danh muc thanh cong", cachedCategories);
        }

        List<CategorySummaryResponse> categories = categoryRepository.findAll()
                .stream()
                .map(categoryMapper::toSummary)
                .toList();
        cacheService.set(publicCacheKey, categories, Duration.ofHours(5));

        return ApiResponse.success("lay danh list danh muc thanh cong", categories);
    }

    @Transactional
    public ApiResponse<CategorySummaryResponse> createCategori(AdminCreateCategoriRequest data, MultipartFile file) {
        CloudinaryImage uploadedImage = null;
        try {
            if (data == null || data.getName() == null || data.getName().isBlank()) {
                throw new ApiError(ErrorCode.BAD_REQUEST, "Ten danh muc khong duoc de trong");
            }

            String categoryName = data.getName().trim();
            if (categoryRepository.existsByNormalizedName(categoryName)) {
                throw new ApiError(ErrorCode.CATEGORY_ALREADY_EXISTS);
            }

            if (file == null || file.isEmpty()) {
                throw new ApiError(ErrorCode.BAD_REQUEST, "Anh danh muc khong duoc de trong");
            }

            uploadedImage = iMediaStorage.uploadImages(List.of(file), "categories").get(0);

            Category category = new Category();
            category.setName(categoryName);
            category.setImage(uploadedImage.getUrl());
            category.setPublicIdUrl(uploadedImage.getPublicId());

            Category savedCategory = categoryRepository.save(category);
            cacheInvalidationService.categoryChanged();
            logger.info("admin id:{} thêm 1 danh mục mới id:{}", currentUserProvider.getCurrentUser().getId(),
                    savedCategory.getId());

            return ApiResponse.success("Tao danh muc thanh cong", categoryMapper.toSummary(savedCategory));
        } catch (Exception e) {
            if (uploadedImage != null) {
                transactionalMediaCleanup.deleteNow(List.of(uploadedImage.getPublicId()));
            }
            throw e;
        }
    }

    @Transactional
    public ApiResponse<CategorySummaryResponse> updateCategori(Long id, AdminUpdateCategoriRequest data,
            MultipartFile file) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ApiError(ErrorCode.CATEGORY_NOT_FOUND));

        if (data.getName() != null && !data.getName().isBlank()) {
            category.setName(data.getName().trim());
        }

        if (file != null && !file.isEmpty()) {
            String oldPublicId = category.getPublicIdUrl();
            CloudinaryImage uploadedImage = iMediaStorage.uploadImages(List.of(file), "categories").get(0);

            // Ảnh cũ chỉ xóa khi commit thành công; ảnh mới chỉ xóa nếu rollback.
            transactionalMediaCleanup.deleteAfterCommit(List.of(oldPublicId));
            transactionalMediaCleanup.deleteOnRollback(List.of(uploadedImage.getPublicId()));

            category.setImage(uploadedImage.getUrl());
            category.setPublicIdUrl(uploadedImage.getPublicId());
        }
        logger.info("admin id:{} chỉnh sửa danh mục Id:{} với data {} ", currentUserProvider.getCurrentUser().getId(),
                id, data);
        cacheInvalidationService.categoryChanged();
        return ApiResponse.success("da chinh sua danh muc thanh cong", categoryMapper.toSummary(category));
    }

    @Transactional
    public ApiResponse<Void> deleteCategori(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ApiError(ErrorCode.CATEGORY_NOT_FOUND));

        // Tan dung lai nghiep vu xoa san pham de don cart item, xoa san pham va len
        // lich xoa anh san pham.
        productAdminService.findProductsByCategoryId(id)
                .forEach(productAdminService::deleteProductCore);

        // Xoa danh muc sau khi cac san pham thuoc danh muc da duoc xu ly.
        List<String> publicIds = collectCategoryImagePublicIds(category);
        categoryRepository.delete(category);

        cacheInvalidationService.categoryChanged();

        transactionalMediaCleanup.deleteAfterCommit(publicIds.stream().distinct().toList());
        logger.info("admin {} xóa danh mục {} ", currentUserProvider.getCurrentUser().getId(), id);

        return ApiResponse.success("Xoa danh muc thanh cong voi id: " + id, null);
    }

    // Lay publicId anh danh muc de xoa tren Cloudinary sau khi DB commit thanh
    // cong.
    private List<String> collectCategoryImagePublicIds(Category category) {
        List<String> publicIds = new ArrayList<>();
        transactionalMediaCleanup.addPublicId(publicIds, category.getPublicIdUrl());
        return publicIds;
    }
}