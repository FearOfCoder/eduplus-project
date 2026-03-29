package com.example.doan10diem.controller;

import com.example.doan10diem.model.Category;
import com.example.doan10diem.model.Course;
import com.example.doan10diem.model.CourseStatus;
import com.example.doan10diem.model.Lesson;
import com.example.doan10diem.model.NotificationType;
import com.example.doan10diem.model.Role;
import com.example.doan10diem.model.SystemConfig;
import com.example.doan10diem.model.SystemSetting;
import com.example.doan10diem.model.User;
import com.example.doan10diem.model.UserStatus;
import com.example.doan10diem.repository.CategoryRepository;
import com.example.doan10diem.repository.CourseRepository;
import com.example.doan10diem.repository.LessonRepository;
import com.example.doan10diem.repository.SystemConfigRepository;
import com.example.doan10diem.repository.SystemSettingRepository;
import com.example.doan10diem.repository.UserRepository;
import com.example.doan10diem.service.CourseService;
import com.example.doan10diem.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseService courseService;
    private final NotificationService notificationService;
    private final SystemConfigRepository systemConfigRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final LessonRepository lessonRepository;
    private final CategoryRepository categoryRepository;

    @Value("${file.avatar.dir:user-photos}")
    private String uploadsBaseDir;
    @GetMapping("/admin/users")
    public String users(Model model) {
        List<User> pendingTeachers = userRepository.findByRoleAndStatus(Role.TEACHER, UserStatus.PENDING);
        // Lấy danh sách nhưng loại bỏ ADMIN để tránh tự khóa/xóa
        List<User> users = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && u.getRole() != Role.ADMIN)
                .collect(Collectors.toList());
        model.addAttribute("pendingTeachers", pendingTeachers);
        model.addAttribute("users", users);
        return "admin/users";
    }

    @GetMapping("/admin/users/detail/{id}")
    public String viewUserDetail(@PathVariable Long id, Model model) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User không tồn tại"));
        model.addAttribute("user", user);
        return "admin/user-detail";
    }

    @PostMapping("/admin/teacher/approve/{userId}")
    public String approveTeacher(@PathVariable Long userId, RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User không tồn tại"));
        if (user.getRole() != Role.TEACHER || user.getStatus() != UserStatus.PENDING) {
            redirectAttributes.addFlashAttribute("message", "Chỉ có thể duyệt giảng viên đang chờ duyệt.");
            return "redirect:/admin/users";
        }
        user.setStatus(UserStatus.ACTIVE);
        user.setIsVerified(true);
        userRepository.save(user);
        notificationService.createNotification(user, "Hồ sơ đã được duyệt! 🎉",
                "Chào mừng bạn gia nhập đội ngũ giảng viên. Hãy bắt đầu tạo khóa học ngay.",
                "/teacher/courses/new", NotificationType.SYSTEM);
        redirectAttributes.addFlashAttribute("message", "Đã duyệt giảng viên: " + user.getFullName());
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/teacher/reject/{userId}")
    public String rejectTeacher(@PathVariable Long userId, RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User không tồn tại"));
        if (user.getRole() != Role.TEACHER || user.getStatus() != UserStatus.PENDING) {
            redirectAttributes.addFlashAttribute("message", "Chỉ có thể từ chối giảng viên đang chờ duyệt.");
            return "redirect:/admin/users";
        }
        user.setStatus(UserStatus.REJECTED);
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("message", "Đã từ chối hồ sơ: " + user.getFullName());
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/toggle-ban/{id}")
    public String toggleBanUser(@PathVariable Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            if (user.getStatus() == UserStatus.BANNED) {
                user.setStatus(UserStatus.ACTIVE);
            } else {
                user.setStatus(UserStatus.BANNED);
            }
            userRepository.save(user);
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMsg", "Đã xóa tài khoản thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Không thể xóa! Tài khoản này đang có dữ liệu liên quan (khóa học, đăng ký...).");
        }
        return "redirect:/admin/users";
    }

    /**
     * Màn hình quản lý & duyệt khóa học.
     */
    @GetMapping("/admin/courses")
    public String manageCourses(Model model) {
        // Sử dụng service để load courses với teacher đã được initialize
        List<Course> allCourses = courseService.findAllWithTeacherInitialized();
        model.addAttribute("courses", allCourses);
        return "admin/courses";
    }

    @GetMapping("/admin/courses/detail/{id}")
    public String showCourseDetail(@PathVariable("id") Long id, Model model) {
        Course course = courseRepository.findById(id).orElse(null);
        if (course == null) {
            return "redirect:/admin/courses?error=NotFound";
        }
        course = courseService.findByIdWithTeacherInitialized(id);
        model.addAttribute("course", course);
        return "admin/course-detail";
    }

    @PostMapping("/admin/courses/{id}/approve")
    public String approveCourse(@PathVariable Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khóa học không tồn tại"));

        course.setStatus(CourseStatus.PUBLISHED);
        if (course.getPublishedAt() == null) {
            course.setPublishedAt(LocalDateTime.now());
        }
        courseRepository.save(course);

        return "redirect:/admin/courses";
    }

    @PostMapping("/admin/courses/{id}/reject")
    public String rejectCourse(@PathVariable Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khóa học không tồn tại"));

        course.setStatus(CourseStatus.REJECTED);
        courseRepository.save(course);

        return "redirect:/admin/courses";
    }

    @PostMapping("/admin/courses/toggle-status/{id}")
    public String toggleCourseStatus(@PathVariable Long id) {
        Course course = courseRepository.findById(id).orElse(null);
        if (course != null && course.getStatus() != null) {
            String status = course.getStatus().name();
            if ("PUBLISHED".equals(status)) {
                course.setStatus(CourseStatus.INACTIVE);
            } else if ("INACTIVE".equals(status)) {
                course.setStatus(CourseStatus.PUBLISHED);
                if (course.getPublishedAt() == null) {
                    course.setPublishedAt(LocalDateTime.now());
                }
            }
            courseRepository.save(course);
        }
        return "redirect:/admin/courses";
    }

    @PostMapping("/admin/courses/delete/{id}")
    public String deleteCourse(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            courseRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMsg", "Đã xóa khóa học thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Không thể xóa! Khóa học này đang chứa dữ liệu liên quan (bài học, đánh giá...).");
        }
        return "redirect:/admin/courses";
    }

    /**
     * API lấy chi tiết khóa học để hiển thị trong Modal (JSON response).
     */
    @GetMapping("/admin/courses/{id}/details")
    @org.springframework.web.bind.annotation.ResponseBody
    public java.util.Map<String, Object> getCourseDetails(@PathVariable Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khóa học không tồn tại"));

        // Force load teacher
        User teacher = course.getTeacher();
        if (teacher != null) {
            teacher.getFullName();
            teacher.getEmail();
            teacher.getUsername();
        }

        // Tìm Lesson đầu tiên (sortOrder = 0) - Bài học "Lời mở đầu"
        Lesson introLesson = lessonRepository.findByCourseOrderBySortOrderAsc(course).stream()
                .filter(l -> l.getSortOrder() != null && l.getSortOrder() == 0)
                .findFirst()
                .orElse(null);

        java.util.Map<String, Object> details = new java.util.HashMap<>();
        details.put("courseId", course.getId());
        details.put("courseTitle", course.getTitle());
        details.put("courseDescription", course.getDescription());
        details.put("coursePrice", course.getPrice());
        details.put("preface", course.getPreface());
        details.put("imageUrl", course.getImageUrl());
        details.put("createdAt", course.getCreatedAt() != null ? 
                course.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : null);
        
        // Thông tin Teacher
        if (teacher != null) {
            java.util.Map<String, Object> teacherInfo = new java.util.HashMap<>();
            teacherInfo.put("fullName", teacher.getFullName());
            teacherInfo.put("email", teacher.getEmail());
            teacherInfo.put("username", teacher.getUsername());
            details.put("teacher", teacherInfo);
        }

        // Nội dung Bài học 0 (Lời mở đầu)
        if (introLesson != null) {
            details.put("introLessonTitle", introLesson.getTitle());
            details.put("introLessonContent", introLesson.getContent());
        } else {
            details.put("introLessonTitle", null);
            details.put("introLessonContent", course.getPreface()); // Fallback về preface nếu chưa có lesson
        }

        return details;
    }

    /**
     * Trang quản lý cấu hình hệ thống (Điều khoản cho Teacher).
     */
    @GetMapping("/admin/settings")
    public String settings(Model model) {
        SystemConfig teacherTerms = systemConfigRepository.findByConfigKey("TEACHER_TERMS")
                .orElseGet(() -> SystemConfig.builder()
                        .configKey("TEACHER_TERMS")
                        .configValue("1. Phí nền tảng là 20% doanh thu.\n2. Giảng viên cam kết nội dung không vi phạm bản quyền.\n3. Nghiêm cấm nội dung chống phá nhà nước hoặc vi phạm pháp luật.")
                        .build());

        SystemConfig platformFee = systemConfigRepository.findByConfigKey("PLATFORM_FEE")
                .orElseGet(() -> SystemConfig.builder()
                        .configKey("PLATFORM_FEE")
                        .configValue("20")
                        .build());

        model.addAttribute("teacherTerms", teacherTerms.getConfigValue());
        model.addAttribute("platformFee", platformFee.getConfigValue());
        return "admin/settings";
    }

    /**
     * Cập nhật điều khoản cho Teacher và phí nền tảng.
     */
    @PostMapping("/admin/settings/update")
    public String updateSettings(@org.springframework.web.bind.annotation.RequestParam("teacherTerms") String teacherTerms,
                                 @org.springframework.web.bind.annotation.RequestParam("platformFee") String platformFee) {
        // Cập nhật điều khoản
        SystemConfig termsConfig = systemConfigRepository.findByConfigKey("TEACHER_TERMS")
                .orElseGet(() -> SystemConfig.builder()
                        .configKey("TEACHER_TERMS")
                        .build());
        termsConfig.setConfigValue(teacherTerms != null ? teacherTerms.trim() : "");
        systemConfigRepository.save(termsConfig);

        // Cập nhật phí nền tảng
        if (platformFee != null && !platformFee.trim().isEmpty()) {
            try {
                Integer feeValue = Integer.parseInt(platformFee.trim());
                if (feeValue >= 0 && feeValue <= 100) {
                    SystemConfig feeConfig = systemConfigRepository.findByConfigKey("PLATFORM_FEE")
                            .orElseGet(() -> SystemConfig.builder()
                                    .configKey("PLATFORM_FEE")
                                    .build());
                    feeConfig.setConfigValue(String.valueOf(feeValue));
                    systemConfigRepository.save(feeConfig);
                }
            } catch (NumberFormatException e) {
                // Ignore invalid format
            }
        }

        return "redirect:/admin/settings?success=true";
    }

    @PostMapping("/admin/settings/logo")
    public String updateLogo(@RequestParam("logoFile") MultipartFile logoFile, RedirectAttributes redirectAttributes) {
        if (logoFile == null || logoFile.isEmpty()) {
            redirectAttributes.addFlashAttribute("logoError", "Vui lòng chọn file ảnh.");
            return "redirect:/admin/settings";
        }
        String contentType = logoFile.getContentType();
        if (contentType == null || (!contentType.startsWith("image/png") && !contentType.startsWith("image/jpeg") && !contentType.startsWith("image/jpg"))) {
            redirectAttributes.addFlashAttribute("logoError", "Chỉ chấp nhận file ảnh PNG hoặc JPEG.");
            return "redirect:/admin/settings";
        }
        try {
            Path logoDir = Paths.get(uploadsBaseDir, "logo");
            if (!Files.exists(logoDir)) {
                Files.createDirectories(logoDir);
            }
            String ext = contentType.contains("png") ? ".png" : ".jpg";
            String filename = "logo-" + UUID.randomUUID().toString().substring(0, 8) + ext;
            Path targetPath = logoDir.resolve(filename);
            Files.copy(logoFile.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            String logoUrl = "/uploads/logo/" + filename;

            SystemSetting setting = systemSettingRepository.findBySettingKey("SITE_LOGO")
                    .orElseGet(() -> SystemSetting.builder().settingKey("SITE_LOGO").build());
            setting.setSettingValue(logoUrl);
            systemSettingRepository.save(setting);
            redirectAttributes.addFlashAttribute("logoSuccess", "Đã cập nhật logo thành công!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("logoError", "Không thể lưu file: " + e.getMessage());
        }
        return "redirect:/admin/settings";
    }

    // ---------- Quản lý Danh mục (Categories) ----------

    @GetMapping("/admin/categories")
    public String categories(@RequestParam(value = "keyword", required = false) String keyword,
                            Model model) {
        Set<Long> categoryIdsInUse = new HashSet<>();
        Set<Long> categoryIdsWithChildren = new HashSet<>();
        for (Category c : categoryRepository.findAll()) {
            if (courseRepository.countByCategory(c) > 0) {
                categoryIdsInUse.add(c.getId());
            }
            if (c.getParent() != null) {
                categoryIdsWithChildren.add(c.getParent().getId());
            }
        }

        List<Category> categories;
        Set<Long> expandedCategoryIds = new HashSet<>();
        Set<Long> highlightCategoryIds = new HashSet<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            List<Category> matches = categoryRepository.findByNameContainingIgnoreCaseOrderByNameAsc(kw);
            Set<Long> rootIdsToShow = new HashSet<>();
            for (Category c : matches) {
                highlightCategoryIds.add(c.getId());
                Long rootId = (c.getParent() == null) ? c.getId() : c.getParent().getId();
                rootIdsToShow.add(rootId);
                expandedCategoryIds.add(rootId);
            }
            categories = rootIdsToShow.isEmpty()
                    ? new ArrayList<>()
                    : categoryRepository.findRootsByIdInWithChildrenOrderByName(rootIdsToShow);
        } else {
            categories = categoryRepository.findAllRootsWithChildrenOrderByName();
        }

        model.addAttribute("categories", categories);
        model.addAttribute("expandedCategoryIds", expandedCategoryIds);
        model.addAttribute("highlightCategoryIds", highlightCategoryIds);
        model.addAttribute("categoryIdsInUse", categoryIdsInUse);
        model.addAttribute("categoryIdsWithChildren", categoryIdsWithChildren);
        model.addAttribute("keyword", keyword);
        return "admin/categories";
    }

    @GetMapping("/admin/categories/new")
    public String categoryFormNew(Model model) {
        model.addAttribute("category", Category.builder().build());
        model.addAttribute("parentCategories", categoryRepository.findByParentIsNullOrderByNameAsc());
        return "admin/category-form";
    }

    @GetMapping("/admin/categories/{id}/edit")
    public String categoryFormEdit(@PathVariable Long id, Model model) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại"));
        model.addAttribute("category", category);
        model.addAttribute("parentCategories", categoryRepository.findByParentIsNullOrderByNameAsc());
        return "admin/category-form";
    }

    @PostMapping("/admin/categories/save")
    public String categorySave(
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "parentId", required = false) Long parentId,
            RedirectAttributes redirectAttributes) {
        name = name != null ? name.trim() : "";
        if (name.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Tên danh mục không được để trống");
            return id == null ? "redirect:/admin/categories/new" : "redirect:/admin/categories/" + id + "/edit";
        }
        Category category;
        if (id != null) {
            category = categoryRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại"));
        } else {
            category = new Category();
        }
        category.setName(name);
        category.setDescription(description != null ? description.trim() : null);
        if (parentId != null && parentId > 0) {
            Category parent = categoryRepository.findById(parentId).orElse(null);
            category.setParent(parent);
        } else {
            category.setParent(null);
        }
        categoryRepository.save(category);
        redirectAttributes.addFlashAttribute("message", id == null ? "Thêm danh mục thành công." : "Cập nhật danh mục thành công.");
        return "redirect:/admin/categories";
    }

    @PostMapping("/admin/categories/{id}/delete")
    public String categoryDelete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại"));
        if (!category.getChildren().isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Không thể xóa danh mục có danh mục con. Hãy xóa hoặc chuyển danh mục con trước.");
            return "redirect:/admin/categories";
        }
        if (courseRepository.countByCategory(category) > 0) {
            redirectAttributes.addFlashAttribute("message", "Không thể xóa danh mục đang được sử dụng bởi khóa học.");
            return "redirect:/admin/categories";
        }
        categoryRepository.delete(category);
        redirectAttributes.addFlashAttribute("message", "Đã xóa danh mục.");
        return "redirect:/admin/categories";
    }
}

