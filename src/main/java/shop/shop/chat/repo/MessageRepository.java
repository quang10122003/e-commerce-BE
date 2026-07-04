package shop.shop.chat.repo;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import shop.shop.chat.entity.Message;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {
    // Lay tin nhan cua room theo id.
    @EntityGraph(attributePaths = { "sender", "room" })
    @Query("""
            select message
            from Message message
            where message.room.id = :roomId
            order by message.createdAt asc, message.id asc
            """)
    List<Message> findMessagesByRoom(Long roomId);


    // Lay tin nhan cuoi cung cua room.
    @EntityGraph(attributePaths = { "sender", "room" })
    @Query("""
            select message
            from Message message
            where message.room.id = :roomId
            order by message.createdAt desc, message.id desc
            limit 1
            """)
    Optional<Message> findLatestMessageByRoom(@Param("roomId") Long roomId);

    // Dem so tin nhan TEXT chua doc trong mot room duoc gui boi mot sender cu the.
    @Query("""
            select count(message)
            from Message message
            where message.room.id = :roomId
              and message.read = false
              and message.messageType = shop.shop.common.MessageType.TEXT
              and message.sender.id = :senderId
            """)
    long countUnreadTextMessagesFromSender(
            @Param("roomId") Long roomId,
            @Param("senderId") Long senderId);

    // Lay id cac tin nhan TEXT chua doc truoc khi cap nhat de phat read-receipt realtime.
    @Query("""
            select message.id
            from Message message
            where message.room.id = :roomId
              and message.read = false
              and message.messageType = shop.shop.common.MessageType.TEXT
              and message.sender.id = :senderId
            order by message.createdAt asc, message.id asc
            """)
    List<Long> findUnreadTextMessageIdsFromSender(
            @Param("roomId") Long roomId,
            @Param("senderId") Long senderId);

    // Danh dau tat ca tin nhan TEXT chua doc tu mot sender trong mot room thanh da doc.
    @Modifying
    @Query("""
            update Message message
            set message.read = true
            where message.room.id = :roomId
              and message.read = false
              and message.messageType = shop.shop.common.MessageType.TEXT
              and message.sender.id = :senderId
            """)
    int markTextMessagesFromSenderAsRead(
            @Param("roomId") Long roomId,
            @Param("senderId") Long senderId);
}
