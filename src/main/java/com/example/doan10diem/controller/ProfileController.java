package com.example.doan10diem.controller;

import com.example.doan10diem.config.CustomUserDetails;
import com.example.doan10diem.dto.ChangePasswordDTO;
import com.example.doan10diem.dto.ProfileUpdateDTO;
import com.example.doan10diem.model.Role;
import com.example.doan10diem.model.User;
import com.example.doan10diem.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    private User getCurrentUser(Authentication auth) {
        if (auth == null) return null;
        if (auth.getPrincipal() instanceof CustomUserDetails details) {
            return details.getUser();
        }
        String username = auth.getName();
        return userService.findByUsername(username).orElse(null);
    }

    @GetMapping
    public String profilePage(Authentication auth, Model model) {
        User user = getCurrentUser(auth);
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        if (!model.containsAttribute("profileUpdateDTO")) {
            ProfileUpdateDTO dto = new ProfileUpdateDTO();
            dto.setFullName(user.getFullName());
            dto.setPhoneNumber(user.getPhoneNumber());
            dto.setAddress(user.getAddress());
            dto.setBio(user.getBio());
            dto.setUniversity(user.getUniversity());
            if (user.getRole() == Role.TEACHER) {
                dto.setCccdNumber(user.getCccdNumber());
                dto.setExperience(user.getExperience());
                dto.setPortfolioUrl(user.getPortfolioUrl());
            }
            model.addAttribute("profileUpdateDTO", dto);
        }
        if (!model.containsAttribute("changePasswordDTO")) {
            model.addAttribute("changePasswordDTO", new ChangePasswordDTO());
        }
        return "profile";
    }

    @PostMapping("/update")
    public String updateProfile(Authentication auth,
                                @Valid @ModelAttribute("profileUpdateDTO") ProfileUpdateDTO dto,
                                BindingResult result,
                                RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(auth);
        if (user == null) {
            return "redirect:/login";
        }
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.profileUpdateDTO", result);
            redirectAttributes.addFlashAttribute("profileUpdateDTO", dto);
            redirectAttributes.addFlashAttribute("profileError", "Vui lòng sửa lỗi trong form.");
            return "redirect:/profile";
        }
        try {
            userService.updateProfile(user, dto);
            // Reload user từ DB và cập nhật SecurityContext để header hiển thị fullName mới
            User updatedUser = userService.findByUsername(auth.getName()).orElse(user);
            CustomUserDetails newDetails = new CustomUserDetails(updatedUser);
            Authentication newAuth = new UsernamePasswordAuthenticationToken(
                    newDetails, auth.getCredentials(), newDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(newAuth);
            redirectAttributes.addFlashAttribute("profileSuccess", "Cập nhật thông tin thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("profileError", e.getMessage());
            redirectAttributes.addFlashAttribute("profileUpdateDTO", dto);
        }
        return "redirect:/profile";
    }

    @PostMapping("/avatar")
    public String uploadAvatar(Authentication auth,
                               @RequestParam("file") MultipartFile file,
                               RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(auth);
        if (user == null) {
            return "redirect:/login";
        }
        try {
            userService.updateAvatar(user, file);
            User updatedUser = userService.findByUsername(auth.getName()).orElse(user);
            CustomUserDetails newDetails = new CustomUserDetails(updatedUser);
            Authentication newAuth = new UsernamePasswordAuthenticationToken(
                    newDetails, auth.getCredentials(), newDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(newAuth);
            redirectAttributes.addFlashAttribute("avatarSuccess", "Cập nhật ảnh đại diện thành công.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("avatarError", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("avatarError", "Lỗi khi tải ảnh: " + e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/password")
    public String changePassword(Authentication auth,
                                @Valid @ModelAttribute("changePasswordDTO") ChangePasswordDTO dto,
                                BindingResult result,
                                RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(auth);
        if (user == null) {
            return "redirect:/login";
        }
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.changePasswordDTO", result);
            redirectAttributes.addFlashAttribute("changePasswordDTO", dto);
            redirectAttributes.addFlashAttribute("passwordError", "Vui lòng sửa lỗi trong form.");
            return "redirect:/profile";
        }
        try {
            userService.changePassword(user, dto);
            redirectAttributes.addFlashAttribute("passwordSuccess", "Đổi mật khẩu thành công.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("passwordError", e.getMessage());
            redirectAttributes.addFlashAttribute("changePasswordDTO", dto);
        }
        return "redirect:/profile";
    }
}
