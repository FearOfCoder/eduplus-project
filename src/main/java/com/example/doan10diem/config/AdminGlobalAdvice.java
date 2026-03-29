package com.example.doan10diem.config;

import com.example.doan10diem.model.CourseStatus;
import com.example.doan10diem.model.Role;
import com.example.doan10diem.model.SystemSetting;
import com.example.doan10diem.model.UserStatus;
import com.example.doan10diem.repository.CourseRepository;
import com.example.doan10diem.repository.SystemSettingRepository;
import com.example.doan10diem.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class AdminGlobalAdvice {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final SystemSettingRepository systemSettingRepository;

    @ModelAttribute("siteLogo")
    public String getSiteLogo() {
        return systemSettingRepository.findBySettingKey("SITE_LOGO")
                .map(SystemSetting::getSettingValue)
                .orElse("/images/logoEduPlus.jpg");
    }

    @ModelAttribute("pendingCourseCount")
    public long getPendingCourseCount(HttpServletRequest request) {
        if (request != null && request.getRequestURI().startsWith("/admin")) {
            return courseRepository.countByStatus(CourseStatus.PENDING_REVIEW);
        }
        return 0;
    }

    @ModelAttribute("pendingTeacherCount")
    public long getPendingTeacherCount(HttpServletRequest request) {
        if (request != null && request.getRequestURI().startsWith("/admin")) {
            return userRepository.countByRoleAndStatus(Role.TEACHER, UserStatus.PENDING);
        }
        return 0;
    }
}
