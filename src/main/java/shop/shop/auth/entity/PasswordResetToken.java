package shop.shop.auth.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import shop.shop.user.entity.User;

@Entity
@Table(name = "password_reset_token")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(unique = true, nullable = false)
    String token;

    // check xem token sử dụng chưa 
    boolean used;

    // thời gian hết hạn
    LocalDateTime expiredAt;

    @JoinColumn(name = "user_id",nullable = false )
    @ManyToOne(fetch = FetchType.LAZY)
     User user;
}