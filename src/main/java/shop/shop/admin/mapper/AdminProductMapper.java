package shop.shop.admin.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import shop.shop.admin.dto.response.AdminProductImageResponse;
import shop.shop.admin.dto.response.AdminProductSummaryResponse;
import shop.shop.product.entity.Product;
import shop.shop.productImage.entity.ProductImageEntity;

@Mapper(componentModel = "spring")
public interface AdminProductMapper {

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    AdminProductSummaryResponse toSummary(Product product);

    AdminProductImageResponse toImageResponse(ProductImageEntity image);
}
