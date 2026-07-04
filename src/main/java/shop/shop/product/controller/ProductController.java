package shop.shop.product.controller;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.shop.common.dto.response.ApiResponse;
import shop.shop.common.dto.response.PagedResponse;
import shop.shop.product.dto.response.ProductSummaryResponse;
import shop.shop.product.dto.response.Productdetail;
import shop.shop.product.service.ProductService;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<PagedResponse<ProductSummaryResponse>>> getActiveProducts(
            @PageableDefault(size = 20, sort = "price", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "search", required = false) String search) {
        Page<ProductSummaryResponse> activeProducts = productService.getActiveProducts(categoryId, search, pageable);

        PagedResponse<ProductSummaryResponse> pagedResponse = PagedResponse.from(activeProducts);
        return ResponseEntity.status(200).body(ApiResponse.success("Active products fetched", pagedResponse));
    }

    @GetMapping("/topSelling")
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> getActiveProductsTopSelling() {
        return ResponseEntity.status(200)   
                .body(ApiResponse.success("Lấy thành công sản phẩm top", productService.getTopSelling()));

    }

    @GetMapping("{id}")
    public ResponseEntity<ApiResponse<Productdetail>> getProductByid(@PathVariable(name = "id" ) Long id )
    {
        return ResponseEntity.status(200).body(
            ApiResponse.success("Lấy thành công sản phẩm có id: " + id, productService.getProductById(id))
        );
    }
}
