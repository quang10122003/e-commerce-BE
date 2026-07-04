package shop.shop.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import shop.shop.admin.dto.response.AdminUserDetailResponse;
import shop.shop.admin.dto.response.AdminUserSummaryResponse;
import shop.shop.user.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
     @Mapping(target = "role", source = "user", qualifiedByName = "toRoleName")
    @Mapping(target = "status", source = "user", qualifiedByName = "toStatus")
    AdminUserSummaryResponse toSummary(User user);

    @Mapping(target = "role", source = "user", qualifiedByName = "toRoleName")
    AdminUserDetailResponse toDetail(User user);

    @Named("toRoleName")
    default String toRoleName(User user) {
        if (user.getRole() == null || user.getRole().getName() == null) {
            return null;
        }
        String name = user.getRole().getName();
        return name.startsWith("ROLE_") ? name.substring(5) : name;
    }

    @Named("toStatus")
    default String toStatus(User user) {
        return user.isLocked() ? "LOCKED" : "ACTIVE";
    }
}
