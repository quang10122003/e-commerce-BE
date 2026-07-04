package shop.shop.chat.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import shop.shop.chat.Dto.repone.ChatMessageResponse;
import shop.shop.chat.Dto.repone.ChatRoomResponse;
import shop.shop.chat.entity.ChatRoom;
import shop.shop.chat.entity.Message;

@Mapper(componentModel = "spring")
public interface ChatMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userName", source = "user.fullName")
    @Mapping(target = "adminId", source = "admin.id")
    @Mapping(target = "adminName", source = "admin.fullName")
    @Mapping(target = "assignmentStatus", expression = "java(room.getAdmin() == null ? shop.shop.common.ChatRoomAssignmentStatus.UNASSIGNED : shop.shop.common.ChatRoomAssignmentStatus.ASSIGNED)")
    ChatRoomResponse toRoomResponse(ChatRoom room);

    @Mapping(target = "roomId", source = "room.id")
    @Mapping(target = "senderId", source = "sender.id")
    @Mapping(target = "senderName", source = "sender.fullName")
    @Mapping(target = "messageType", expression = "java(message.getMessageType() == null ? null : message.getMessageType().name())")
    ChatMessageResponse toMessageResponse(Message message);
}
