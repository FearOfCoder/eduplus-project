package com.example.doan10diem.config;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.example.doan10diem.model.User;
import com.example.doan10diem.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final UserRepository userRepository;

    public CustomAuthenticationFailureHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String redirectUrl = "/login?error=bad_credentials";
        if (exception instanceof DisabledException) {
            String username = request.getParameter("username");
            if (username != null && !username.isBlank()) {
                Optional<User> optUser = userRepository.findByUsername(username);
                if (optUser.isPresent() && optUser.get().getEmail() != null) {
                    redirectUrl = "/login?error=unverified&email="
                            + URLEncoder.encode(optUser.get().getEmail(), StandardCharsets.UTF_8);
                } else {
                    redirectUrl = "/login?error=unverified";
                }
            } else {
                redirectUrl = "/login?error=unverified";
            }
        } else if (exception instanceof LockedException) {
            redirectUrl = "/login?error=locked";
        }
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
