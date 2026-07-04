package shop.shop.Role.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import shop.shop.Role.entity.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
    Optional<Role> findByNameIgnoreCase(String name);
}
