package shop.shop.security;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import shop.shop.chat.entity.ChatRoom;
import shop.shop.chat.repo.ChatRoomRepository;
import shop.shop.common.error.ApiError;
import shop.shop.common.error.ErrorCode;
import shop.shop.user.entity.User;
import shop.shop.user.repos.UserRepo;

@Component
@RequiredArgsConstructor
public class WebSocketJwtInterceptor implements ChannelInterceptor {
    // Interceptor nay la lop bao ve chinh cua kenh STOMP inbound.
    // HTTP security filter chi xu ly request bat tay ban dau; sau khi WebSocket da
    // mo, cac frame CONNECT/SUBSCRIBE/SEND se di qua ChannelInterceptor nay.
    private final AuthUtil authUtil;
    private final UserDetailsServiceCustom userDetailsService;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepo userRepo;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // Phai lay accessor GOC dang nam trong message.
        // Neu dung StompHeaderAccessor.wrap(message), Spring tao mot ban sao; khi
        // goi setUser() tren ban sao do, Principal co the khong duoc luu vao session.
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // Mot so message noi bo cua Spring khong phai STOMP command tu client.
        // Cho di qua som de interceptor chi can thiep vao frame co y nghia bao mat.
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        // CONNECT la thoi diem client chung minh danh tinh bang ws-ticket.
        // Sau khi xac thuc thanh cong, Principal duoc gan vao WebSocket session va
        // duoc tai su dung cho cac frame SUBSCRIBE/SEND tiep theo.
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String header = accessor.getFirstNativeHeader("Authorization");

            // STOMP native header tach biet voi HTTP header ban dau.
            // Client phai gui dung "Authorization: Bearer <ws-ticket>" trong CONNECT.
            if (header == null || !header.startsWith("Bearer ")) {
                throw new ApiError(ErrorCode.AUTHORIZATION_HEADER_INVALID);
            }

            try {
                String token = header.substring(7);
                String email = authUtil.extractEmail(token);
                UserDetails user = userDetailsService.loadUserByUsername(email);

                // WebSocket chi chap nhan ticket ngan han loai "ws-ticket".
                // Khong dung access token dai han o day de giam rui ro khi token bi lo
                // trong cong cu log/debug cua client thoi gian thuc.
                if (!authUtil.isWsTicketValid(token, user)) {
                    throw new ApiError(ErrorCode.UNAUTHORIZED, "WebSocket ticket khong hop le hoac da het han");
                }

                // Chuyen UserDetails thanh Authentication chuan cua Spring Security.
                // Controller co the nhan Principal, con interceptor co the doc lai user
                // qua accessor.getUser() o cac frame sau.
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, null,
                        user.getAuthorities());

                // Giu accessor o trang thai mutable de Spring kip doc Principal da set.
                // Neu accessor bi dong qua som, user co the khong duoc dua sang
                // session va SUBSCRIBE/SEND se bi xem la chua xac thuc.
                accessor.setLeaveMutable(true);

                // Gan user da xac thuc vao WebSocket session hien tai.
                accessor.setUser(authentication);
            } catch (ApiError e) {
                // ApiError da mang ErrorCode chuan cua he thong, khong boc them de
                // tranh mat ma loi nghiep vu.
                throw e;
            } catch (Exception e) {
                // Cac loi con lai gom token malformed, user khong ton tai trong DB,
                // hoac loi khi load UserDetails. Với WebSocket, loi xac thuc phai chan
                // ngay tai CONNECT thay vi de session ton tai o trang thai mo ho.
                throw new ApiError(ErrorCode.AUTHENTICATION_FAILED, e.getMessage());
            }
        }

        // SEND va SUBSCRIBE la hai frame co the tac dong toi du lieu chat:
        // - SUBSCRIBE quyet dinh client duoc nghe topic nao.
        // - SEND quyet dinh client duoc ghi tin nhan vao room nao.
        // Vi vay ca hai deu phai co Principal va di qua buoc kiem tra quyen room.
        if (StompCommand.SEND.equals(accessor.getCommand())
                || StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            if (accessor.getUser() == null) {
                throw new ApiError(ErrorCode.UNAUTHORIZED);
            }

            validateChatRoomAccess(accessor);
        }

        return message;
    }

    private void validateChatRoomAccess(StompHeaderAccessor accessor) {
        // Khong phai moi destination STOMP deu la chat room.
        // Neu destination khong chua /chat/rooms/{id}, interceptor khong ap quy tac room
        // o day de cac luong WebSocket khac trong tuong lai van co the hoat dong.
        Long roomId = extractChatRoomId(accessor.getDestination());
        if (roomId == null) {
            return;
        }

        // Principal trong WebSocket chi giu dinh danh dang nhap.
        // Lay lai User tu DB de kiem tra role/id hien tai, tranh tin vao du lieu cu
        // trong token neu quyen user vua thay doi.
        User user = userRepo.findByEmailIgnoreCase(accessor.getUser().getName())
                .orElseThrow(() -> new ApiError(ErrorCode.USER_NOT_FOUND));

        // Room phai ton tai truoc khi cho subscribe/send.
        // Khong de client tu tao hoac doan roomId qua WebSocket.
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ApiError(ErrorCode.BAD_REQUEST, "Khong tim thay room chat"));

        // Admin duoc phep vao cac room chat de ho tro khach hang.
        // Quy tac admin nao duoc nhan room se do ChatService xu ly khi SEND.
        if (isAdmin(user)) {
            return;
        }

        // User thong thuong chi duoc vao dung room do chinh ho tao.
        // Chan ngay o SUBSCRIBE de tranh nghe len tin nhan broadcast len topic.
        if (room.getUser() == null || !room.getUser().getId().equals(user.getId())) {
            throw new ApiError(ErrorCode.ACCESS_DENIED);
        }
    }

    private Long extractChatRoomId(String destination) {
        // Destination co the null o mot so frame hoac message noi bo.
        // Tra null thay vi nem loi de interceptor khong can thiep nham.
        if (destination == null) {
            return null;
        }

        // Ho tro ca hai dang:
        // - SUBSCRIBE: /topic/chat/rooms/{roomId}
        // - SEND: /api/chat/rooms/{roomId}/send
        // Vi vay chi tim marker chung "/chat/rooms/" thay vi hard-code prefix.
        String marker = "/chat/rooms/";
        int markerIndex = destination.indexOf(marker);
        if (markerIndex < 0) {
            return null;
        }

        // RoomId ket thuc o dau "/" ke tiep neu destination con hau to nhu "/send";
        // neu khong co hau to thi lay den het chuoi.
        int roomIdStart = markerIndex + marker.length();
        int roomIdEnd = destination.indexOf('/', roomIdStart);
        String roomId = roomIdEnd < 0
                ? destination.substring(roomIdStart)
                : destination.substring(roomIdStart, roomIdEnd);

        try {
            return Long.valueOf(roomId);
        } catch (NumberFormatException e) {
            // Co marker chat room nhung roomId khong phai so la client goi sai contract.
            // Tra BAD_REQUEST de loi hien ro thay vi lang le bo qua buoc kiem tra quyen.
            throw new ApiError(ErrorCode.BAD_REQUEST, "Room chat khong hop le");
        }
    }

    private boolean isAdmin(User user) {
        // So sanh khong phan biet hoa thuong de tranh lech du lieu role trong DB lam
        // admin bi tu choi quyen truy cap room.
        return user != null && "ADMIN".equalsIgnoreCase(user.getRoleName());
    }
}
