package shop.shop.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import shop.shop.Role.entity.Role;
import shop.shop.Role.repo.RoleRepository;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleDataInitializer implements ApplicationRunner {
    RoleRepository roleRepository;

    @Override
    public void run(ApplicationArguments args) {
        ensureRole("USER");
        ensureRole("ADMIN");
    }

    private void ensureRole(String normalizedName) {
        if (roleRepository.findByNameIgnoreCase(normalizedName).isPresent()) {
            return;
        }

        roleRepository.findByNameIgnoreCase("ROLE_" + normalizedName)
                .ifPresentOrElse(role -> {
                    role.setName(normalizedName);
                    roleRepository.save(role);
                }, () -> {
                    Role role = new Role();
                    role.setName(normalizedName);
                    roleRepository.save(role);
                });
    }
}
