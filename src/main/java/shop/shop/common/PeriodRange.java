// class để lưu khoảng thời gian 
package shop.shop.common;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeriodRange {
    private LocalDateTime start;
    private LocalDateTime end;

}
