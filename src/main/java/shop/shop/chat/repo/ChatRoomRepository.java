package shop.shop.chat.repo;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import shop.shop.chat.entity.ChatRoom;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    @Override
    @EntityGraph(attributePaths = { "product", "user", "admin" })
    Optional<ChatRoom> findById(Long id);

    @EntityGraph(attributePaths = { "product", "user", "admin" })
    @Query("""
            select room
            from ChatRoom room
            where room.product.id = :productId and room.user.id = :userId
            """)
    Optional<ChatRoom> findRoom(Long productId, Long userId);

    @EntityGraph(attributePaths = { "product", "user", "admin" })
    @Query("""
            select room
            from ChatRoom room
            where room.user.id = :userId
            order by room.createdAt desc
            """)
    List<ChatRoom> findRoomsByUser(Long userId);

    // Tìm room của user theo tên sản phẩm.
    @EntityGraph(attributePaths = { "product", "user", "admin" })
    @Query("""
            select room
            from ChatRoom room
            where room.user.id = :userId
            and lower(room.product.name) like lower(concat('%', :search, '%'))
            order by room.createdAt desc
            """)
    List<ChatRoom> findRoomsByUserAndProductName(Long userId, String search);

    @EntityGraph(attributePaths = { "product", "user", "admin" })
    @Query("""
            select room
            from ChatRoom room
            where room.admin.id = :adminId or room.admin is null
            order by room.createdAt desc
            """)
    List<ChatRoom> findRoomsForAdmin(Long adminId);

    // Tìm room admin phụ trách hoặc chưa gán theo tên sản phẩm.
    @EntityGraph(attributePaths = { "product", "user", "admin" })
    @Query("""
            select room
            from ChatRoom room
            where (room.admin.id = :adminId or room.admin is null)
            and lower(room.product.name) like lower(concat('%', :search, '%'))
            order by room.createdAt desc
            """)
    List<ChatRoom> findRoomsForAdminAndProductName(Long adminId, String search);
}
