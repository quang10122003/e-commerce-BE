// class  chỉ lo "ai được làm gì trong room" - kiểm tra quyền truy cập, gán
// admin phụ trách, xác định phía đối diện của viewer. < chính sách phân quyền + vai trò trong room> 
package shop.shop.chat.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import shop.shop.chat.entity.ChatRoom;
import shop.shop.chat.repo.ChatRoomRepository;
import shop.shop.common.error.ApiError;
import shop.shop.common.error.ErrorCode;
import shop.shop.user.entity.User;


@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatAccessGuard {
    ChatRoomRepository chatRoomRepository;

    public boolean isAdmin(User user) {
        return user != null && "ADMIN".equalsIgnoreCase(user.getRoleName());
    }

    // check xem user có quền xem room hay k 
    public void validateRoomAccess(ChatRoom room, User user) {
        if (isAdmin(user)) {
            if (room.getAdmin() == null
                    || (room.getAdmin() != null && room.getAdmin().getId().equals(user.getId()))) {
                return;
            }
            throw new ApiError(ErrorCode.ACCESS_DENIED);
        }

        if (room.getUser() == null || !room.getUser().getId().equals(user.getId())) {
            throw new ApiError(ErrorCode.ACCESS_DENIED);
        }
    }

    // Gán room cho admin khi admin phản hồi lần đầu tiên. Trả về true nếu vừa gán.
    public boolean assignRoomToAdminIfNeeded(ChatRoom room, User sender) {
        if (isAdmin(sender)
                && room.getAdmin() == null
                && room.getUser() != null
                && !room.getUser().getId().equals(sender.getId())) {
            room.setAdmin(sender);
            chatRoomRepository.save(room);
            return true;
        }
        return false;
    }

    //Lấy ID người gửi phía đối diện để tính số tin nhắn chưa đọc.
    public Long resolveUnreadSenderId(ChatRoom room, User viewer) {
        if (isAdmin(viewer)) {
            return room.getUser() == null ? null : room.getUser().getId();
        }
        return room.getAdmin() == null ? null : room.getAdmin().getId();
    }
}