package com.aihub.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomUserDetails implements UserDetails {

    private Long userId;
    private String username;
    private String password;
    private String roleCode;
    private Integer status;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + roleCode.toUpperCase()));
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != null && status == 1;
    }

    @Override
    public boolean isEnabled() {
        return status != null && status == 1;
    }
}