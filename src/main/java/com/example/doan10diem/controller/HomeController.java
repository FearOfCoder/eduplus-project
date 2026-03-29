package com.example.doan10diem.controller;

import com.example.doan10diem.model.Course;
import com.example.doan10diem.model.CourseStatus;
import com.example.doan10diem.model.Enrollment;
import com.example.doan10diem.model.Lesson;
import com.example.doan10diem.model.Role;
import com.example.doan10diem.model.User;
import com.example.doan10diem.repository.EnrollmentRepository;
import com.example.doan10diem.repository.UserRepository;
import com.example.doan10diem.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.doan10diem.repository.LessonRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final CourseService courseService;
    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("courses", courseService.getTopCourses(8));
        return "index";
    }

    /**
     * Trang giới thiệu khóa học công khai (Sales Page - Udemy/Coursera style).
     * Không cần đăng nhập. Nếu đã đăng nhập (STUDENT) thì kiểm tra đã mua để hiện "Vào học" hay "Mua ngay".
     */
    @GetMapping("/course/{id}/intro")
    public String courseIntro(@PathVariable Long id, Authentication authentication, Model model) {
        Course course = courseService.findByIdWithTeacherInitialized(id);
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new IllegalArgumentException("Khóa học không tồn tại hoặc chưa được xuất bản");
        }
        List<Lesson> lessons = lessonRepository.findByCourseOrderBySortOrderAsc(course);
        model.addAttribute("course", course);
        model.addAttribute("lessons", lessons);

        boolean hasEnrollment = false;
        if (authentication != null && authentication.isAuthenticated()) {
            Optional<User> userOpt = userRepository.findByUsername(authentication.getName());
            if (userOpt.isPresent() && userOpt.get().getRole() == Role.STUDENT) {
                Optional<Enrollment> enrollmentOpt = enrollmentRepository.findByCourseAndStudent(course, userOpt.get());
                hasEnrollment = enrollmentOpt.isPresent();
            }
        }
        model.addAttribute("hasEnrollment", hasEnrollment);
        return "student/course-intro";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        boolean isAdmin = authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isTeacher = authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"));

        if (isAdmin) {
            return "redirect:/admin/dashboard";
        } else if (isTeacher) {
            return "redirect:/teacher/dashboard";
        } else {
            return "redirect:/student/dashboard";
        }
    }
}

