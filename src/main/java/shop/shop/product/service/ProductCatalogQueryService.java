// class chỉ lo duyệt catalog<các tính năng hiển thị> công khai cho storefront (list, filter, top selling, chi tiết sản phẩm)
package shop.shop.product.service;

import java.time.Duration;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import shop.shop.common.ProductStatus;
import shop.shop.common.cache.CacheKeys;
import shop.shop.common.dto.response.PagedResponse;
import shop.shop.common.error.ApiError;
import shop.shop.common.error.ErrorCode;
import shop.shop.integration.redis.service.interfaces.ICacheService;
import shop.shop.product.dto.response.ProductSummaryResponse;
import shop.shop.product.dto.response.Productdetail;
import shop.shop.product.mapper.ProductMapper;
import shop.shop.product.repository.ProductRepository;
import tools.jackson.core.type.TypeReference;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
@RequiredArgsConstructor
public class ProductCatalogQueryService {
    ProductRepository productRepository;
    ProductMapper productMapper;
    ICacheService cacheService;

    // Lấy danh sách sản phẩm public dạng phân trang, cache theo bộ lọc và paging.
    public PagedResponse<ProductSummaryResponse> getActiveProductsPaged(Long categoryId, String search,
            Pageable pageable) {
        String cacheKey = CacheKeys.productList(
                categoryId == null ? null : categoryId.toString(),
                search,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort().toString());

        PagedResponse<ProductSummaryResponse> cachedProducts = cacheService.getPayload(cacheKey,
                new TypeReference<PagedResponse<ProductSummaryResponse>>() {});
        if (cachedProducts != null) {
            return cachedProducts;
        }

        Page<ProductSummaryResponse> activeProducts = getActiveProducts(categoryId, search, pageable);
        PagedResponse<ProductSummaryResponse> pagedResponse = PagedResponse.from(activeProducts);

        cacheService.set(cacheKey, pagedResponse, Duration.ofHours(1));

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

        List<ProductSummaryResponse> cachedProducts = cacheService.getPayload(cacheKey,
                new TypeReference<List<ProductSummaryResponse>>() {});
        if (cachedProducts != null) {
            return cachedProducts;
        }
        List<ProductSummaryResponse> products = productRepository
                .findTop6ByStatusOrderByPurchasesDescCreatedAtDesc(ProductStatus.ACTIVE)
                .stream()
                .map(productMapper::toSummary)
                .toList();

        cacheService.set(cacheKey, products, Duration.ofMinutes(15));

        return products;
    }

    public Productdetail getProductById(Long id) {
        String cacheKey = CacheKeys.productDetail(id);
        Productdetail cachedProduct = cacheService.getPayload(cacheKey, Productdetail.class);
        if (cachedProduct != null) {
            return cachedProduct;
        }

        Productdetail productDetail = productRepository.findDetailById(id)
                .map(productMapper::toDetail)
                .orElseThrow(() -> new ApiError(ErrorCode.PRODUCT_NOT_FOUND));

        cacheService.set(cacheKey, productDetail, Duration.ofHours(1));

        return productDetail;
    }

    // chuẩn hóa chuỗi
    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
