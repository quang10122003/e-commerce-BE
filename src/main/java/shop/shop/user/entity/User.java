package shop.shop.user.entity;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;
import shop.shop.Role.entity.Role;
import shop.shop.common.AuthProvider;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true)
    String email;

    @Column(name = "password_hash", columnDefinition = "TEXT")
    String password;

    @Column(name = "full_name", nullable = false)

    String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name ="provider",nullable = false)
    @Default
    AuthProvider provider = AuthProvider.LOCAL;

    @Column(length = 255,name = "provider_id")
    String providerId;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    Role role;

    @Column(name = "is_locked")
    boolean isLocked;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    Instant updatedAt;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String roleName = getRoleName();
        if (roleName.isBlank()) {
            return List.of();
        }

        return List.of(new SimpleGrantedAuthority(getAuthorityName()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !isLocked;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public String getRoleName() {
        if (role == null || role.getName() == null || role.getName().isBlank()) {
            return "";
        }
        return role.getName().startsWith("ROLE_")
                ? role.getName().substring("ROLE_".length())
                : role.getName();
    }

    public String getAuthorityName() {
        String roleName = getRoleName();
        return roleName.isBlank() ? "" : "ROLE_" + roleName;
    }
}
