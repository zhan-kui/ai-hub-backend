package com.aihub.service;

import com.aihub.common.exception.BizException;
import com.aihub.dto.auth.RegisterUserRequest;
import com.aihub.dto.user.ChangePasswordRequest;
import com.aihub.dto.user.UpdateUserRequest;
import com.aihub.dto.user.UserInfoVO;
import com.aihub.entity.Role;
import com.aihub.entity.User;
import com.aihub.mapper.UserMapper;
import com.aihub.repository.RoleRepository;
import com.aihub.repository.UserRepository;
import com.aihub.security.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserInfoVO getCurrentUserInfo() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BizException(404, "用户不存在"));
        Role role = roleRepository.findById(user.getRoleId())
                .orElseThrow(() -> new BizException(404, "角色不存在"));
        return toVO(user, role);
    }

    @Transactional(rollbackFor = Exception.class)
    public UserInfoVO updateCurrentUser(UpdateUserRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BizException(404, "用户不存在"));

        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setAvatar(request.getAvatar());

        User saved = userRepository.save(user);
        Role role = roleRepository.findById(saved.getRoleId())
                .orElseThrow(() -> new BizException(404, "角色不存在"));
        return toVO(saved, role);
    }

    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BizException(404, "用户不存在"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BizException(400, "旧密码错误");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public List<UserInfoVO> listUsers(String keyword, Integer page, Integer size) {
        int current = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 20 : size;

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getDeleted, false)
                .orderByDesc(User::getId);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword)
                    .or().like(User::getEmail, keyword)
                    .or().like(User::getPhone, keyword));
        }

        Page<User> pager = userMapper.selectPage(new Page<>(current, pageSize), wrapper);
        List<User> users = pager.getRecords();
        if (users.isEmpty()) {
            return List.of();
        }

        Map<Long, Role> roleMap = roleRepository.findAllByDeletedFalseOrderBySortAsc().stream()
                .collect(Collectors.toMap(Role::getId, Function.identity()));

        return users.stream()
                .map(user -> toVO(user, roleMap.get(user.getRoleId())))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public UserInfoVO createUser(RegisterUserRequest request) {
        if (userRepository.existsByUsernameAndDeletedFalse(request.getUsername())) {
            throw new BizException(400, "用户名已存在");
        }

        Role defaultRole = roleRepository.findByCodeAndDeletedFalse("user")
                .orElseThrow(() -> new BizException(500, "默认角色不存在"));

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(StringUtils.hasText(request.getNickname())
                ? request.getNickname() : request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRoleId(defaultRole.getId());
        user.setStatus(1);
        user.setDeleted(false);

        User saved = userRepository.save(user);
        return toVO(saved, defaultRole);
    }

    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id, Integer status) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BizException(404, "用户不存在"));

        if (!Objects.equals(status, 0) && !Objects.equals(status, 1)) {
            throw new BizException(400, "状态值仅支持 0 或 1");
        }

        user.setStatus(status);
        userRepository.save(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeRole(Long id, Long roleId) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BizException(404, "用户不存在"));
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BizException(404, "角色不存在"));

        user.setRoleId(role.getId());
        userRepository.save(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BizException(404, "用户不存在"));
        user.setDeleted(true);
        userRepository.save(user);
    }

    private UserInfoVO toVO(User user, Role role) {
        return UserInfoVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .email(user.getEmail())
                .phone(user.getPhone())
                .roleCode(role != null ? role.getCode() : null)
                .roleName(role != null ? role.getName() : null)
                .status(user.getStatus())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}