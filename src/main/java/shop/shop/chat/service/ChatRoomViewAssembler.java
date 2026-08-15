// class  chỉ lo dựng ChatRoomResponse đầy đủ (lastMessage, unreadCount) theo góc nhìn của 1 viewer cụ thể.
package shop.shop.chat.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import shop.shop.chat.Dto.repone.ChatRoomResponse;
import shop.shop.chat.entity.ChatRoom;
import shop.shop.chat.mapper.ChatMapper;
import shop.shop.chat.repo.MessageRepository;
import shop.shop.user.entity.User;


@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatRoomViewAssembler {
    MessageRepository messageRepository;
    ChatMapper chatMapper;
    ChatAccessGuard chatAccessGuard;

    public ChatRoomResponse toRoomResponseForViewer(ChatRoom room, User viewer) {
        ChatRoomResponse chatRoomResponse = chatMapper.toRoomResponse(room);

        // Nếu room có tin nhắn cuối thì đưa thông tin đó vào repone.
        messageRepository.findLatestMessageByRoom(room.getId()).ifPresent(lastMessage -> {
            chatRoomResponse.setLastMessageContent(lastMessage.getContent());

            chatRoomResponse.setLastMessageType(
                    lastMessage.getMessageType() == null ? null : lastMessage.getMessageType().toString());

            chatRoomResponse.setLastMessageAt(lastMessage.getCreatedAt());

            if (lastMessage.getSender() != null) {
                chatRoomResponse.setLastMessageSenderId(lastMessage.getSender().getId());
                chatRoomResponse.setLastMessageSenderName(lastMessage.getSender().getFullName());
            }
        });

        Long unreadSenderId = chatAccessGuard.resolveUnreadSenderId(room, viewer);

        if (unreadSenderId != null) {
            chatRoomResponse.setUnreadCount(
                    messageRepository.countUnreadTextMessagesFromSender(room.getId(), unreadSenderId));
        }
        return chatRoomResponse;
    }
}