package shop.shop.admin.dto.response;

import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data 
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminPaymentsRepone {
    Long total;
    Long pending;
    Long paid;
    Long failed;
    Long paidLate;

    List<AdminPayementItemRepone> item;

}
