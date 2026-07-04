package shop.shop.admin.Projection;
public interface PaymentStatsProjection {

    Long getTotal();

    Long getPending();

    Long getPaid();

    Long getFailed();

    Long getPaidLate();
}