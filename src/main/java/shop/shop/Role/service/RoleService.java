package shop.shop.Role.service;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import shop.shop.admin.dto.response.RoleRepone;
import shop.shop.Role.mapper.RoleMapper;
import shop.shop.Role.repo.RoleRepository;
import shop.shop.common.dto.response.ApiResponse;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
@RequiredArgsConstructor
public class RoleService {
    RoleRepository roleRepository;
    RoleMapper roleMapper;

    public ApiResponse<List<RoleRepone>> getRole(){
        return ApiResponse.success("Lấy danh sách role thành công",  roleMapper.toRoleRepone(roleRepository.findAll()));
    }
}
