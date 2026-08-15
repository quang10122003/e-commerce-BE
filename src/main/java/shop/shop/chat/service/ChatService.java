// class Chỉ điều phối nghiệp vụ chat (tạo room, gửi tin, đánh dấu đã đọc).
package shop.shop.chat.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import shop.shop.chat.Dto.repone.ChatMessageResponse;
import shop.shop.chat.Dto.repone.ChatReadReceiptResponse;
import shop.shop.chat.Dto.repone.ChatRoomResponse;
import shop.shop.chat.Dto.repone.MarkRoomAsReadResult;
import shop.shop.chat.Dto.request.SendMessageRequest;
import shop.shop.chat.entity.ChatRoom;
import shop.shop.chat.entity.Message;
import shop.shop.chat.mapper.ChatMapper;
import shop.shop.chat.repo.ChatRoomRepository;
import shop.shop.chat.repo.MessageRepository;
import shop.shop.common.MessageType;
import shop.shop.common.dto.response.ApiResponse;
import shop.shop.common.error.ApiError;
import shop.shop.common.error.ErrorCode;
import shop.shop.common.until.CurrentUserProvider;
import shop.shop.common.until.ValidationUtils;
import shop.shop.integration.wedsocket.service.interfaces.IWebSocketSender;
import shop.shop.product.entity.Product;
import shop.shop.product.repository.ProductRepository;
import shop.shop.user.entity.User;
import shop.shop.user.repos.UserRepo;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatService {
    ChatRoomRepository chatRoomRepository;
    MessageRepository messageRepository;
    ProductRepository productRepository;
    UserRepo userRepo;
    ChatMapper chatMapper;
    IWebSocketSender iWebSocketSender;
    CurrentUserProvider currentUserProvider;
    ValidationUtils validationUtils;
    ChatAccessGuard chatAccessGuard;
    ChatRoomViewAssembler chatRoomViewAssembler;

    // Tạo phòng chat mới cho user hiện tại theo sản phẩm.
    @Transactional
    public ApiResponse<ChatRoomResponse> createRoom(Long productId) {
        User user = currentUserProvider.getCurrentUser();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ApiError(ErrorCode.PRODUCT_NOT_FOUND));

        ChatRoom newRoom = new ChatRoom();
        newRoom.setProduct(product);
        newRoom.setUser(user);

        return ApiResponse.success("Lay room chat thanh cong",
                chatMapper.toRoomResponse(chatRoomRepository.save(newRoom)));
    }

    // Lấy phòng chat của user hiện tại theo sản phẩm.
    @Transactional(readOnly = true)
    public ApiResponse<ChatRoomResponse> getCurrentUserRoomByProduct(Long productId) {
        User user = currentUserProvider.getCurrentUser();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ApiError(ErrorCode.PRODUCT_NOT_FOUND));

        ChatRoom room = chatRoomRepository
                .findRoom(product.getId(), user.getId())
                .orElseThrow(() -> new ApiError(ErrorCode.CHAT_ROOM_NOT_FOUND));

        return ApiResponse.success("Lay room chat thanh cong",
                chatRoomViewAssembler.toRoomResponseForViewer(room, user));
    }

    // Lấy danh sách phòng chat của user hiện tại.
    @Transactional(readOnly = true)
    public ApiResponse<List<ChatRoomResponse>> getCurrentUserRooms(String search) {
        User user = currentUserProvider.getCurrentUser();
        String normalizedSearch = validationUtils.normalize(search);
        List<ChatRoom> rooms = normalizedSearch == null
                ? chatRoomRepository.findRoomsByUser(user.getId())
                : chatRoomRepository.findRoomsByUserAndProductName(user.getId(), normalizedSearch);

        return ApiResponse.success(
                "Lay danh sach room chat cua user thanh cong",
                rooms.stream()
                        .map(room -> chatRoomViewAssembler.toRoomResponseForViewer(room, user))
                        .toList());
    }

    // Lấy danh sách phòng chat cho admin và sắp xếp theo tin nhắn mới nhất.
    @Transactional(readOnly = true)
    public ApiResponse<List<ChatRoomResponse>> getAdminRooms(String search) {
        User admin = currentUserProvider.getCurrentUser();
        String normalizedSearch = validationUtils.normalize(search);

        List<ChatRoom> sourceRooms = normalizedSearch == null
                ? chatRoomRepository.findRoomsForAdmin(admin.getId())
                : chatRoomRepository.findRoomsForAdminAndProductName(admin.getId(), normalizedSearch);

        List<ChatRoomResponse> rooms = sourceRooms
                .stream()
                .map(room -> chatRoomViewAssembler.toRoomResponseForViewer(room, admin))
                .sorted((left, right) -> {
                    LocalDateTime leftTime = left.getLastMessageAt() != null
                            ? left.getLastMessageAt()
                            : left.getCreatedAt();

                    LocalDateTime rightTime = right.getLastMessageAt() != null
                            ? right.getLastMessageAt()
                            : right.getCreatedAt();

                    return rightTime.compareTo(leftTime);
                })
                .toList();

        return ApiResponse.success("Lay danh sach room chat thanh cong", rooms);
    }

    // Lấy toàn bộ tin nhắn trong một phòng chat sau khi kiểm tra quyền truy cập.
    @Transactional(readOnly = true)
    public ApiResponse<List<ChatMessageResponse>> getMessages(Long roomId) {
        User user = currentUserProvider.getCurrentUser();
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ApiError(ErrorCode.BAD_REQUEST, "Khong tim thay room chat"));

        chatAccessGuard.validateRoomAccess(room, user);

        return ApiResponse.success(
                "Lay tin nhan thanh cong",
                messageRepository.findMessagesByRoom(roomId)
                        .stream()
                        .map(chatMapper::toMessageResponse)
                        .toList());
    }

    // gửi tin nhắn WebSocketWebSocket bắn envetn cho topic  room chat + danh sách phòng.
    // Lần admin phản hồi đầu tiên có thể tạo thêm tin SYSTEM trước tin TEXT.
    @Transactional
    public void sendMessage(Long roomId, SendMessageRequest request, Principal principal) {
        if (request == null || request.getContent() == null || request.getContent().isBlank()) {
            throw new ApiError(ErrorCode.BAD_REQUEST, "Noi dung tin nhan khong duoc de trong");
        }
        if (principal == null) {
            throw new ApiError(ErrorCode.UNAUTHORIZED);
        }

        User sender = userRepo.findByEmailIgnoreCase(principal.getName())
                .orElseThrow(() -> new ApiError(ErrorCode.USER_NOT_FOUND));

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ApiError(ErrorCode.BAD_REQUEST, "Khong tim thay room chat"));

        chatAccessGuard.validateRoomAccess(room, sender);
        boolean roomAssigned = chatAccessGuard.assignRoomToAdminIfNeeded(room, sender);

        List<Message> persistedMessages = new ArrayList<>();

        if (roomAssigned) {
            persistedMessages.add(messageRepository.save(createAdminAssignedSystemMessage(room, sender)));
        }

        persistedMessages.add(messageRepository.save(createTextMessage(room, sender, request.getContent().trim())));

        List<ChatMessageResponse> responses = persistedMessages.stream()
                .map(chatMapper::toMessageResponse)
                .toList();

        // Gửi từng tin nhắn theo đúng thứ tự đã lưu: SYSTEM trước, TEXT sau.
        responses.forEach(response -> iWebSocketSender.send("/topic/chat/rooms/" + roomId, response));

        // Gửi tóm tắt mới để danh sách phòng cập nhật lastMessage và unreadCount
        // realtime.
        ChatRoomResponse roomSummary = getRoomSummaryForBroadcast(roomId);
        iWebSocketSender.send("/topic/chat/rooms", roomSummary);
    }

    // Đánh dấu các tin nhắn chưa đọc trong room là đã đọc bởi user hiện tại.
    @Transactional
    public MarkRoomAsReadResult markRoomAsRead(Long roomId) {
        User viewer = currentUserProvider.getCurrentUser();

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ApiError(ErrorCode.BAD_REQUEST, "Khong tim thay room chat"));

        chatAccessGuard.validateRoomAccess(room, viewer);
        Long unreadSenderId = chatAccessGuard.resolveUnreadSenderId(room, viewer);
        List<Long> readMessageIds = List.of();

        if (unreadSenderId != null) {
            readMessageIds = messageRepository.findUnreadTextMessageIdsFromSender(roomId, unreadSenderId);
            messageRepository.markTextMessagesFromSenderAsRead(roomId, unreadSenderId);
        }

        ChatRoomResponse roomResponseForReader = chatRoomViewAssembler.toRoomResponseForViewer(room, viewer);
        ChatReadReceiptResponse readReceipt = readMessageIds.isEmpty()
                ? null
                : ChatReadReceiptResponse.builder()
                        .type("MESSAGES_READ")
                        .roomId(roomId)
                        .readerId(viewer.getId())
                        .readerName(viewer.getFullName())
                        .messageIds(readMessageIds)
                        .readAt(LocalDateTime.now())
                        .build();

        return MarkRoomAsReadResult.builder()
                .room(roomResponseForReader)
                .adminRoomSummary(chatAccessGuard.isAdmin(viewer) ? roomResponseForReader : null)
                .readReceipt(readReceipt)
                .build();
    }

    // Lấy thông tin tóm tắt của một room theo người dùng đang kết nối WebSocket.
    @Transactional(readOnly = true)
    public ChatRoomResponse getRoomSummaryForPrincipal(Long roomId, Principal principal) {
        if (principal == null) {
            throw new ApiError(ErrorCode.UNAUTHORIZED);
        }

        User viewer = userRepo.findByEmailIgnoreCase(principal.getName())
                .orElseThrow(() -> new ApiError(ErrorCode.USER_NOT_FOUND));

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ApiError(ErrorCode.BAD_REQUEST, "Khong tim thay room chat"));

        chatAccessGuard.validateRoomAccess(room, viewer);

        return chatRoomViewAssembler.toRoomResponseForViewer(room, viewer);
    }

    // Tạo bản tóm tắt room dùng chung cho phát realtime tới danh sách phòng.
    // Mặc định dùng góc nhìn admin để unreadCount đúng khi customer gửi tin.
    @Transactional(readOnly = true)
    public ChatRoomResponse getRoomSummaryForBroadcast(Long roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ApiError(ErrorCode.BAD_REQUEST, "Khong tim thay room chat"));

        User adminViewer = room.getAdmin() != null
                ? room.getAdmin()
                : userRepo.findFirstActiveAdminForChatSummary()
                        .orElseThrow(() -> new ApiError(ErrorCode.USER_NOT_FOUND));

        return chatRoomViewAssembler.toRoomResponseForViewer(room, adminViewer);
    }

    // Tạo tin nhắn hệ thống thông báo admin đã tiếp nhận room.
    private Message createAdminAssignedSystemMessage(ChatRoom room, User admin) {
        Message message = new Message();
        message.setRoom(room);
        message.setContent("Ban da duoc admin " + admin.getFullName() + " tiep nhan.");
        message.setMessageType(MessageType.SYSTEM);

        return message;
    }

    // Tạo tin nhắn văn bản do user hoặc admin gửi.
    private Message createTextMessage(ChatRoom room, User sender, String content) {
        Message message = new Message();
        message.setRoom(room);
        message.setSender(sender);
        message.setContent(content);
        message.setMessageType(MessageType.TEXT);

        return message;
    }
}