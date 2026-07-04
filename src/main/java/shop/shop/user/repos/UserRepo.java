package shop.shop.user.repos;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import shop.shop.common.AuthProvider;
import shop.shop.user.entity.User;

public interface UserRepo extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            SELECT u FROM User u
            JOIN FETCH u.role r
            WHERE (:search IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')))
            AND (:role IS NULL OR UPPER(REPLACE(r.name, 'ROLE_', '')) = UPPER(:role))
            AND (:status IS NULL OR
                CASE WHEN u.isLocked = true THEN 'LOCKED' ELSE 'ACTIVE' END = UPPER(:status))
            """)
    Page<User> findAdminUsers(
            @Param("search") String search,
            @Param("role") String role,
            @Param("status") String status,
            Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u")
    long countAllUsersForAdmin();

    @Query("SELECT COUNT(u) FROM User u WHERE LOWER(REPLACE(u.role.name, 'ROLE_', '')) = 'admin'")
    long countAdminUsersForAdmin();

    @Query("""
            SELECT u FROM User u
            JOIN FETCH u.role r
            WHERE LOWER(REPLACE(r.name, 'ROLE_', '')) = 'admin'
              AND u.isLocked = false
            ORDER BY u.id ASC
            LIMIT 1
            """)
    Optional<User> findFirstActiveAdminForChatSummary();

    @Query("SELECT COUNT(u) FROM User u WHERE u.isLocked = true")
    long countLockedUsersForAdmin();

    // Lấy user đăng ký trong 7 ngày qua.
    @Query(value = "SELECT COUNT(*) FROM users u WHERE u.created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)", nativeQuery = true)
    long countNewUsersInLast7Days();

    
    Optional<User> findByEmailIgnoreCaseAndProvider(String email, AuthProvider provider);

   
    Optional<User> findByProviderId(String providerId);
}
