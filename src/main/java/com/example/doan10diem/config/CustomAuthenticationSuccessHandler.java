package com.example.doan10diem.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String roles = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        String redirectUrl;
        if (roles.contains("ROLE_ADMIN")) {
            redirectUrl = "/admin/dashboard?loginSuccess";
        } else if (roles.contains("ROLE_TEACHER")) {
            redirectUrl = "/teacher/dashboard?loginSuccess";
        } else {
            redirectUrl = "/?loginSuccess";
        }
        response.sendRedirect(request.getContextPath() + redirectUrl);
    }
}
