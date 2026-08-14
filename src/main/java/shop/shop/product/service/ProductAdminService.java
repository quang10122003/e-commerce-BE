/**
 * Class chỉ lo quản trị sản phẩm cho admin.
 */
package shop.shop.product.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import shop.shop.admin.dto.request.AdminCreateProductRequest;
import shop.shop.admin.dto.request.AdminUpdateProductRequest;
import shop.shop.admin.dto.response.AdminProductListResponse;
import shop.shop.admin.dto.response.AdminProductStatusResponse;
import shop.shop.admin.dto.response.AdminProductSummaryResponse;
import shop.shop.admin.mapper.AdminProductMapper;
import shop.shop.cart.repository.CartLineItemRepository;
import shop.shop.category.repository.CategoryRepository;
import shop.shop.common.ProductStatus;
import shop.shop.common.cache.CacheKeys;
import shop.shop.common.dto.response.ApiResponse;
import shop.shop.common.dto.response.PagedResponse;
import shop.shop.common.error.ApiError;
import shop.shop.common.error.ErrorCode;
import shop.shop.common.until.CurrentUserProvider;
import shop.shop.common.until.ValidationUtils;
import shop.shop.integration.cloudinary.DTO.CloudinaryImage;
import shop.shop.integration.cloudinary.service.TransactionalMediaCleanup;
import shop.shop.integration.cloudinary.service.interfaces.IMediaStorage;
import shop.shop.integration.redis.service.CacheInvalidationService;
import shop.shop.integration.redis.service.interfaces.ICacheService;
import shop.shop.product.entity.Product;
import shop.shop.product.mapper.ProductMapper;
import shop.shop.product.repository.ProductRepository;
import shop.shop.productImage.entity.ProductImageEntity;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductAdminService {

    ProductRepository productRepository;
    ProductMapper productMapper;
    AdminProductMapper adminProductMapper;
    CartLineItemRepository cartLineItemRepository;
    IMediaStorage iMediaStorage;
    CategoryRepository categoryRepository;
    CurrentUserProvider currentUserProvider;
    ICacheService cacheService;
    CacheInvalidationService cacheInvalidationService;
    TransactionalMediaCleanup mediaCleanup;
    ValidationUtils validationUtils;
    Logger logger = LoggerFactory.getLogger(this.getClass());

    // Lấy danh sách sản phẩm admin theo bộ lọc và phân trang.
    public ApiResponse<AdminProductListResponse> getAdminProducts(Long catagoryId, String search, String status,
            Pageable pageable) {
        String normalizedSearch = validationUtils.normalize(search);
        ProductStatus normalizedStatus = normalizeStatus(status);
        String cacheKey = CacheKeys.adminProductList(
                catagoryId,
                normalizedSearch,
                normalizedStatus == null ? null : normalizedStatus.name(),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort().toString());

        AdminProductListResponse cachedProducts = cacheService.getPayload(cacheKey, AdminProductListResponse.class);
        if (cachedProducts != null) {
            return ApiResponse.success("Lấy danh sách sản phẩm thành công", cachedProducts);
        }

        Page<Product> productPage = productRepository.findAdminProducts(catagoryId, normalizedSearch,
                normalizedStatus, pageable);
        Map<Long, Product> productsWithImages = findProductsWithImages(productPage.getContent());

        Page<AdminProductSummaryResponse> products = productPage
                .map(product -> adminProductMapper
                        .toSummary(productsWithImages.getOrDefault(product.getId(), product)));

        AdminProductListResponse response = AdminProductListResponse.builder()
                .products(PagedResponse.from(products))
                .build();

        cacheService.set(cacheKey, response, Duration.ofHours(1));

        return ApiResponse.success("Lấy danh sách sản phẩm thành công", response);
    }

    // Lấy thêm collection ảnh cho các sản phẩm trong trang hiện tại.
    private Map<Long, Product> findProductsWithImages(List<Product> products) {
        List<Long> productIds = products.stream()
                .map(Product::getId)
                .toList();

        if (productIds.isEmpty()) {
            return Map.of();
        }

        return productRepository.findAdminProductsWithImagesByIdIn(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    @Transactional
    public ApiResponse<AdminProductStatusResponse> updateProductStatus(Long productId, ProductStatus status) {
        if (status == null) {
            throw new ApiError(ErrorCode.BAD_REQUEST);
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ApiError(ErrorCode.PRODUCT_NOT_FOUND));

        ProductStatus beforeStatus = product.getStatus();
        product.setStatus(status);
        invalidateProductCachesAfterCommit(productId);

        logger.info("admin voi id:{} vừa cập nhật trang thái sản phẩm {} -> {} với sản phẩm id:{} ",
                currentUserProvider.getCurrentUser().getId(), beforeStatus, status, productId);

        return ApiResponse.success("Cap nhat trang thai san pham thanh cong",
                AdminProductStatusResponse.builder()
                        .productId(product.getId())
                        .status(product.getStatus())
                        .build());
    }

    @Transactional
    public ApiResponse<AdminProductSummaryResponse> createProduct(AdminCreateProductRequest request,
            MultipartFile thumbnail, List<MultipartFile> images) {
        validateCreateProductRequest(request, thumbnail);

        // Kiểm tra danh mục của sản phẩm được tạo có tồn tại hay không.
        if (!categoryRepository.existsById(request.getCategoryId())) {
            throw new ApiError(ErrorCode.CATEGORY_NOT_FOUND);
        }

        // Lưu danh sách ảnh đã upload để dọn dẹp nếu upload lỗi hoặc DB rollback.
        List<CloudinaryImage> uploadedCloudinaryImages = new ArrayList<>();

        CloudinaryImage thumbnailImage;
        List<CloudinaryImage> uploadedImages;

        try {
            // Tải thumbnailImage lên Cloudinary.
            thumbnailImage = iMediaStorage.uploadImages(List.of(thumbnail), "productsThumbnail")
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new ApiError(ErrorCode.BAD_REQUEST));

            // Thêm thumbnailImage vào biến để rollback nếu cần.
            uploadedCloudinaryImages.add(thumbnailImage);

            uploadedImages = uploadProductImages(images);

            // Thêm các ảnh của sản phẩm vào biến để rollback nếu cần.
            uploadedCloudinaryImages.addAll(uploadedImages);
        } catch (RuntimeException ex) {
            // Nếu tải ảnh lên Cloudinary lỗi thì xóa các ảnh đã upload lên Cloudinary.
            logger.error("có lỗi trong quá trình tải ảnh lên Cloudinary trong quá trình tạo sản phẩm:{}",
                    ex.getMessage());
            cleanupUploadedImages(uploadedCloudinaryImages);
            throw ex;
        }

        // Nếu DB rollback sau khi upload thành công, xóa lại ảnh vừa đẩy lên
        // Cloudinary.
        registerUploadedImageCleanupOnRollback(uploadedCloudinaryImages);

        Product product = productMapper.toProduct(request);

        product.setThumbnail(thumbnailImage.getUrl());
        product.setPublicIdUrl(thumbnailImage.getPublicId());
        product.setImages(toProductImageEntities(uploadedImages, product));

        Product savedProduct = productRepository.save(product);

        invalidateProductCachesAfterCommit(savedProduct.getId());

        logger.info("admin với với id:{} và username: {} đã thêm 1 sản phẩm mới",
                currentUserProvider.getCurrentUser().getId(),
                currentUserProvider.getCurrentUser().getEmail());

        return ApiResponse.success("Them san pham thanh cong", adminProductMapper.toSummary(savedProduct));
    }

    @Transactional
    public ApiResponse<AdminProductSummaryResponse> updateProduct(Long productId, AdminUpdateProductRequest request) {
        // Lấy product kèm category và images để cập nhật thông tin, thêm ảnh, xóa ảnh
        // trong cùng transaction.
        Product product = productRepository.findDetailById(productId)
                .orElseThrow(() -> new ApiError(ErrorCode.PRODUCT_NOT_FOUND));

        // Kiểm tra các trường nếu admin có truyền; request null vẫn hợp lệ nếu chỉ muốn
        // giữ nguyên.
        validateUpdateProductRequest(request);
        validateProductVersion(product, request);

        // Ảnh mới của sản phẩm được upload lên Cloudinary; biến này dùng để rollback.
        List<CloudinaryImage> uploadedCloudinaryImages = new ArrayList<>();

        // Ảnh cần xóa của sản phẩm sau khi cập nhật thành công; biến này dùng để xóa
        // ảnh trên Cloudinary.
        List<String> publicIdsToDeleteAfterCommit = new ArrayList<>();

        // Dùng MapStruct để cập nhật các trường cơ bản.
        if (request != null) {
            productMapper.updateProduct(product, request);
        }

        // Xóa ảnh trong DB và đưa publicId vào publicIdsToDeleteAfterCommit.
        removeProductImagesByUrl(product, request == null ? null : request.getDeleteImageUrls(),
                publicIdsToDeleteAfterCommit);

        try {
            // Nếu có thumbnail mới thì upload, gán vào product và đưa publicId của
            // thumbnail vào danh sách để xóa ảnh.
            MultipartFile thumbnail = request == null ? null : request.getThumbnail();
            if (thumbnail != null && !thumbnail.isEmpty()) {
                // Tải thumbnailImage lên Cloudinary.
                CloudinaryImage thumbnailImage = iMediaStorage.uploadImages(List.of(thumbnail), "productsThumbnail")
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> new ApiError(ErrorCode.BAD_REQUEST));

                // Thêm vào biến để rollback.
                uploadedCloudinaryImages.add(thumbnailImage);

                // Thêm thumbnailImage cũ vào danh sách để xóa ảnh khi thành công.
                mediaCleanup.addPublicId(publicIdsToDeleteAfterCommit, product.getPublicIdUrl());

                // Gán thumbnailImage mới vào DB.
                product.setThumbnail(thumbnailImage.getUrl());
                product.setPublicIdUrl(thumbnailImage.getPublicId());
            }

            // Nếu có ảnh phụ mới thì upload và thêm vào collection images của product.
            List<CloudinaryImage> uploadedImages = uploadProductImages(request == null ? null : request.getImages());

            // Thêm ảnh mới vào biến để rollback.
            uploadedCloudinaryImages.addAll(uploadedImages);

            // Thêm ảnh mới vào DB.
            addProductImages(product, uploadedImages);
        } catch (RuntimeException ex) {
            logger.error(
                    "admin voi id: {} chỉnh sửa sản phẩm id:{} nhưng có lỗi trong quá trình update ảnh lên Cloudinary",
                    currentUserProvider.getCurrentUser().getId(), productId);

            // Nếu lỗi trong quá trình upload ảnh thì xóa các ảnh vừa upload lên Cloudinary.
            cleanupUploadedImages(uploadedCloudinaryImages);
            throw ex;
        }

        // DB rollback sau khi upload thành công thì xóa ảnh mới vừa upload.
        registerUploadedImageCleanupOnRollback(uploadedCloudinaryImages);

        // DB commit thành công mới xóa ảnh cũ.
        mediaCleanup.deleteAfterCommit(publicIdsToDeleteAfterCommit.stream().distinct().toList());

        Product savedProduct = productRepository.save(product);
        invalidateProductCachesAfterCommit(savedProduct.getId());

        logger.info("admin với id: {} cập nhật sản phẩm với id:{} thành công",
                currentUserProvider.getCurrentUser().getId(), productId);

        return ApiResponse.success("Cap nhat san pham thanh cong", adminProductMapper.toSummary(savedProduct));
    }

    // Xóa sản phẩm cho admin, đồng thời dọn cart item và lên lịch xóa ảnh
    // Cloudinary.
    @Transactional
    public ApiResponse<Void> deleteProduct(Long productId) {
        Product product = findProduct(productId);

        deleteProductCore(product);

        logger.info("admin với id:{} xóa sản phẩm id:{} thành công", currentUserProvider.getCurrentUser().getId(),
                productId);

        return ApiResponse.success("Xoa san pham thanh cong voi id: " + productId, null);
    }

    // Tìm sản phẩm kèm danh sách ảnh để phục vụ nghiệp vụ xóa và dọn ảnh
    // Cloudinary.
    private Product findProduct(Long productId) {
        return productRepository.findDetailById(productId)
                .orElseThrow(() -> new ApiError(ErrorCode.PRODUCT_NOT_FOUND));
    }

    // Lấy toàn bộ sản phẩm thuộc một danh mục, kèm ảnh phụ để xóa danh mục 
    public List<Product> findProductsByCategoryId(Long categoryId) {
        return productRepository.findByCategory_Id(categoryId);
    }

    public void deleteProductCore(Product product) {
        List<String> publicIds = collectProductImagePublicIds(product);

        cartLineItemRepository.deleteByProduct_Id(product.getId());
        productRepository.delete(product);
        mediaCleanup.deleteAfterCommit(publicIds);
        invalidateProductCachesAfterCommit(product.getId());
    }

    // Gom publicId của thumbnail và các ảnh phụ để xóa trên Cloudinary sau khi xóa
    // DB thành công.
    private List<String> collectProductImagePublicIds(Product product) {
        List<String> publicIds = new ArrayList<>();

        mediaCleanup.addPublicId(publicIds, product.getPublicIdUrl());

        if (product.getImages() != null) {
            product.getImages().stream()
                    .map(image -> image.getPublicIdUrl())
                    .forEach(publicId -> mediaCleanup.addPublicId(publicIds, publicId));
        }

        return publicIds.stream().distinct().toList();
    }

    // Kiểm tra update dạng partial: trường nào null thì xem như không cập nhật
    // trường đó.
    private void validateUpdateProductRequest(AdminUpdateProductRequest request) {
        if (request == null) {
            return;
        }

        if ((request.getName() != null && validationUtils.normalize(request.getName()) == null)
                || (request.getPrice() != null && request.getPrice().signum() < 0)
                || hasFractionPart(request.getPrice())
                || (request.getStock() != null && request.getStock() < 0)) {
            throw new ApiError(ErrorCode.BAD_REQUEST);
        }

        if (request.getCategoryId() != null && !categoryRepository.existsById(request.getCategoryId())) {
            throw new ApiError(ErrorCode.CATEGORY_NOT_FOUND);
        }
    }

    // Bắt buộc client gửi version hiện tại; version cũ hoặc thiếu version sẽ không
    // được cập nhật.
    private void validateProductVersion(Product product, AdminUpdateProductRequest request) {
        if (request == null || request.getVersion() == null) {
            throw new ApiError(ErrorCode.BAD_REQUEST, "Version san pham khong duoc de trong");
        }

        // Version cũ thì không hợp lệ.
        if (!request.getVersion().equals(product.getVersion())) {
            throw new ApiError(ErrorCode.PRODUCT_VERSION_CONFLICT);
        }
    }

    // Tìm ảnh phụ có URL trùng chính xác với deleteImageUrls, lấy publicId để xóa
    // Cloudinary sau commit.
    private void removeProductImagesByUrl(Product product, List<String> deleteImageUrls,
            List<String> publicIdsToDeleteAfterCommit) {
        if (deleteImageUrls == null || deleteImageUrls.isEmpty()
                || product.getImages() == null || product.getImages().isEmpty()) {
            return;
        }

        Set<String> normalizedUrls = new HashSet<>(deleteImageUrls.stream()
                .map(validationUtils::normalize)
                .filter(url -> url != null)
                .toList());

        if (normalizedUrls.isEmpty()) {
            return;
        }

        product.getImages().removeIf(image -> {
            if (!normalizedUrls.contains(image.getUrl())) {
                return false;
            }

            mediaCleanup.addPublicId(publicIdsToDeleteAfterCommit, image.getPublicIdUrl());
            return true;
        });
    }

    // Thêm các ảnh vừa upload vào product; cascade ALL sẽ lưu ProductImageEntity
    // khi save product.
    private void addProductImages(Product product, List<CloudinaryImage> uploadedImages) {
        if (uploadedImages == null || uploadedImages.isEmpty()) {
            return;
        }

        if (product.getImages() == null) {
            product.setImages(new ArrayList<>());
        }

        product.getImages().addAll(toProductImageEntities(uploadedImages, product));
    }

    // Kiểm tra request để phát hiện lỗi.
    private void validateCreateProductRequest(AdminCreateProductRequest request, MultipartFile thumbnail) {
        if (request == null
                || validationUtils.normalize(request.getName()) == null
                || request.getPrice() == null
                || request.getPrice().signum() < 0
                || hasFractionPart(request.getPrice())
                || request.getStock() == null
                || request.getStock() < 0
                || request.getCategoryId() == null
                || thumbnail == null
                || thumbnail.isEmpty()) {
            throw new ApiError(ErrorCode.BAD_REQUEST);
        }
    }

    // Ảnh phụ không bắt buộc; nếu có thì upload vào folder riêng của product.
    private List<CloudinaryImage> uploadProductImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }

        return iMediaStorage.uploadImages(images, "products");
    }

    // Chuyển kết quả upload Cloudinary thành entity ảnh phụ và gán ngược product để
    // cascade save.
    private List<ProductImageEntity> toProductImageEntities(List<CloudinaryImage> images, Product product) {
        return images.stream()
                .map(image -> {
                    ProductImageEntity entity = new ProductImageEntity();
                    entity.setUrl(image.getUrl());
                    entity.setPublicIdUrl(image.getPublicId());
                    entity.setProduct(product);
                    return entity;
                })
                .toList();
    }

    // Chỉ dọn dẹp ảnh vừa upload khi transaction DB rollback.
    private void registerUploadedImageCleanupOnRollback(List<CloudinaryImage> images) {
        List<String> publicIds = images.stream()
                .map(CloudinaryImage::getPublicId)
                .filter(validationUtils::hasText)
                .distinct()
                .toList();

        mediaCleanup.deleteOnRollback(publicIds);
    }

    // Dùng khi lỗi xảy ra ngay trong quá trình upload, trước khi đến bước save DB.
    private void cleanupUploadedImages(List<CloudinaryImage> images) {
        List<String> publicIds = images.stream()
                .map(CloudinaryImage::getPublicId)
                .filter(validationUtils::hasText)
                .distinct()
                .toList();

        mediaCleanup.deleteNow(publicIds);
    }

    // Xóa cache sản phẩm, danh sách catalog/admin và cart snapshot sau khi commit.
    private void invalidateProductCachesAfterCommit(Long productId) {
        cacheInvalidationService.productChanged(productId);
    }

    // Kiểm tra giá có phần thập phân khác 0 hay không.
    private boolean hasFractionPart(BigDecimal value) {
        return value != null && value.stripTrailingZeros().scale() > 0;
    }

    // Chuyển chuỗi trạng thái sang ProductStatus và báo lỗi nếu giá trị không hợp lệ.
    private ProductStatus normalizeStatus(String status) {
        String normalizedStatus = validationUtils.normalize(status);

        if (normalizedStatus == null) {
            return null;
        }

        try {
            return ProductStatus.valueOf(normalizedStatus.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiError(ErrorCode.BAD_REQUEST);
        }
    }
}
