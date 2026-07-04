package shop.shop.common.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record PagedResponse<T>(
        List<T> items,
        int page,
        int pageSize,
        long totalItems,
        int totalPages
) {
    public static <T> PagedResponse<T> from(Page<T> pageData) {
        return new PagedResponse<>(
                pageData.getContent(),
                pageData.getNumber() + 1,
                pageData.getSize(),
                pageData.getTotalElements(),
                pageData.getTotalPages()
        );
    }
}
