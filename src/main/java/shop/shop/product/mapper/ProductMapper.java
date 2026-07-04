package shop.shop.product.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import shop.shop.admin.dto.request.AdminCreateProductRequest;
import shop.shop.admin.dto.request.AdminUpdateProductRequest;
import shop.shop.category.entity.Category;
import shop.shop.common.ProductStatus;
import shop.shop.product.dto.response.Productdetail;
import shop.shop.product.dto.response.ProductSummaryResponse;
import shop.shop.product.entity.Product;
import shop.shop.productImage.entity.ProductImageEntity;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "categoryId", source = "category.id")
    ProductSummaryResponse toSummary(Product product);

    @Mapping(target = "nameCategory", source = "category.name")
    @Mapping(target = "url", source = "images")
    Productdetail toDetail(Product product);

    // Anh xa cac truong co trong request sang Product; cac truong anh duoc gan sau khi upload Cloudinary.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", source = "categoryId", qualifiedByName = "toCategory")
    @Mapping(target = "thumbnail", ignore = true)
    @Mapping(target = "publicIdUrl", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "purchases", constant = "0")
    @Mapping(target = "name", expression = "java(normalize(request.getName()))")
    @Mapping(target = "description", expression = "java(normalize(request.getDescription()))")
    Product toProduct(AdminCreateProductRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", source = "categoryId", qualifiedByName = "toCategory")
    @Mapping(target = "thumbnail", ignore = true)
    @Mapping(target = "publicIdUrl", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "purchases", ignore = true)
    @Mapping(target = "name", expression = "java(request.getName() == null ? product.getName() : normalize(request.getName()))")
    @Mapping(target = "description", expression = "java(request.getDescription() == null ? product.getDescription() : normalize(request.getDescription()))")
    void updateProduct(@MappingTarget Product product, AdminUpdateProductRequest request);

    // Neu admin khong truyen status, san pham mac dinh o trang thai ACTIVE.
    default ProductStatus map(ProductStatus status) {
        return status == null ? ProductStatus.ACTIVE : status;
    }

    // Tao tham chieu Category tu categoryId de mapper gan truc tiep vao Product.category.
    @Named("toCategory")
    default Category toCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }

        Category category = new Category();
        category.setId(categoryId);
        return category;
    }

    // Bo khoang trang thua va doi chuoi rong thanh null truoc khi luu DB.
    default String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    default String toUrl(ProductImageEntity image) {
        return image == null ? null : image.getUrl();
    }

    default List<String> toUrlList(List<ProductImageEntity> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }

        return images.stream()
                .map(this::toUrl)
                .toList();
    }
}
