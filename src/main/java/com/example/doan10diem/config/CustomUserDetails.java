package com.example.doan10diem.config;

import com.example.doan10diem.model.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * UserDetails bọc User entity, cung cấp fullName để hiển thị trên header
 * và tương thích Spring Security.
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        if (user.getStatus() == null) return true;
        return !user.getStatus().name().equals("BANNED");
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        if (user.getStatus() == null) return false;
        String s = user.getStatus().name();
        return "ACTIVE".equals(s) || "PENDING".equals(s); // PENDING: giảng viên chờ duyệt vẫn được đăng nhập
    }

    /** Tên hiển thị (fullName) dùng cho header "Xin chào, ..." */
    public String getFullName() {
        return user.getFullName();
    }
}
