package shop.shop.category.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.shop.category.dto.response.CategorySummaryResponse;
import shop.shop.category.service.CategoryService;
import shop.shop.common.dto.response.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategorySummaryResponse>>> getAllCategories() {
        return ResponseEntity.status(200).body(categoryService.getAllCategories());
    }
}
