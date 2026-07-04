package shop.shop.Role.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import shop.shop.admin.dto.response.RoleRepone;
import shop.shop.Role.entity.Role;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    List<RoleRepone> toRoleRepone(List<Role> roles);
}
