package shop.shop.common.cache;

public final class CacheKeys {
    // key: product detail (catalog:product:detail:{id})
    public static String productDetail(Long id) { return "catalog:product:detail:" + id; }

    // key: public product list (paged/filter/sort)
    public static String productList(String categiryID, String search, int page, int size, String sort) {
        return String.format("catalog:product:list:cat:%s:search:%s:p:%d:s:%d:sort:%s",
                categiryID==null?"all": categiryID, search==null?"":search, page, size, sort==null?"":sort);
    }
    // key: public top-selling list
    public static String productTopSelling() { return "catalog:product:topSelling"; }
    // key: shared categories list for user/admin reads (catalog:categories:all)
    public static String categoriesAll() { return "catalog:categories:all"; }

    // key: cart state theo user (cart:user:{userId})
    public static String cartByUser(Long userId) { return "cart:user:" + userId; }

    // key: admin product list/search (admin:product:list:...)
    public static String adminProductList(Long categoryId, String search, String status, int page, int size, String sort) {
        return String.format("admin:product:list:cat:%s:search:%s:status:%s:p:%d:s:%d:sort:%s",
                categoryId == null ? "all" : categoryId,
                search == null ? "" : search,
                status == null ? "" : status,
                page,
                size,
                sort == null ? "" : sort);
    }

}
