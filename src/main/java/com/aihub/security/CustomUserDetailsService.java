package com.aihub.security;

import com.aihub.entity.Role;
import com.aihub.entity.User;
import com.aihub.repository.RoleRepository;
import com.aihub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));

        Role role = roleRepository.findById(user.getRoleId())
                .orElseThrow(() -> new UsernameNotFoundException("用户角色不存在"));

        return new CustomUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                role.getCode(),
                user.getStatus()
        );
    }
}