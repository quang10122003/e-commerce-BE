package shop.shop.admin.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminListNewCategory {
    String name;
    LocalDateTime createdAt;
    
}
