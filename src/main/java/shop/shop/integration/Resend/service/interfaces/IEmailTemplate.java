package shop.shop.integration.Resend.service.interfaces;

import org.springframework.beans.factory.annotation.Value;

import shop.shop.integration.Resend.DTO.respone.EmailContent;
public abstract class IEmailTemplate<T> {
    @Value("${app.form-email}")
    protected String from_email;

    @Value("${app.frontend-url}")
    protected String domain;
    
    protected String wrapHtml(String content) {
        return """
                <html>
                    <body>
                        %s
                    </body>
                </html>
                """.formatted(content);
    }

    protected abstract EmailContent build(T data);
}
