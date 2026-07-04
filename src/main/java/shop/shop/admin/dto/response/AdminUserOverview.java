package shop.shop.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserOverview {
    long totalUser;
    long newUserIn7day;
}