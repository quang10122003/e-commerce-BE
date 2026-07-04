package shop.shop.cart.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import shop.shop.cart.dto.response.CartItemResponse;
import shop.shop.cart.entity.CartLineItem;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring")
public interface CartMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "thumbnail", source = "product.thumbnail")
    @Mapping(target = "unitPrice", source = "product.price")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "stock", source = "product.stock") // Anh xa stock tu product.
    @Mapping(target = "totalPrice", source = ".", qualifiedByName = "mapTotalPrice")
    CartItemResponse toResponse(CartLineItem cartLineItem);

    List<CartItemResponse> toResponseList(List<CartLineItem> items);

    @Named("mapTotalPrice")
    default BigDecimal mapTotalPrice(CartLineItem item) {
        return item.getProduct().getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));
    }
}
