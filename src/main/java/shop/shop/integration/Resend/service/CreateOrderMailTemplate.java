package shop.shop.integration.Resend.service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import shop.shop.common.PaymentMethod;
import shop.shop.integration.Resend.DTO.respone.EmailContent;
import shop.shop.integration.Resend.DTO.resquest.CreateOrderMailDTO;
import shop.shop.integration.Resend.service.interfaces.IEmailTemplate;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
public class CreateOrderMailTemplate extends IEmailTemplate<CreateOrderMailDTO> {
    private String emailOrderSend = "order@daoxuanquang.dev";

    @Override
    public EmailContent build(CreateOrderMailDTO data) {

        // Render danh sách sản phẩm thành các dòng <tr>
        String itemsHtml = data.getItems().stream()
                .map(item -> """
                        <tr>
                            <td style="padding:12px 0; border-bottom:1px solid #eeeeee;">
                                <table role="presentation" cellpadding="0" cellspacing="0" width="100%%">
                                    <tr>
                                        <td width="64" style="vertical-align:top;">
                                            <img src="%s" alt="%s" width="56" height="56"
                                                 style="border-radius:6px; object-fit:cover; display:block;" />
                                        </td>
                                        <td style="vertical-align:top; padding-left:12px;">
                                            <p style="margin:0 0 4px; font-size:14px; color:#333333; font-weight:600;">%s</p>
                                            <p style="margin:0; font-size:12px; color:#888888;">%s · SL: %d</p>
                                        </td>
                                        <td width="100" style="vertical-align:top; text-align:right;">
                                            <p style="margin:0; font-size:14px; color:#333333; font-weight:600;">%s</p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                        """
                        .formatted(
                                item.getThumbnail() != null ? item.getThumbnail() : "",
                                item.getProductName(),
                                item.getProductName(),
                                item.getCategoryName() != null ? item.getCategoryName() : "",
                                item.getQuantity(),
                                formatCurrency(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))))
                .collect(Collectors.joining());

        EmailContent content = new EmailContent(
                emailOrderSend,
                data.getEmail(),
                "Xác nhận đơn hàng #" + data.getOrderCode(),
                wrapHtml(
                        """
                                <tr>
                                    <td style="background-color:#4F46E5; padding:32px; text-align:center;">
                                        <h1 style="color:#ffffff; margin:0; font-size:22px; font-weight:600;">
                                            Đặt hàng thành công
                                        </h1>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding:32px;">
                                        <p style="color:#333333; font-size:15px; line-height:1.6; margin:0 0 16px;">
                                            Xin chào %s,
                                        </p>
                                        <p style="color:#333333; font-size:15px; line-height:1.6; margin:0 0 24px;">
                                            Cảm ơn bạn đã đặt hàng. Đơn hàng <strong>#%s</strong> của bạn đã được ghi nhận
                                            và đang được xử lý.
                                        </p>

                                        <table role="presentation" cellpadding="0" cellspacing="0" width="100%%"
                                               style="background-color:#f9fafb; border-radius:8px; padding:16px; margin-bottom:24px;">
                                            <tr>
                                                <td style="font-size:14px; color:#555555; padding:4px 0;">Mã đơn hàng</td>
                                                <td style="font-size:14px; color:#333333; font-weight:600; text-align:right;">%s</td>
                                            </tr>
                                            <tr>
                                                <td style="font-size:14px; color:#555555; padding:4px 0;">Phương thức thanh toán</td>
                                                <td style="font-size:14px; color:#333333; font-weight:600; text-align:right;">%s</td>
                                            </tr>
                                            <tr>
                                                <td style="font-size:14px; color:#555555; padding:4px 0;">Người nhận</td>
                                                <td style="font-size:14px; color:#333333; font-weight:600; text-align:right;">%s</td>
                                            </tr>
                                            <tr>
                                                <td style="font-size:14px; color:#555555; padding:4px 0;">Số điện thoại</td>
                                                <td style="font-size:14px; color:#333333; font-weight:600; text-align:right;">%s</td>
                                            </tr>
                                            <tr>
                                                <td style="font-size:14px; color:#555555; padding:4px 0; vertical-align:top;">Địa chỉ giao hàng</td>
                                                <td style="font-size:14px; color:#333333; font-weight:600; text-align:right;">%s</td>
                                            </tr>
                                        </table>

                                        <table role="presentation" cellpadding="0" cellspacing="0" width="100%%" style="margin-bottom:16px;">
                                            %s
                                        </table>

                                        <table role="presentation" cellpadding="0" cellspacing="0" width="100%%">
                                            <tr>
                                                <td style="padding-top:16px; text-align:right; font-size:16px; color:#333333; font-weight:700;">
                                                    Tổng cộng: %s
                                                </td>
                                            </tr>
                                        </table>

                                        <p style="color:#888888; font-size:13px; line-height:1.6; margin:28px 0 0;">
                                            Chúng tôi sẽ gửi email cập nhật khi đơn hàng được giao cho đơn vị vận chuyển.
                                            Nếu bạn có bất kỳ thắc mắc nào, vui lòng liên hệ với chúng tôi.
                                        </p>
                                    </td>
                                </tr>
                                """
                                .formatted(
                                        data.getShippingName(),
                                        data.getOrderCode(),
                                        data.getOrderCode(),
                                        formatPaymentMethod(data.getPaymentMethod()),
                                        data.getShippingName(),
                                        data.getShippingPhone(),
                                        data.getShippingAddress(),
                                        itemsHtml,
                                        formatCurrency(data.getTotalAmount()))));

        return content;
    }

    private String formatCurrency(BigDecimal amount) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        return formatter.format(amount) + " đ";
    }

    private String formatPaymentMethod(PaymentMethod method) {
        if (method == null)
            return "";
        return switch (method) {
            case COD -> "Thanh toán khi nhận hàng (COD)";
            case SEPAY -> "thành toán qua chuyển khoản ngân hàng";
            default -> method.name();
        };
    }

}
