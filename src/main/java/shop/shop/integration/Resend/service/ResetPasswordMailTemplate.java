package shop.shop.integration.Resend.service;

import org.springframework.stereotype.Component;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import shop.shop.integration.Resend.DTO.respone.EmailContent;
import shop.shop.integration.Resend.DTO.resquest.ResetPasswordMailDTO;
import shop.shop.integration.Resend.service.interfaces.IEmailTemplate;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
public class ResetPasswordMailTemplate extends IEmailTemplate<ResetPasswordMailDTO> {
    String resetPasswordMailSend = "support@daoxuanquang.dev";

    @Override
    protected EmailContent build(ResetPasswordMailDTO data) {
        String resetLink = domain + "/reset-password?token="
                + data.getToken();

        EmailContent contenet = new EmailContent(
                resetPasswordMailSend,
                data.getEmail(),
                "Reset password",
                wrapHtml(
                        """
                                <tr>
                                    <td style="background-color:#4F46E5; padding:32px; text-align:center;">
                                        <h1 style="color:#ffffff; margin:0; font-size:22px; font-weight:600;">
                                            Đặt lại mật khẩu
                                        </h1>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding:32px;">
                                        <p style="color:#333333; font-size:15px; line-height:1.6; margin:0 0 16px;">
                                            Xin chào,
                                        </p>
                                        <p style="color:#333333; font-size:15px; line-height:1.6; margin:0 0 24px;">
                                            Chúng tôi vừa nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.
                                            Vui lòng nhấn vào nút bên dưới để tạo mật khẩu mới.
                                        </p>
                                        <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 auto;">
                                            <tr>
                                                <td align="center" style="border-radius:6px; background-color:#4F46E5;">
                                                    <a href="%s"
                                                       style="display:inline-block; padding:14px 32px; font-size:15px; font-weight:600; color:#ffffff; text-decoration:none; border-radius:6px;">
                                                        Đặt lại mật khẩu
                                                    </a>
                                                </td>
                                            </tr>
                                        </table>
                                        <p style="color:#888888; font-size:13px; line-height:1.6; margin:28px 0 0;">
                                            Liên kết này sẽ hết hạn sau 15 phút. Nếu bạn không yêu cầu đặt lại mật khẩu,
                                            vui lòng bỏ qua email này.
                                        </p>
                                    </td>
                                </tr>
                                """
                                .formatted(resetLink))

        );
        return contenet;

    }

}
