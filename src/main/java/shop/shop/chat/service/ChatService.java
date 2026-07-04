package shop.shop.chat.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    SimpMessagingTemplate messagingTemplate;

    // Tao phong chat moi cho user hien tai theo san pham.
    @Transactional
    public ApiResponse<ChatRoomResponse> createRoom(Long productId) {
        User user = getCurrentUser();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ApiError(ErrorCode.PRODUCT_NOT_FOUND));

        ChatRoom newRoom = new ChatRoom();
        newRoom.setProduct(product);
        newRoom.setUser(user);

        return ApiResponse.success("Lay room chat thanh cong", chatMapper.toRoomResponse(chatRoomRepository.save(newRoom)));
    }

    // Lay phong chat cua user hien tai theo san pham.
    @Transactional(readOnly = true)
    public ApiResponse<ChatRoomResponse> getCurrentUserRoomByProduct(Long productId) {
        User user = getCurrentUser();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ApiError(ErrorCode.PRODUCT_NOT_FOUND));

        ChatRoom room = chatRoomRepository
                .findRoom(product.getId(), user.getId())
                .orElseThrow(() -> new ApiError(ErrorCode.CHAT_ROOM_NOT_FOUND));

        return ApiResponse.success("Lay room chat thanh cong", toRoomResponseForViewer(room, user));
    }

    // Lay danh sach phong chat cua user hien tai.
    @Transactional(readOnly = true)
    public ApiResponse<List<ChatRoomResponse>> getCurrentUserRooms(String search) {
        User user = getCurrentUser();
        String normalizedSearch = normalize(search);
        List<ChatRoom> rooms = normalizedSearch == null
                ? chatRoomRepository.findRoomsByUser(user.getId())
                : chatRoomRepository.findRoomsByUserAndProductName(user.getId(), normalizedSearch);

        return ApiResponse.success(
                "Lay danh sach room chat cua user thanh cong",
                rooms
                        .stream()
                        .map(room -> toRoomResponseForViewer(room, user))
                        .toList());
    }

    // Lay danh sach phong chat cho admin va sap xep theo tin nhan moi nhat.
    @Transactional(readOnly = true)
    public ApiResponse<List<ChatRoomResponse>> getAdminRooms(String search) {
        User admin = getCurrentUser();
        String normalizedSearch = normalize(search);

        List<ChatRoom> sourceRooms = normalizedSearch == null
                ? chatRoomRepository.findRoomsForAdmin(admin.getId())
                : chatRoomRepository.findRoomsForAdminAndProductName(admin.getId(), normalizedSearch);

        List<ChatRoomResponse> rooms = sourceRooms
                .stream()
                .map(room -> toRoomResponseForViewer(room, admin))
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

    // Chuẩn hóa từ khóa tìm kiếm trước khi truyền xuống repository.
    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // Lay toan bo tin nhan trong mot phong chat sau khi kiem tra quyen truy cap.
    @Transactional(readOnly = true)
    public ApiResponse<List<ChatMessageResponse>> getMessages(Long roomId) {
        User user = getCurrentUser();
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ApiError(ErrorCode.BAD_REQUEST, "Khong tim thay room chat"));

        validateRoomAccess(room, user);

        return ApiResponse.success(
                "Lay tin nhan thanh cong",
                messageRepository.findMessagesByRoom(roomId)
                        .stream()
                        .map(chatMapper::toMessageResponse)
                        .toList());
    }

    // Luu tin nhan WebSocket va phat realtime cho room chat cung danh sach phong.
    // Lan admin phan hoi dau tien co the tao them tin SYSTEM truoc tin TEXT.
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

        validateRoomAccess(room, sender);
        boolean roomAssigned = assignRoomToAdminIfNeeded(room, sender);

        List<Message> persistedMessages = new ArrayList<>();

        if (roomAssigned) {
            persistedMessages.add(messageRepository.save(createAdminAssignedSystemMessage(room, sender)));
        }

        persistedMessages.add(messageRepository.save(createTextMessage(room, sender, request.getContent().trim())));

        List<ChatMessageResponse> responses = persistedMessages.stream()
                .map(chatMapper::toMessageResponse)
                .toList();

        // Gui tung tin nhan theo dung thu tu da luu: SYSTEM truoc, TEXT sau.
        responses.forEach(response -> messagingTemplate.convertAndSend("/topic/chat/rooms/" + roomId, response));

        // Gui tom tat moi de danh sach phong cap nhat lastMessage va unreadCount realtime.
        ChatRoomResponse roomSummary = getRoomSummaryForBroadcast(roomId);
        messagingTemplate.convertAndSend("/topic/chat/rooms", roomSummary);
    }

    // Danh dau cac tin nhan chua doc trong room la da doc boi user hien tai.
    @Transactional
    public MarkRoomAsReadResult markRoomAsRead(Long roomId) {
        User viewer = getCurrentUser();

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ApiError(ErrorCode.BAD_REQUEST, "Khong tim thay room chat"));

        validateRoomAccess(room, viewer);
        Long unreadSenderId = resolveUnreadSenderId(room, viewer);
        List<Long> readMessageIds = List.of();

        if (unreadSenderId != null) {
            // Lay danh sach message id truoc khi cap nhat de client ben gui biet tin nao da doc.
            readMessageIds = messageRepository.findUnreadTextMessageIdsFromSender(roomId, unreadSenderId);
            messageRepository.markTextMessagesFromSenderAsRead(roomId, unreadSenderId);
        }

        ChatRoomResponse roomResponseForReader = toRoomResponseForViewer(room, viewer);
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
                .adminRoomSummary(isAdmin(viewer) ? roomResponseForReader : null)
                .readReceipt(readReceipt)
                .build();
    }

    // Lay thong tin tom tat cua mot room theo nguoi dung dang ket noi WebSocket.
    @Transactional(readOnly = true)
    public ChatRoomResponse getRoomSummaryForPrincipal(Long roomId, Principal principal) {
        if (principal == null) {
            throw new ApiError(ErrorCode.UNAUTHORIZED);
        }

        User viewer = userRepo.findByEmailIgnoreCase(principal.getName())
                .orElseThrow(() -> new ApiError(ErrorCode.USER_NOT_FOUND));

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ApiError(ErrorCode.BAD_REQUEST, "Khong tim thay room chat"));

        validateRoomAccess(room, viewer);

        return toRoomResponseForViewer(room, viewer);
    }

    // Tao ban tom tat room dung chung cho phat realtime toi danh sach phong.
    // Mac dinh dung goc nhin admin de unreadCount dung khi customer gui tin.
    @Transactional(readOnly = true)
    public ChatRoomResponse getRoomSummaryForBroadcast(Long roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ApiError(ErrorCode.BAD_REQUEST, "Khong tim thay room chat"));

        User adminViewer = room.getAdmin() != null
                ? room.getAdmin()
                : userRepo.findFirstActiveAdminForChatSummary()
                        .orElseThrow(() -> new ApiError(ErrorCode.USER_NOT_FOUND));

        return toRoomResponseForViewer(room, adminViewer);
    }

    // Lay user hien tai tu SecurityContext cua request HTTP.
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new ApiError(ErrorCode.UNAUTHORIZED);
        }

        return userRepo.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new ApiError(ErrorCode.USER_NOT_FOUND));
    }

    // Kiem tra user co quyen xem hoac thao tac voi room hay khong.
    private void validateRoomAccess(ChatRoom room, User user) {
        if (isAdmin(user)) {
            // Room chua gan admin hoac da gan dung admin nay thi duoc truy cap.
            if (room.getAdmin() == null
                    || (room.getAdmin() != null && room.getAdmin().getId().equals(user.getId()))) {
                return;
            }
            throw new ApiError(ErrorCode.ACCESS_DENIED);
        }

        // User thong thuong chi duoc truy cap room thuoc ve chinh ho.
        if (room.getUser() == null || !room.getUser().getId().equals(user.getId())) {
            throw new ApiError(ErrorCode.ACCESS_DENIED);
        }
    }

    // Gan room cho admin khi admin phan hoi lan dau tien.
    private boolean assignRoomToAdminIfNeeded(ChatRoom room, User sender) {
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

    // Tao tin nhan he thong thong bao admin da tiep nhan room.
    private Message createAdminAssignedSystemMessage(ChatRoom room, User admin) {
        Message message = new Message();
        message.setRoom(room);
        message.setContent("Ban da duoc admin " + admin.getFullName() + " tiep nhan.");
        message.setMessageType(MessageType.SYSTEM);

        return message;
    }

    // Tao tin nhan van ban do user hoac admin gui.
    private Message createTextMessage(ChatRoom room, User sender, String content) {
        Message message = new Message();
        message.setRoom(room);
        message.setSender(sender);
        message.setContent(content);
        message.setMessageType(MessageType.TEXT);

        return message;
    }

    // Kiem tra user co role ADMIN hay khong.
    private boolean isAdmin(User user) {
        return user != null && "ADMIN".equalsIgnoreCase(user.getRoleName());
    }

    // Tao response room theo goc nhin cua nguoi xem, gom lastMessage va unreadCount.
    private ChatRoomResponse toRoomResponseForViewer(ChatRoom room, User viewer) {
        ChatRoomResponse chatRoomResponse = chatMapper.toRoomResponse(room);

        // Neu room co tin nhan cuoi thi dua thong tin do vao ban tom tat.
        messageRepository.findLatestMessageByRoom(room.getId()).ifPresent(lastMessage -> {
            chatRoomResponse.setLastMessageContent(lastMessage.getContent());

            chatRoomResponse.setLastMessageType(
                    lastMessage.getMessageType() == null ? null : lastMessage.getMessageType().toString());

            chatRoomResponse.setLastMessageAt(lastMessage.getCreatedAt());

            // Gan thong tin nguoi gui tin nhan cuoi cung neu la tin TEXT co sender.
            if (lastMessage.getSender() != null) {
                chatRoomResponse.setLastMessageSenderId(lastMessage.getSender().getId());
                chatRoomResponse.setLastMessageSenderName(lastMessage.getSender().getFullName());
            }
        });

        // Xac dinh phia gui can dem unread theo goc nhin cua viewer.
        Long unreadSenderId = resolveUnreadSenderId(room, viewer);

        if (unreadSenderId != null) {
            // Dem so tin TEXT chua doc tu phia doi dien trong room.
            chatRoomResponse.setUnreadCount(
                    messageRepository.countUnreadTextMessagesFromSender(
                            room.getId(),
                            unreadSenderId));
        }
        return chatRoomResponse;
    }

    // Lay id sender phia doi dien de tinh so tin nhan chua doc.
    private Long resolveUnreadSenderId(ChatRoom room, User viewer) {
        // Neu viewer la admin thi unread den tu customer.
        if (isAdmin(viewer)) {
            return room.getUser() == null
                    ? null
                    : room.getUser().getId();
        }

        // Neu viewer la customer thi unread den tu admin.
        return room.getAdmin() == null
                ? null
                : room.getAdmin().getId();
    }
}
