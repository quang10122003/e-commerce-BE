package shop.shop.product.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import shop.shop.productImage.entity.ProductImageEntity;

@Mapper(componentModel = "spring")
public interface ProductImageMapper {

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
