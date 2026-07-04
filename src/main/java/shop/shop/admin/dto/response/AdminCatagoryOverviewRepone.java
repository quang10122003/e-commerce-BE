package shop.shop.admin.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminCatagoryOverviewRepone {
    Long totalCategory;
    String topCategory;
    Long emptyCategories;
    List<AdminListNewCategory> listNewCategory;
}
