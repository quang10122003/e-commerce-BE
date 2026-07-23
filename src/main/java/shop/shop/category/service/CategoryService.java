package shop.shop.category.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import shop.shop.admin.dto.request.AdminCreateCategoriRequest;
import shop.shop.admin.dto.request.AdminUpdateCategoriRequest;
import shop.shop.admin.dto.response.AdminCatagoryOverviewRepone;
import shop.shop.category.dto.response.CategorySummaryResponse;
import shop.shop.category.entity.Category;
import shop.shop.category.mapper.CategoryMapper;
import shop.shop.category.repository.CategoryRepository;
import shop.shop.common.cache.CacheKeys;
import shop.shop.common.dto.response.ApiResponse;
import shop.shop.common.error.ApiError;
import shop.shop.common.error.ErrorCode;
import shop.shop.common.until.CurrentUserClass;
import shop.shop.integration.cloudinary.DTO.CloudinaryImage;
import shop.shop.integration.cloudinary.service.CloudinaryService;
import shop.shop.integration.redis.service.CatalogCacheService;
import shop.shop.product.service.ProductService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.core.type.TypeReference;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryService {
    CurrentUserClass currentUserClass;
    Logger logger = LoggerFactory.getLogger(this.getClass());
    CategoryRepository categoryRepository;
    CategoryMapper categoryMapper;
    CloudinaryService cloudinaryService;
    ProductService productService;
    CatalogCacheService catalogCacheService;

    // Lấy toàn bộ danh mục dùng chung cho user/admin, ưu tiên đọc từ Redis trước khi query database.
public ApiResponse<List<CategorySummaryResponse>> getAllCategories() {
    String publicCacheKey = CacheKeys.categoriesAll();

    List<CategorySummaryResponse> cachedCategories = catalogCacheService.getPayload(publicCacheKey, new TypeReference<List<CategorySummaryResponse>>() {});

    if (cachedCategories != null) {
        return ApiResponse.success("lay danh list danh muc thanh cong", cachedCategories);
    }

    List<CategorySummaryResponse> categories = categoryRepository.findAll()
            .stream()
            .map(categoryMapper::toSummary)
            .toList();
    catalogCacheService.set(publicCacheKey, categories, Duration.ofHours(5));

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

            uploadedImage = cloudinaryService.uploadImages(List.of(file), "categories").get(0);

            Category category = new Category();
            category.setName(categoryName);
            category.setImage(uploadedImage.getUrl());
            category.setPublicIdUrl(uploadedImage.getPublicId());

            Category savedCategory = categoryRepository.save(category);
            catalogCacheService.registerCategoryCacheDeleteAfterCommit();
            logger.info("admin id:{} thêm 1 danh mục mới id:{}",currentUserClass.getCurrentUser().getId(),savedCategory.getId());

            return ApiResponse.success("Tao danh muc thanh cong", categoryMapper.toSummary(savedCategory));
        } catch (Exception e) {
            if (uploadedImage != null) {
                cloudinaryService.deleteImage(List.of(uploadedImage.getPublicId()));
            }
            throw e;
        }
    }

    @Transactional
    public ApiResponse<CategorySummaryResponse> updateCategori(Long id, AdminUpdateCategoriRequest data, MultipartFile file) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> {
            return new ApiError(ErrorCode.CATEGORY_NOT_FOUND);
        });

        if (data.getName() != null && !data.getName().isBlank()) {
            category.setName(data.getName().trim());
        }

        if (file != null && !file.isEmpty()) {
            String oldPublicId = category.getPublicIdUrl();
            CloudinaryImage uploadedImage = cloudinaryService.uploadImages(List.of(file), "categories").get(0);

            registerImageCleanup(oldPublicId, uploadedImage.getPublicId());

            category.setImage(uploadedImage.getUrl());
            category.setPublicIdUrl(uploadedImage.getPublicId());
        }
        logger.info("admin id:{} chỉnh sửa danh mục Id:{} với data {} ",currentUserClass.getCurrentUser().getId(),id,data);
        catalogCacheService.registerCategoryCacheDeleteAfterCommit();
        System.out.println("laoding cache");
        return ApiResponse.success("da chinh sua danh muc thanh cong", categoryMapper.toSummary(category));
    }

    @Transactional
    public ApiResponse<Void> deleteCategori(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ApiError(ErrorCode.CATEGORY_NOT_FOUND));

        // Tan dung lai nghiep vu xoa san pham de don cart item, xoa san pham va len lich xoa anh san pham.
        productService.findProductsByCategoryId(id)
                .forEach(productService::deleteProductCore);

        // Xoa danh muc sau khi cac san pham thuoc danh muc da duoc xu ly.
        List<String> publicIds = collectCategoryImagePublicIds(category);
        categoryRepository.delete(category);

        catalogCacheService.registerCategoryCacheDeleteAfterCommit();

        registerImageCleanupAfterCommit(publicIds.stream().distinct().toList());
        logger.info("admin {} xóa danh mục {} ",currentUserClass.getCurrentUser().getId(),id);

        return ApiResponse.success("Xoa danh muc thanh cong voi id: " + id, null);
    }

    // Lay publicId anh danh muc de xoa tren Cloudinary sau khi DB commit thanh cong.
    private List<String> collectCategoryImagePublicIds(Category category) {
        List<String> publicIds = new ArrayList<>();

        addPublicId(publicIds, category.getPublicIdUrl());

        return publicIds;
    }

    // Rollback anh khi xay ra loi.
    private void registerImageCleanup(String oldPublicId, String uploadedPublicId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            // Luu anh moi thanh cong vao DB; khi Transactional commit thanh cong thi xoa anh cu.
            @Override
            public void afterCommit() {
                cloudinaryService.deleteImage(List.of(oldPublicId));
            }

            // Khi Transactional ket thuc ma rollback thi xoa anh moi vua cap nhat.
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    cloudinaryService.deleteImage(List.of(uploadedPublicId));
                }
            }
        });
    }

    // Chi xoa anh tren Cloudinary sau khi transaction xoa danh muc trong DB commit thanh cong.
    private void registerImageCleanupAfterCommit(List<String> publicIds) {
        if (publicIds.isEmpty()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                logger.info("xóa ảnh trên cloudinary");
                cloudinaryService.deleteImage(publicIds);
            }
        });
    }

    // Them publicId hop le vao danh sach, bo qua gia tri null hoac chuoi rong.
    private void addPublicId(List<String> publicIds, String publicId) {
        if (publicId != null && !publicId.isBlank()) {
            publicIds.add(publicId);
        }
    }


    public ApiResponse<AdminCatagoryOverviewRepone> getOverviewCategory(){
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
