package shop.shop.admin.Projection;

import java.time.LocalDate;

public interface DailyRevenueProjection {

    LocalDate getDate();

    Long getTotal();

}
