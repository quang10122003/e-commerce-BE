package shop.shop.integration.Resend.service.interfaces;

import org.springframework.beans.factory.annotation.Value;

import shop.shop.integration.Resend.DTO.respone.EmailContent;
public abstract class IEmailTemplate<T> {
    @Value("${app.frontend-url}")
    protected String domain;
    
    protected String wrapHtml(String content) {
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Thông báo</title>
                </head>
                <body style="margin:0; padding:0; background-color:#f4f5f7; font-family:'Helvetica Neue', Arial, sans-serif;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f5f7; padding: 40px 0;">
                        <tr>
                            <td align="center">
                                <table role="presentation" width="480" cellpadding="0" cellspacing="0" style="background-color:#ffffff; border-radius:8px; overflow:hidden; box-shadow:0 2px 8px rgba(0,0,0,0.06);">
                                    %s
                                    <tr>
                                        <td style="background-color:#f9fafb; padding:20px 32px; text-align:center; border-top:1px solid #eeeeee;">
                                            <p style="color:#aaaaaa; font-size:12px; margin:0;">
                                                © 2026 shop mini. Mọi quyền được bảo lưu.
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """
                .formatted(content);
    }

    protected abstract EmailContent build(T data);
}
