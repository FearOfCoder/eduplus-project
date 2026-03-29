package com.example.doan10diem.controller;

import com.example.doan10diem.dto.RegisterDTO;
import com.example.doan10diem.model.Role;
import com.example.doan10diem.model.User;
import com.example.doan10diem.model.UserStatus;
import com.example.doan10diem.repository.UserRepository;
import com.example.doan10diem.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/verify")
    public String verifyEmail(@RequestParam("code") String code) {
        Optional<User> optUser = userRepository.findByVerificationCode(code);
        if (optUser.isEmpty()) {
            return "redirect:/login?error=invalid_code";
        }
        User user = optUser.get();
        user.setEmailVerified(true);
        user.setVerificationCode(null);

        if (user.getRole() == Role.STUDENT) {
            user.setStatus(UserStatus.ACTIVE);
        }
        // TEACHER: giữ nguyên PENDING (chờ Admin duyệt chuyên môn)
        userRepository.save(user);
        return "redirect:/login?verified";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        RegisterDTO dto = new RegisterDTO();
        dto.setRole(Role.STUDENT);
        model.addAttribute("registerDTO", dto);
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerDTO") RegisterDTO dto,
                           BindingResult result,
                           Model model) {
        if (result.hasErrors()) {
            model.addAttribute("registerDTO", dto);
            return "auth/register";
        }
        try {
            userService.register(dto);
            return "redirect:/login?registered";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("registerDTO", dto);
            return "auth/register";
        }
    }

    @PostMapping("/auth/resend-verification")
    public String resendVerification(@RequestParam String email) {
        try {
            userService.resendVerificationCode(email);
            return "redirect:/login?resend_success";
        } catch (RuntimeException e) {
            return "redirect:/login?resend_error";
        }
    }
}
