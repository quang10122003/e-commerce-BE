// class đảm nhận việc cấp vé WebSocket ngắn hạn
package shop.shop.auth.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import shop.shop.auth.dto.response.WsTicketResponse;
import shop.shop.common.dto.response.ApiResponse;
import shop.shop.common.until.CurrentUserProvider;
import shop.shop.security.AuthUtil;
import shop.shop.user.entity.User;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WsTicketService {

    AuthUtil authUtil;
    CurrentUserProvider currentUserProvider;

    @Transactional(readOnly = true)
    public ApiResponse<WsTicketResponse> createWsTicket() {
        User user = currentUserProvider.getCurrentUser();

        WsTicketResponse response = WsTicketResponse.builder()
                .ticket(authUtil.generateWsTicket(user))
                .tokenType("Bearer")
                .expiresInSeconds(authUtil.getWsTicketExpirationSeconds())
                .build();

        return ApiResponse.success("Cap WebSocket ticket thanh cong", response);
    }
}