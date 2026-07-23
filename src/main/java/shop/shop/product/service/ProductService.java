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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import shop.shop.admin.dto.request.AdminCreateProductRequest;
import shop.shop.admin.dto.request.AdminUpdateProductRequest;
import shop.shop.admin.dto.response.AdminProductListResponse;
import shop.shop.admin.dto.response.AdminProductStatusResponse;
import shop.shop.admin.dto.response.AdminProductSummaryResponse;
import shop.shop.admin.mapper.AdminProductMapper;
import shop.shop.cart.repository.CartLineItemRepository;
import shop.shop.category.repository.CategoryRepository;
import shop.shop.common.dto.response.ApiResponse;
import shop.shop.common.dto.response.PagedResponse;
import shop.shop.common.error.ApiError;
import shop.shop.common.error.ErrorCode;
import shop.shop.common.until.CurrentUserClass;
import shop.shop.integration.cloudinary.DTO.CloudinaryImage;
import shop.shop.integration.cloudinary.service.CloudinaryService;
import shop.shop.integration.redis.service.CatalogCacheService;
import shop.shop.integration.redis.service.CartCacheService;
import shop.shop.product.dto.response.ProductSummaryResponse;
import shop.shop.product.dto.response.Productdetail;
import shop.shop.common.ProductStatus;
import shop.shop.common.cache.CacheKeys;
import shop.shop.product.entity.Product;
import shop.shop.product.mapper.ProductMapper;
import shop.shop.product.repository.ProductRepository;
import shop.shop.productImage.entity.ProductImageEntity;
import tools.jackson.core.type.TypeReference;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductService {

    ProductRepository productRepository;
    ProductMapper productMapper;
    AdminProductMapper adminProductMapper;
    CartLineItemRepository cartLineItemRepository;
    CloudinaryService cloudinaryService;
    CategoryRepository categoryRepository;
    CurrentUserClass currentUserClass;
    CatalogCacheService catalogCacheService;
    CartCacheService cartCacheService;
    Logger logger = LoggerFactory.getLogger(this.getClass());

    // Lấy danh sách sản phẩm public dạng phân trang, cache theo bộ lọc và paging.
    public PagedResponse<ProductSummaryResponse> getActiveProductsPaged(Long categoryId, String search,Pageable pageable) {
        String cacheKey = CacheKeys.productList(
                categoryId == null ? null : categoryId.toString(),
                search,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort().toString());

        PagedResponse<ProductSummaryResponse> cachedProducts = catalogCacheService.getPayload(cacheKey, new TypeReference<PagedResponse<ProductSummaryResponse>>() {});
        if (cachedProducts != null) {
            return cachedProducts;
        }

        Page<ProductSummaryResponse> activeProducts = getActiveProducts(categoryId, search, pageable);
        PagedResponse<ProductSummaryResponse> pagedResponse = PagedResponse.from(activeProducts);

        catalogCacheService.set(cacheKey, pagedResponse, Duration.ofHours(1));

        return pagedResponse;
    }

    public Page<ProductSummaryResponse> getActiveProducts(Pageable pageable) {
        return productRepository.findByStatus(ProductStatus.ACTIVE, pageable)
                .map(productMapper::toSummary);
    }

    public Page<ProductSummaryResponse> getActiveProductsByCategory(Long categoryId, Pageable pageable) {
        return productRepository.findByStatusAndCategory_Id(ProductStatus.ACTIVE, categoryId, pageable)
                .map(productMapper::toSummary);
    }

    // Lấy sản phẩm đang bán theo danh mục và từ khóa tìm kiếm.
    public Page<ProductSummaryResponse> getActiveProducts(Long categoryId, String search, Pageable pageable) {
        String normalizedSearch = normalize(search);

        if (normalizedSearch == null) {
            if (categoryId != null) {
                return getActiveProductsByCategory(categoryId, pageable);
            }

            return getActiveProducts(pageable);
        }

        return productRepository.findActiveProducts(categoryId, normalizedSearch, pageable)
                .map(productMapper::toSummary);
    }

    public List<ProductSummaryResponse> getTopSelling() {
        String cacheKey = CacheKeys.productTopSelling();
        
        List<ProductSummaryResponse> cachedProducts = catalogCacheService.getPayload(cacheKey, new TypeReference<List<ProductSummaryResponse>>() {});
        if (cachedProducts != null) {
            return cachedProducts;
        }
        List<ProductSummaryResponse> products = productRepository
                .findTop6ByStatusOrderByPurchasesDescCreatedAtDesc(ProductStatus.ACTIVE)
                .stream()
                .map(productMapper::toSummary)
                .toList();

        catalogCacheService.set(cacheKey, products, Duration.ofMinutes(15));

        return products;
    }

    public Productdetail getProductById(Long id) {
        String cacheKey  = CacheKeys.productDetail(id);
        Productdetail cachedProduct  = catalogCacheService.getPayload(cacheKey, Productdetail.class);
        if (cachedProduct != null) {
            return cachedProduct;
        }

        Productdetail productDetail = productRepository.findDetailById(id)
            .map(productMapper::toDetail)
                .orElseThrow(() -> new ApiError(ErrorCode.PRODUCT_NOT_FOUND));
        
        catalogCacheService.set(cacheKey, productDetail, Duration.ofHours(1));

        return productDetail;
    }

    // Chuan hoa chuoi.
    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ProductStatus normalizeStatus(String status) {
        String normalizedStatus = normalize(status);
        if (normalizedStatus == null) {
            return null;
        }

        try {
            return ProductStatus.valueOf(normalizedStatus.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiError(ErrorCode.BAD_REQUEST);
        }
    }

    // Kiểm tra giá có phần thập phân khác 0 hay không.
    private boolean hasFractionPart(BigDecimal value) {
        return value != null && value.stripTrailingZeros().scale() > 0;
    }

    // Lấy danh sách sản phẩm admin, cache theo bộ lọc/paging để giảm tải query.
    public ApiResponse<AdminProductListResponse> getAdminProducts(Long catagoryId, String search, String status,
            Pageable pageable) {
        String normalizedSearch = normalize(search);
        ProductStatus normalizedStatus = normalizeStatus(status);
        String cacheKey = CacheKeys.adminProductList(
            catagoryId,
            normalizedSearch,
            normalizedStatus == null ? null : normalizedStatus.name(),
            pageable.getPageNumber(),
            pageable.getPageSize(),
            pageable.getSort().toString());

        AdminProductListResponse cachedProducts = catalogCacheService.getPayload(cacheKey, AdminProductListResponse.class);
        if (cachedProducts != null) {
            return ApiResponse.success("Lấy danh sách sản phẩm thành công", cachedProducts);
        }

        Page<Product> productPage = productRepository.findAdminProducts(catagoryId, normalizedSearch, normalizedStatus,
                pageable);
        Map<Long, Product> productsWithImages = findProductsWithImages(productPage.getContent());

        Page<AdminProductSummaryResponse> products = productPage
                .map(product -> adminProductMapper
                        .toSummary(productsWithImages.getOrDefault(product.getId(), product)));

        AdminProductListResponse response = AdminProductListResponse.builder()
                .products(PagedResponse.from(products))
                .build();

        catalogCacheService.set(cacheKey, response, Duration.ofHours(1));

        return ApiResponse.success("Lấy danh sách sản phẩm thành công", response);
    }

    // Lấy thêm collection images cho các sản phẩm trong page hiện tại.
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
        catalogCacheService.registerProductCacheDeleteAfterCommit(productId);
        cartCacheService.registerCartCacheDeleteAfterCommit();

        logger.info("admin voi id:{} vừa cập nhật trang thái sản phẩm {} -> {} với sản phẩm id:{} ",
                currentUserClass.getCurrentUser().getId(), beforeStatus, status, productId);

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

        // Kiem tra danh muc cua san pham duoc tao co ton tai hay khong.
        if (!categoryRepository.existsById(request.getCategoryId())) {
            throw new ApiError(ErrorCode.CATEGORY_NOT_FOUND);
        }

        // Luu danh sach anh da upload de don dep neu upload loi hoac DB rollback.
        List<CloudinaryImage> uploadedCloudinaryImages = new ArrayList<>();

        CloudinaryImage thumbnailImage;

        List<CloudinaryImage> uploadedImages;

        try {
            // Tai thumbnailImage len Cloudinary.
            thumbnailImage = cloudinaryService.uploadImages(List.of(thumbnail), "productsThumbnail")
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new ApiError(ErrorCode.BAD_REQUEST));
            // Them thumbnailImage vao bien de rollback neu can.
            uploadedCloudinaryImages.add(thumbnailImage);

            uploadedImages = uploadProductImages(images);
            // Them cac anh cua san pham vao bien de rollback neu can.
            uploadedCloudinaryImages.addAll(uploadedImages);
        } catch (RuntimeException ex) {
            // Neu tai anh len Cloudinary loi thi xoa cac anh da upload len Cloudinary.
            logger.error("có lỗi trong quá trình tải ảnh lên Cloudinary trong quá trình tạo sản phẩm:{}",
                    ex.getMessage());
            cleanupUploadedImages(uploadedCloudinaryImages);
            throw ex;
        }

        // Neu DB rollback sau khi upload thanh cong, xoa lai anh vua day len
        // Cloudinary.
        registerUploadedImageCleanupOnRollback(uploadedCloudinaryImages);

        Product product = productMapper.toProduct(request);

        product.setThumbnail(thumbnailImage.getUrl());

        product.setPublicIdUrl(thumbnailImage.getPublicId());

        product.setImages(toProductImageEntities(uploadedImages, product));

        Product savedProduct = productRepository.save(product);

        catalogCacheService.registerProductCacheDeleteAfterCommit(savedProduct.getId());
        cartCacheService.registerCartCacheDeleteAfterCommit();

        logger.info("admin với với id:{} và username: {} đã thêm 1 sản phẩm mới",
                currentUserClass.getCurrentUser().getId(),
                currentUserClass.getCurrentUser().getEmail());
        return ApiResponse.success("Them san pham thanh cong", adminProductMapper.toSummary(savedProduct));
    }

    @Transactional
    public ApiResponse<AdminProductSummaryResponse> updateProduct(Long productId, AdminUpdateProductRequest request) {
        // Lay product kem category va images de cap nhat thong tin, them anh, xoa anh
        // trong cung transaction.
        Product product = productRepository.findDetailById(productId)
                .orElseThrow(() -> new ApiError(ErrorCode.PRODUCT_NOT_FOUND));

        // Kiem tra cac truong neu admin co truyen; request null van hop le neu chi muon
        // giu nguyen.
        validateUpdateProductRequest(request);
        validateProductVersion(product, request);

        // Anh moi cua san pham duoc upload len Cloudinary; bien nay dung de rollback.
        List<CloudinaryImage> uploadedCloudinaryImages = new ArrayList<>();

        // Anh can xoa cua san pham sau khi cap nhat thanh cong; bien nay dung de xoa
        // anh tren Cloudinary.
        List<String> publicIdsToDeleteAfterCommit = new ArrayList<>();

        // Dung MapStruct de cap nhat cac truong co ban.
        if (request != null) {
            productMapper.updateProduct(product, request);
        }

        // Xoa anh trong DB va dua publicId vao publicIdsToDeleteAfterCommit.
        removeProductImagesByUrl(product, request == null ? null : request.getDeleteImageUrls(),
                publicIdsToDeleteAfterCommit);

        try {
            // Neu co thumbnail moi thi upload, gan vao product, va dua publicId cua
            // thumbnail vao danh sach de xoa anh.
            MultipartFile thumbnail = request == null ? null : request.getThumbnail();
            if (thumbnail != null && !thumbnail.isEmpty()) {
                // Tai thumbnailImage len Cloudinary.
                CloudinaryImage thumbnailImage = cloudinaryService.uploadImages(List.of(thumbnail), "productsThumbnail")
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> new ApiError(ErrorCode.BAD_REQUEST));

                // Them vao bien de rollback.
                uploadedCloudinaryImages.add(thumbnailImage);
                // Them thumbnailImage cu vao danh sach de xoa anh khi thanh cong.
                addPublicId(publicIdsToDeleteAfterCommit, product.getPublicIdUrl());
                // Gan thumbnailImage moi vao DB.
                product.setThumbnail(thumbnailImage.getUrl());
                product.setPublicIdUrl(thumbnailImage.getPublicId());
            }

            // Neu co anh phu moi thi upload va them vao collection images cua product.
            List<CloudinaryImage> uploadedImages = uploadProductImages(request == null ? null : request.getImages());
            // Them vao bien de rollback.
            uploadedCloudinaryImages.addAll(uploadedImages);
            // Them anh moi vao DB.
            addProductImages(product, uploadedImages);
        } catch (RuntimeException ex) {
            logger.error(
                    "admin voi id: {} chỉnh sửa sản phẩm id:{} nhưng có lỗi trong quá trình update ảnh lên Cloudinary",
                    currentUserClass.getCurrentUser().getId(), productId);
            // Neu loi trong qua trinh upload anh thi xoa cac anh vua upload len Cloudinary.
            cleanupUploadedImages(uploadedCloudinaryImages);
            throw ex;
        }

        // DB rollback sau khi upload thanh cong thi xoa anh moi vua upload.
        registerUploadedImageCleanupOnRollback(uploadedCloudinaryImages);

        // DB commit thanh cong moi xoa anh cu.
        registerProductImageCleanup(publicIdsToDeleteAfterCommit.stream().distinct().toList());

        Product savedProduct = productRepository.save(product);
        catalogCacheService.registerProductCacheDeleteAfterCommit(savedProduct.getId());
        cartCacheService.registerCartCacheDeleteAfterCommit();
        logger.info("admin với id: {} cập nhật sản phẩm với id:{} thành công",
                currentUserClass.getCurrentUser().getId(), productId);

        return ApiResponse.success("Cap nhat san pham thanh cong", adminProductMapper.toSummary(savedProduct));
    }

    // Kiem tra update dang partial: truong nao null thi xem nhu khong cap nhat
    // truong do.
    private void validateUpdateProductRequest(AdminUpdateProductRequest request) {
        if (request == null) {
            return;
        }

        if ((request.getName() != null && normalize(request.getName()) == null)
                || (request.getPrice() != null && request.getPrice().signum() < 0)
                || hasFractionPart(request.getPrice())
                || (request.getStock() != null && request.getStock() < 0)) {
            throw new ApiError(ErrorCode.BAD_REQUEST);
        }

        if (request.getCategoryId() != null && !categoryRepository.existsById(request.getCategoryId())) {
            throw new ApiError(ErrorCode.CATEGORY_NOT_FOUND);
        }
    }

    // Bat buoc client gui version hien tai; version cu hoac thieu version se khong
    // duoc cap nhat.
    private void validateProductVersion(Product product, AdminUpdateProductRequest request) {
        if (request == null || request.getVersion() == null) {
            throw new ApiError(ErrorCode.BAD_REQUEST, "Version san pham khong duoc de trong");
        }

        // Version cu thi khong hop le.
        if (!request.getVersion().equals(product.getVersion())) {
            throw new ApiError(ErrorCode.PRODUCT_VERSION_CONFLICT);
        }
    }

    // Tim anh phu co URL trung chinh xac voi deleteImageUrls, lay publicId de xoa
    // Cloudinary sau commit.
    private void removeProductImagesByUrl(Product product, List<String> deleteImageUrls,
            List<String> publicIdsToDeleteAfterCommit) {
        // Khong co anh can xoa hoac san pham khong co anh.
        if (deleteImageUrls == null || deleteImageUrls.isEmpty()
                || product.getImages() == null || product.getImages().isEmpty()) {
            return;
        }

        // Loai bo khoang trang cua cac URL can xoa.
        Set<String> normalizedUrls = new HashSet<>(deleteImageUrls.stream()
                .map(this::normalize)
                .filter(url -> url != null)
                .toList());

        // Kiem tra danh sach co rong khong.
        if (normalizedUrls.isEmpty()) {
            return;
        }

        // Neu product khong co URL anh can xoa duoc gui len thi bo qua.
        product.getImages().removeIf(image -> {
            if (!normalizedUrls.contains(image.getUrl())) {
                return false;
            }

            // Luu publicId truoc khi xoa anh trong DB de con xoa file tren Cloudinary sau
            // commit.

            addPublicId(publicIdsToDeleteAfterCommit, image.getPublicIdUrl());
            return true;
        });
    }

    // Them cac anh vua upload vao product; cascade ALL se luu ProductImageEntity
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

    // Kiem tra request de phat hien loi.
    private void validateCreateProductRequest(AdminCreateProductRequest request, MultipartFile thumbnail) {
        if (request == null
                || normalize(request.getName()) == null
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

    // Anh phu khong bat buoc; neu co thi upload vao folder rieng cua product.
    private List<CloudinaryImage> uploadProductImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }

        return cloudinaryService.uploadImages(images, "products");
    }

    // Chuyen ket qua upload Cloudinary thanh entity anh phu va gan nguoc product de
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

    // Chi don dep anh vua upload khi transaction DB rollback.
    private void registerUploadedImageCleanupOnRollback(List<CloudinaryImage> images) {
        List<String> publicIds = images.stream()
                .map(CloudinaryImage::getPublicId)
                .filter(publicId -> publicId != null && !publicId.isBlank())
                .distinct()
                .toList();

        if (publicIds.isEmpty()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    logger.error("có lỗi ghi database ảnh trên Cloudinary rollbank");
                    cloudinaryService.deleteImage(publicIds);
                }
            }
        });
    }

    // Dung khi loi xay ra ngay trong qua trinh upload, truoc khi den buoc save DB.
    private void cleanupUploadedImages(List<CloudinaryImage> images) {
        List<String> publicIds = images.stream()
                .map(CloudinaryImage::getPublicId)
                .filter(publicId -> publicId != null && !publicId.isBlank())
                .distinct()
                .toList();

        if (!publicIds.isEmpty()) {
            cloudinaryService.deleteImage(publicIds);
        }
    }

    // Xoa san pham cho admin, dong thoi don cart item va len lich xoa anh
    // Cloudinary.
    @Transactional
    public ApiResponse<Void> deleteProduct(Long productId) {
        Product product = findProduct(productId);

        deleteProductCore(product);
        logger.info("admin với id:{} xóa sản phẩm id:{} thành công", currentUserClass.getCurrentUser().getId(),
                productId);

        return ApiResponse.success("Xoa san pham thanh cong voi id: " + productId, null);
    }

    // Tim san pham kem danh sach anh de phuc vu nghiep vu xoa va don anh
    // Cloudinary.
    private Product findProduct(Long productId) {
        return productRepository.findDetailById(productId)
                .orElseThrow(() -> new ApiError(ErrorCode.PRODUCT_NOT_FOUND));
    }

    // Lay toan bo san pham thuoc mot danh muc, kem anh phu de xoa danh muc co the
    // tai su dung logic xoa san pham.
    public List<Product> findProductsByCategoryId(Long categoryId) {
        return productRepository.findByCategory_Id(categoryId);
    }

    // Phan nghiep vu xoa san pham dung chung cho xoa san pham rieng le va xoa danh
    // muc.
    // Ham nay don cart item, xoa product trong DB va chi xoa anh Cloudinary sau khi
    // transaction commit.
    public void deleteProductCore(Product product) {
        List<String> publicIds = collectProductImagePublicIds(product);

        cartLineItemRepository.deleteByProduct_Id(product.getId());
        productRepository.delete(product);
        registerProductImageCleanup(publicIds);
        catalogCacheService.registerProductCacheDeleteAfterCommit(product.getId());
        cartCacheService.registerCartCacheDeleteAfterCommit();
    }

    // Gom publicId cua thumbnail va cac anh phu de xoa tren Cloudinary sau khi xoa
    // DB thanh cong.
    private List<String> collectProductImagePublicIds(Product product) {
        List<String> publicIds = new ArrayList<>();

        addPublicId(publicIds, product.getPublicIdUrl());

        if (product.getImages() != null) {
            product.getImages().stream()
                    .map(image -> image.getPublicIdUrl())
                    .forEach(publicId -> addPublicId(publicIds, publicId));
        }

        return publicIds.stream().distinct().toList();
    }

    // Them public_id vao danh sach dung cho viec xoa anh tren Cloudinary.
    private void addPublicId(List<String> publicIds, String publicId) {
        if (publicId != null && !publicId.isBlank()) {
            publicIds.add(publicId);
        }
    }

    // Chi xoa anh tren Cloudinary sau khi transaction DB commit, tranh mat anh neu
    // DB rollback.
    private void registerProductImageCleanup(List<String> publicIds) {
        if (publicIds.isEmpty()) {
            return;
        }

        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            logger.info("database commit thành công xóa ảnh trên Cloudinary");
            cloudinaryService.deleteImage(publicIds);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                logger.info("database commit thành công xóa ảnh trên Cloudinary");
                cloudinaryService.deleteImage(publicIds);
            }
        });
    }
}
