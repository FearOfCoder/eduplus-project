    package com.example.doan10diem.controller;

    import java.io.IOException;
    import java.math.BigDecimal;
    import java.nio.file.Files;
    import java.nio.file.Path;
    import java.nio.file.Paths;
    import java.nio.file.StandardCopyOption;
    import java.time.LocalDateTime;
    import java.time.format.DateTimeFormatter;
    import java.util.ArrayList;
    import java.util.List;
    import java.util.UUID;

    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.security.access.AccessDeniedException;
    import org.springframework.security.core.Authentication;
    import org.springframework.stereotype.Controller;
    import org.springframework.ui.Model;
    import org.springframework.validation.BindingResult;
    import org.springframework.web.bind.annotation.GetMapping;
    import org.springframework.web.bind.annotation.ModelAttribute;
    import org.springframework.web.bind.annotation.PathVariable;
    import org.springframework.web.bind.annotation.PostMapping;
    import org.springframework.web.bind.annotation.RequestParam;
    import org.springframework.web.multipart.MultipartFile;

    import com.example.doan10diem.dto.CourseRevenueDTO;
    import com.example.doan10diem.model.Assignment;
    import com.example.doan10diem.model.Course;
    import com.example.doan10diem.model.CourseStatus;
import com.example.doan10diem.model.Category;
import com.example.doan10diem.model.Lesson;
import com.example.doan10diem.model.LessonType;
import com.example.doan10diem.model.PaymentStatus;
    import com.example.doan10diem.model.Submission;
    import com.example.doan10diem.model.User;
    import com.example.doan10diem.model.UserStatus;
import com.example.doan10diem.repository.AssignmentRepository;
import com.example.doan10diem.repository.CategoryRepository;
import com.example.doan10diem.repository.CourseRepository;
import com.example.doan10diem.repository.LessonRepository;
    import com.example.doan10diem.repository.PaymentRepository;
    import com.example.doan10diem.repository.SubmissionRepository;
    import com.example.doan10diem.repository.SystemConfigRepository;
    import com.example.doan10diem.repository.UserRepository;
import com.example.doan10diem.service.CourseService;
import com.example.doan10diem.service.NotificationService;
import com.example.doan10diem.util.FileUploadUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

    @Controller
    @RequiredArgsConstructor
    public class TeacherController {

        private final UserRepository userRepository;
        private final CourseRepository courseRepository;
        private final PaymentRepository paymentRepository;
        private final AssignmentRepository assignmentRepository;
        private final SubmissionRepository submissionRepository;
        private final LessonRepository lessonRepository;
        private final CourseService courseService;
        private final NotificationService notificationService;
        private final SystemConfigRepository systemConfigRepository;
        private final CategoryRepository categoryRepository;

        @Value("${file.upload.dir:uploads}")
        private String uploadDir;

        @Value("${file.course-images.dir:course-images}")
        private String courseImagesDir;

        // Removed @ModelAttribute - set lessonTypes explicitly in each method to avoid null issues

        /**
         * Helper method để lưu file upload và trả về đường dẫn
         */
        private String saveUploadedFile(MultipartFile file, Long courseId) throws IOException {
            if (file == null || file.isEmpty()) {
                return null;
            }

            // Tạo thư mục upload nếu chưa có
            Path uploadPath = Paths.get(uploadDir, "courses", courseId.toString());
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Tạo tên file unique
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String uniqueFilename = UUID.randomUUID().toString() + extension;

            // Lưu file
            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Trả về đường dẫn relative để lưu vào database
            return "/" + uploadDir + "/courses/" + courseId + "/" + uniqueFilename;
        }

        @GetMapping("/teacher/dashboard")
        public String dashboard(Authentication authentication, Model model) {
            User teacher = currentUser(authentication);
            List<Course> courses = courseRepository.findByTeacher(teacher);
            List<CourseRevenueDTO> revenueList = paymentRepository.findCourseRevenueByTeacherAndStatus(teacher, PaymentStatus.SUCCESS);

            BigDecimal totalNetRevenue = revenueList.stream()
                    .map(CourseRevenueDTO::getNetTotal)
                    .filter(v -> v != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long totalStudents = revenueList.stream()
                    .mapToLong(dto -> dto.getStudentCount() != null ? dto.getStudentCount() : 0L)
                    .sum();
            int totalCoursesSelling = revenueList.size();

            List<String> courseNames = new ArrayList<>();
            List<Double> netAmounts = new ArrayList<>();
            List<Long> studentCounts = new ArrayList<>();
            for (CourseRevenueDTO dto : revenueList) {
                courseNames.add(dto.getCourseName() != null ? dto.getCourseName() : "");
                netAmounts.add(dto.getNetTotal() != null ? dto.getNetTotal().doubleValue() : 0.0);
                studentCounts.add(dto.getStudentCount() != null ? dto.getStudentCount() : 0L);
            }

            model.addAttribute("teacher", teacher);
            model.addAttribute("courses", courses);
            model.addAttribute("revenueList", revenueList);
            model.addAttribute("totalNetRevenue", totalNetRevenue);
            model.addAttribute("totalStudents", totalStudents);
            model.addAttribute("totalCoursesSelling", totalCoursesSelling);
            model.addAttribute("courseNames", courseNames);
            model.addAttribute("netAmounts", netAmounts);
            model.addAttribute("studentCounts", studentCounts);
            return "teacher/dashboard";
        }

        /**
         * Danh sách khóa học của giảng viên (màn riêng để CRUD).
         */
        @GetMapping("/teacher/courses")
        public String listCourses(Authentication authentication, Model model) {
            User teacher = currentUser(authentication);
            List<Course> courses = courseRepository.findByTeacher(teacher);
            model.addAttribute("teacher", teacher);
            model.addAttribute("courses", courses);
            return "teacher/courses";
        }

        @GetMapping("/teacher/courses/new")
        public String showCreateCourseForm(Authentication authentication, Model model) {
            User teacher = currentUser(authentication);
            if (teacher != null && teacher.getStatus() == UserStatus.PENDING) {
                return "redirect:/teacher/dashboard?error=pending_approval";
            }

            // Lấy điều khoản từ SystemConfig
            String teacherTerms = systemConfigRepository.findByConfigKey("TEACHER_TERMS")
                    .map(com.example.doan10diem.model.SystemConfig::getConfigValue)
                    .orElse("1. Phí nền tảng là 20% doanh thu.\n2. Giảng viên cam kết nội dung không vi phạm bản quyền.\n3. Nghiêm cấm nội dung chống phá nhà nước hoặc vi phạm pháp luật.");
            
            // Lấy phí nền tảng từ SystemConfig
            Integer platformFeePercent = systemConfigRepository.findByConfigKey("PLATFORM_FEE")
                    .map(config -> {
                        try {
                            return Integer.parseInt(config.getConfigValue());
                        } catch (NumberFormatException e) {
                            return 20; // Default fallback
                        }
                    })
                    .orElse(20);
            
            Course course = Course.builder()
                    .platformFeePercent(platformFeePercent)
                    .build();
            model.addAttribute("course", course);
            model.addAttribute("mode", "create");
            model.addAttribute("teacherTerms", teacherTerms);
            model.addAttribute("platformFeePercent", platformFeePercent);
            model.addAttribute("rootCategories", categoryRepository.findByParentIsNullOrderByNameAsc());
            return "teacher/course-form";
        }

        @PostMapping("/teacher/courses")
        public String createCourse(Authentication authentication,
                                @Valid @ModelAttribute("course") Course formCourse,
                                BindingResult bindingResult,
                                @RequestParam("preface") String preface,
                                @RequestParam(value = "agreeTerms", required = false) String agreeTerms,
                                @RequestParam(value = "categoryId", required = false) Long categoryId,
                                @RequestParam(value = "image", required = false) MultipartFile imageFile,
                                Model model) {
            User teacher = currentUser(authentication);
            if (teacher != null && teacher.getStatus() == UserStatus.PENDING) {
                return "redirect:/teacher/dashboard?error=pending_approval";
            }
            courseService.ensureTeacherCanCreateCourse(teacher);

            // Validate preface - BẮT BUỘC phải có
            String trimmedPreface = preface != null ? preface.trim() : "";
            if (trimmedPreface.isEmpty()) {
                bindingResult.rejectValue("preface", "NotBlank", "Lời nói đầu không được để trống");
            }
            
            // Validate checkbox cam kết - BẮT BUỘC phải tick
            if (agreeTerms == null || !agreeTerms.equals("on")) {
                model.addAttribute("termsError", "Bạn phải đồng ý với điều khoản để tạo khóa học");
            }
            
            // Lấy lại điều khoản và phí nền tảng để hiển thị lại form nếu có lỗi
            String teacherTerms = systemConfigRepository.findByConfigKey("TEACHER_TERMS")
                    .map(com.example.doan10diem.model.SystemConfig::getConfigValue)
                    .orElse("1. Phí nền tảng là 20% doanh thu.\n2. Giảng viên cam kết nội dung không vi phạm bản quyền.\n3. Nghiêm cấm nội dung chống phá nhà nước hoặc vi phạm pháp luật.");
            
            Integer platformFeePercent = systemConfigRepository.findByConfigKey("PLATFORM_FEE")
                    .map(config -> {
                        try {
                            return Integer.parseInt(config.getConfigValue());
                        } catch (NumberFormatException e) {
                            return 20;
                        }
                    })
                    .orElse(20);
            
            if (bindingResult.hasErrors() || trimmedPreface.isEmpty() || agreeTerms == null || !agreeTerms.equals("on")) {
                model.addAttribute("mode", "create");
                model.addAttribute("teacherTerms", teacherTerms);
                model.addAttribute("platformFeePercent", platformFeePercent);
                model.addAttribute("preface", trimmedPreface);
                model.addAttribute("rootCategories", categoryRepository.findByParentIsNullOrderByNameAsc());
                return "teacher/course-form";
            }

            // Lấy phí nền tảng từ SystemConfig (KHÔNG lấy từ form)
            Integer platformFee = systemConfigRepository.findByConfigKey("PLATFORM_FEE")
                    .map(config -> {
                        try {
                            return Integer.parseInt(config.getConfigValue());
                        } catch (NumberFormatException e) {
                            return 20; // Default fallback
                        }
                    })
                    .orElse(20);

            Category category = null;
            if (categoryId != null && categoryId > 0) {
                category = categoryRepository.findById(categoryId).orElse(null);
            }

            Course course = Course.builder()
                    .title(formCourse.getTitle())
                    .description(formCourse.getDescription())
                    .teacher(teacher)
                    .category(category)
                    .status(CourseStatus.PENDING_REVIEW) // Đổi từ DRAFT sang PENDING_REVIEW
                    .price(formCourse.getPrice())
                    .platformFeePercent(platformFee) // Lấy từ SystemConfig, không từ form
                    .preface(trimmedPreface)
                    .createdAt(LocalDateTime.now())
                    .build();

            courseRepository.save(course);

            // Xử lý upload ảnh đại diện
            if (imageFile != null && !imageFile.isEmpty()) {
                try {
                    String imageUrl = FileUploadUtil.saveFile(courseImagesDir, course.getId().toString(), null, imageFile);
                    if (imageUrl != null) {
                        course.setImageUrl(imageUrl);
                        courseRepository.save(course);
                    }
                } catch (IOException e) {
                    // Log lỗi nhưng không chặn việc tạo khóa học
                    // Có thể thêm thông báo lỗi vào model nếu cần
                }
            }

            // Tự động tạo Lesson đầu tiên (Bài học 0) từ preface
            Lesson introLesson = Lesson.builder()
                    .course(course)
                    .title("Lời mở đầu / Giới thiệu khóa học")
                    .content(trimmedPreface)
                    .sortOrder(0) // Luôn nằm đầu tiên
                    .type(LessonType.DOCUMENT) // Loại tài liệu
                    .build();
            lessonRepository.save(introLesson);

            return "redirect:/teacher/courses";
        }

        @GetMapping("/teacher/courses/{id}/edit")
        public String showEditCourseForm(@PathVariable Long id,
                                        Authentication authentication,
                                        Model model) {
            User teacher = currentUser(authentication);
            Course course = courseRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Khóa học không tồn tại"));
            ensureOwner(teacher, course);

            model.addAttribute("course", course);
            model.addAttribute("mode", "edit");
            model.addAttribute("rootCategories", categoryRepository.findByParentIsNullOrderByNameAsc());
            return "teacher/course-form";
        }

        @PostMapping("/teacher/courses/{id}")
        public String updateCourse(@PathVariable Long id,
                                Authentication authentication,
                                @Valid @ModelAttribute("course") Course formCourse,
                                BindingResult bindingResult,
                                @RequestParam(value = "categoryId", required = false) Long categoryId,
                                @RequestParam(value = "image", required = false) MultipartFile imageFile,
                                Model model) {
            User teacher = currentUser(authentication);
            Course course = courseRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Khóa học không tồn tại"));
            ensureOwner(teacher, course);

            if (bindingResult.hasErrors()) {
                model.addAttribute("mode", "edit");
                model.addAttribute("rootCategories", categoryRepository.findByParentIsNullOrderByNameAsc());
                return "teacher/course-form";
            }

            Category category = null;
            if (categoryId != null && categoryId > 0) {
                category = categoryRepository.findById(categoryId).orElse(null);
            }

            // Chỉ cho phép sửa thông tin cơ bản, không cho sửa teacher/status trực tiếp
            course.setTitle(formCourse.getTitle());
            course.setDescription(formCourse.getDescription());
            course.setPrice(formCourse.getPrice());
            course.setPlatformFeePercent(formCourse.getPlatformFeePercent());
            course.setCategory(category);

            // Xử lý upload ảnh đại diện (nếu có)
            if (imageFile != null && !imageFile.isEmpty()) {
                try {
                    String imageUrl = FileUploadUtil.saveFile(courseImagesDir, course.getId().toString(), null, imageFile);
                    if (imageUrl != null) {
                        course.setImageUrl(imageUrl);
                    }
                } catch (IOException e) {
                    // Log lỗi nhưng không chặn việc cập nhật khóa học
                }
            }

            courseRepository.save(course);
            return "redirect:/teacher/courses";
        }

        @PostMapping("/teacher/courses/{id}/submit-review")
        public String submitCourseForReview(@PathVariable Long id,
                                            Authentication authentication) {
            User teacher = currentUser(authentication);
            Course course = courseRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Khóa học không tồn tại"));
            ensureOwner(teacher, course);

            // Chỉ cho phép gửi duyệt từ DRAFT hoặc REJECTED
            if (course.getStatus() == CourseStatus.DRAFT || course.getStatus() == CourseStatus.REJECTED) {
                course.setStatus(CourseStatus.PENDING_REVIEW);
                courseRepository.save(course);
            }
            return "redirect:/teacher/courses";
        }

        @PostMapping("/teacher/courses/{id}/delete")
        public String deleteCourse(@PathVariable Long id,
                                Authentication authentication) {
            User teacher = currentUser(authentication);
            Course course = courseRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Khóa học không tồn tại"));
            ensureOwner(teacher, course);

            // Không cho xóa khóa học đã publish để tránh mất dữ liệu thanh toán
            if (course.getStatus() != CourseStatus.PUBLISHED) {
                courseRepository.delete(course);
            }

            return "redirect:/teacher/courses";
        }

        /**
         * Danh sách assignment của một khóa học.
         */
        @GetMapping("/teacher/courses/{courseId}/assignments")
        @org.springframework.transaction.annotation.Transactional(readOnly = true)
        public String listAssignments(@PathVariable Long courseId,
                                    Authentication authentication,
                                    Model model,
                                    @org.springframework.web.bind.annotation.RequestParam(required = false) String error) {
            User teacher = currentUser(authentication);
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("Khóa học không tồn tại"));
            ensureOwner(teacher, course);

            // Force load course title
            String courseTitle = course.getTitle();

            List<Assignment> assignments = assignmentRepository.findByCourse(course);
            // Force load submissions để tránh lazy loading
            assignments.forEach(a -> {
                if (a.getSubmissions() != null) {
                    a.getSubmissions().size();
                }
            });

            model.addAttribute("course", course);
            model.addAttribute("assignments", assignments);
            if (error != null) {
                model.addAttribute("error", "Có lỗi xảy ra khi tạo bài tập. Vui lòng thử lại.");
            }
            return "teacher/assignments";
        }

        @GetMapping("/teacher/courses/{courseId}/assignments/new")
        public String showCreateAssignmentForm(@PathVariable Long courseId,
                                            Authentication authentication,
                                            Model model) {
            User teacher = currentUser(authentication);
            // Sử dụng service để load course với teacher đã được initialize
            Course course = courseService.findByIdWithTeacherInitialized(courseId);
            ensureOwner(teacher, course);

            // Đảm bảo tất cả fields của course được access để tránh lazy loading
            course.getId();
            course.getTitle();
            if (course.getTeacher() != null) {
                course.getTeacher().getId();
                course.getTeacher().getFullName();
            }

            Assignment assignment = new Assignment();
            assignment.setCourse(course);
            model.addAttribute("course", course);
            model.addAttribute("assignment", assignment);
            model.addAttribute("mode", "create");
            model.addAttribute("dueDateFormatted", ""); // Empty for new assignment
            return "teacher/assignment-form";
        }

        @PostMapping("/teacher/courses/{courseId}/assignments")
        public String createAssignment(@PathVariable Long courseId,
                                    Authentication authentication,
                                    @RequestParam(value = "title", required = false) String title,
                                    @RequestParam(value = "description", required = false) String description,
                                    @RequestParam(value = "correctAnswer", required = false) String correctAnswer,
                                    @RequestParam(value = "dueDate", required = false) String dueDateStr,
                                    Model model) {
            try {
                User teacher = currentUser(authentication);
                Course course = courseRepository.findById(courseId)
                        .orElseThrow(() -> new IllegalArgumentException("Khóa học không tồn tại"));
                ensureOwner(teacher, course);

                // Đảm bảo course được load đầy đủ
                course = courseService.findByIdWithTeacherInitialized(courseId);
                course.getId();
                course.getTitle();
                if (course.getTeacher() != null) {
                    course.getTeacher().getId();
                    course.getTeacher().getFullName();
                }

                // Validation thủ công
                if (title == null || title.trim().isEmpty()) {
                    model.addAttribute("course", course);
                    model.addAttribute("mode", "create");
                    Assignment assignment = new Assignment();
                    assignment.setCourse(course);
                    if (description != null) {
                        assignment.setDescription(description);
                    }
                    model.addAttribute("assignment", assignment);
                    model.addAttribute("dueDateFormatted", dueDateStr != null ? dueDateStr : "");
                    model.addAttribute("error", "Tiêu đề bài tập không được để trống");
                    return "teacher/assignment-form";
                }

                // Parse dueDate nếu có - datetime-local format: "yyyy-MM-ddTHH:mm"
                LocalDateTime dueDate = null;
                if (dueDateStr != null && !dueDateStr.trim().isEmpty()) {
                    try {
                        // datetime-local gửi format: "2026-01-25T16:30"
                        dueDate = LocalDateTime.parse(dueDateStr);
                    } catch (Exception e) {
                        // Nếu parse lỗi, thử format khác
                        try {
                            dueDate = LocalDateTime.parse(dueDateStr.replace(" ", "T"));
                        } catch (Exception e2) {
                            // Ignore parse error, dueDate sẽ là null
                        }
                    }
                }

                // Tạo Assignment mới
                Assignment assignment = Assignment.builder()
                        .course(course)
                        .title(title.trim())
                        .description(description != null && !description.trim().isEmpty() ? description.trim() : null)
                        .correctAnswer(correctAnswer != null && !correctAnswer.trim().isEmpty() ? correctAnswer.trim() : null)
                        .dueDate(dueDate)
                        .build();

                assignmentRepository.save(assignment);
                return "redirect:/teacher/courses/" + courseId + "/assignments";
            } catch (Exception e) {
                // Log error và redirect về danh sách với error message
                return "redirect:/teacher/courses/" + courseId + "/assignments?error=create_failed";
            }
        }

        @GetMapping("/teacher/assignments/{id}/edit")
        public String showEditAssignmentForm(@PathVariable Long id,
                                            Authentication authentication,
                                            Model model) {
            User teacher = currentUser(authentication);
            Assignment assignment = assignmentRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Bài tập không tồn tại"));
            Course course = assignment.getCourse();
            ensureOwner(teacher, course);

            // Đảm bảo course và teacher được load đầy đủ
            if (course != null) {
                course = courseService.findByIdWithTeacherInitialized(course.getId());
                course.getId();
                course.getTitle();
                if (course.getTeacher() != null) {
                    course.getTeacher().getId();
                    course.getTeacher().getFullName();
                }
            }

            // Format dueDate for datetime-local input
            String dueDateFormatted = "";
            if (assignment.getDueDate() != null) {
                dueDateFormatted = assignment.getDueDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            }
            
            model.addAttribute("course", course);
            model.addAttribute("assignment", assignment);
            model.addAttribute("mode", "edit");
            model.addAttribute("dueDateFormatted", dueDateFormatted);
            return "teacher/assignment-form";
        }

        @PostMapping("/teacher/assignments/{id}")
        public String updateAssignment(@PathVariable Long id,
                                    Authentication authentication,
                                    @Valid @ModelAttribute("assignment") Assignment formAssignment,
                                    BindingResult bindingResult,
                                    Model model) {
            User teacher = currentUser(authentication);
            Assignment assignment = assignmentRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Bài tập không tồn tại"));
            ensureOwner(teacher, assignment.getCourse());

            if (bindingResult.hasErrors()) {
                Course course = assignment.getCourse();
                // Đảm bảo course được load đầy đủ
                if (course != null) {
                    course = courseService.findByIdWithTeacherInitialized(course.getId());
                }
                // Format dueDate for datetime-local input
                String dueDateFormatted = "";
                if (assignment.getDueDate() != null) {
                    dueDateFormatted = assignment.getDueDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
                }
                model.addAttribute("course", course);
                model.addAttribute("assignment", assignment);
                model.addAttribute("mode", "edit");
                model.addAttribute("dueDateFormatted", dueDateFormatted);
                return "teacher/assignment-form";
            }

            assignment.setTitle(formAssignment.getTitle());
            assignment.setDescription(formAssignment.getDescription());
            assignment.setCorrectAnswer(formAssignment.getCorrectAnswer());
            assignment.setDueDate(formAssignment.getDueDate());
            assignmentRepository.save(assignment);

            return "redirect:/teacher/courses/" + assignment.getCourse().getId() + "/assignments";
        }

        @PostMapping("/teacher/assignments/{id}/delete")
        public String deleteAssignment(@PathVariable Long id,
                                    Authentication authentication) {
            User teacher = currentUser(authentication);
            Assignment assignment = assignmentRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Bài tập không tồn tại"));
            ensureOwner(teacher, assignment.getCourse());

            Long courseId = assignment.getCourse().getId();
            assignmentRepository.delete(assignment);
            return "redirect:/teacher/courses/" + courseId + "/assignments";
        }

        /**
         * Danh sách bài nộp của một bài tập – hỗ trợ tìm kiếm, lọc trạng thái, sắp xếp.
         */
        @GetMapping("/teacher/assignments/{assignmentId}/submissions")
        public String listSubmissions(@PathVariable Long assignmentId,
                                    Authentication authentication,
                                    @RequestParam(value = "keyword", required = false) String keyword,
                                    @RequestParam(value = "status", required = false) String status,
                                    @RequestParam(value = "sort", required = false) String sort,
                                    Model model) {
            User teacher = currentUser(authentication);
            Assignment assignment = assignmentRepository.findById(assignmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Bài tập không tồn tại"));
            ensureOwner(teacher, assignment.getCourse());

            List<Submission> submissions = submissionRepository.findByAssignment(assignment);

            // Lọc theo tên sinh viên
            if (keyword != null && !keyword.isBlank()) {
                String k = keyword.trim().toLowerCase();
                submissions = submissions.stream()
                        .filter(s -> s.getStudent().getFullName().toLowerCase().contains(k)
                                || (s.getStudent().getEmail() != null && s.getStudent().getEmail().toLowerCase().contains(k)))
                        .toList();
            }

            // Lọc theo trạng thái: ALL, GRADED, UNGRADED, AI_GRADED
            if (status != null && !status.isBlank()) {
                switch (status.toUpperCase()) {
                    case "GRADED" -> submissions = submissions.stream().filter(s -> s.getGrade() != null).toList();
                    case "UNGRADED" -> submissions = submissions.stream().filter(s -> s.getGrade() == null).toList();
                    case "AI_GRADED" -> submissions = submissions.stream().filter(s -> s.getAiGrade() != null).toList();
                    default -> { }
                }
            }

            // Sắp xếp: newest, oldest, score_high, score_low
            final String sortBy = sort != null ? sort : "newest";
            submissions = submissions.stream().sorted((a, b) -> {
                switch (sortBy.toLowerCase()) {
                    case "oldest":
                        return (a.getSubmittedAt() == null ? java.time.LocalDateTime.MIN : a.getSubmittedAt())
                                .compareTo(b.getSubmittedAt() == null ? java.time.LocalDateTime.MIN : b.getSubmittedAt());
                    case "score_high": {
                        double ga = a.getGrade() != null ? a.getGrade() : -1;
                        double gb = b.getGrade() != null ? b.getGrade() : -1;
                        return Double.compare(gb, ga);
                    }
                    case "score_low": {
                        double ga = a.getGrade() != null ? a.getGrade() : 999;
                        double gb = b.getGrade() != null ? b.getGrade() : 999;
                        return Double.compare(ga, gb);
                    }
                    default: // newest
                        return (b.getSubmittedAt() == null ? java.time.LocalDateTime.MIN : b.getSubmittedAt())
                                .compareTo(a.getSubmittedAt() == null ? java.time.LocalDateTime.MIN : a.getSubmittedAt());
                }
            }).toList();

            long gradedCount = submissions.stream().filter(s -> s.getGrade() != null).count();
            double avgScore = submissions.stream()
                    .filter(s -> s.getGrade() != null)
                    .mapToDouble(Submission::getGrade)
                    .average()
                    .orElse(0);

            model.addAttribute("assignment", assignment);
            model.addAttribute("submissions", submissions);
            model.addAttribute("keyword", keyword);
            model.addAttribute("status", status);
            model.addAttribute("sort", sort);
            model.addAttribute("totalCount", submissions.size());
            model.addAttribute("gradedCount", gradedCount);
            model.addAttribute("avgScore", Math.round(avgScore * 10) / 10.0);
            return "teacher/submissions";
        }

        @GetMapping("/teacher/submissions/{submissionId}")
        public String showSubmissionDetail(@PathVariable Long submissionId,
                                        Authentication authentication,
                                        Model model) {
            User teacher = currentUser(authentication);
            Submission submission = submissionRepository.findById(submissionId)
                    .orElseThrow(() -> new IllegalArgumentException("Bài nộp không tồn tại"));
            ensureOwner(teacher, submission.getAssignment().getCourse());

            model.addAttribute("submission", submission);
            return "teacher/submission-detail";
        }

    @PostMapping("/teacher/submissions/{submissionId}/grade")
    public String gradeSubmission(@PathVariable Long submissionId,
                                  Authentication authentication,
                                  Double grade,
                                  String teacherFeedback,
                                  org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
            User teacher = currentUser(authentication);
            Submission submission = submissionRepository.findById(submissionId)
                    .orElseThrow(() -> new IllegalArgumentException("Bài nộp không tồn tại"));
            ensureOwner(teacher, submission.getAssignment().getCourse());

            submission.setGrade(grade);
            submission.setTeacherFeedback(teacherFeedback);
            submission.setGradedAt(LocalDateTime.now());
            submission.setGradedBy(teacher);

        submissionRepository.save(submission);

        redirectAttributes.addFlashAttribute("message", "Chấm điểm thành công!");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/teacher/assignments/" + submission.getAssignment().getId() + "/submissions";
        }

        /**
         * Trang tổng quan quản lý tất cả bài nộp của teacher.
         * filter: "all" | "ungraded_only" – chỉ hiện bài tập còn bài chưa chấm.
         * sort: "newest" | "most_ungraded" – sắp xếp theo mới nhất hoặc nhiều bài chưa chấm nhất.
         */
        @GetMapping("/teacher/all-submissions")
        public String allSubmissions(
                Authentication authentication,
                Model model,
                @RequestParam(required = false, defaultValue = "all") String filter,
                @RequestParam(required = false, defaultValue = "newest") String sort) {
            User teacher = currentUser(authentication);
            List<Course> courses = courseRepository.findByTeacher(teacher);

            List<Assignment> allAssignments = new java.util.ArrayList<>();
            for (Course course : courses) {
                List<Assignment> assignments = assignmentRepository.findByCourse(course);
                allAssignments.addAll(assignments);
            }

            java.util.List<java.util.Map<String, Object>> assignmentInfoList = new java.util.ArrayList<>();
            int totalSubmissions = 0;
            int ungradedSubmissions = 0;

            for (Assignment assignment : allAssignments) {
                List<Submission> submissions = assignment.getSubmissions().stream().toList();
                long ungradedCount = submissions.stream().filter(s -> s.getGrade() == null).count();

                java.util.Map<String, Object> info = new java.util.HashMap<>();
                info.put("assignment", assignment);
                info.put("submissionCount", submissions.size());
                info.put("ungradedCount", ungradedCount);
                assignmentInfoList.add(info);
            }

            // Lọc: chỉ giữ bài tập có ít nhất 1 bài chưa chấm
            if ("ungraded_only".equalsIgnoreCase(filter)) {
                assignmentInfoList = assignmentInfoList.stream()
                        .filter(m -> ((Number) m.get("ungradedCount")).longValue() > 0)
                        .toList();
            }

            // Sắp xếp
            if ("most_ungraded".equalsIgnoreCase(sort)) {
                assignmentInfoList = assignmentInfoList.stream()
                        .sorted((a, b) -> Long.compare(
                                ((Number) b.get("ungradedCount")).longValue(),
                                ((Number) a.get("ungradedCount")).longValue()))
                        .toList();
            } else {
                // newest: theo id assignment giảm dần (id lớn = mới hơn)
                assignmentInfoList = assignmentInfoList.stream()
                        .sorted((a, b) -> Long.compare(
                                ((Assignment) b.get("assignment")).getId(),
                                ((Assignment) a.get("assignment")).getId()))
                        .toList();
            }

            for (java.util.Map<String, Object> info : assignmentInfoList) {
                totalSubmissions += ((Number) info.get("submissionCount")).intValue();
                ungradedSubmissions += ((Number) info.get("ungradedCount")).longValue();
            }

            model.addAttribute("assignmentInfoList", assignmentInfoList);
            model.addAttribute("totalSubmissions", totalSubmissions);
            model.addAttribute("ungradedSubmissions", ungradedSubmissions);
            model.addAttribute("totalAssignments", assignmentInfoList.size());
            model.addAttribute("filter", filter);
            model.addAttribute("sort", sort);
            return "teacher/all-submissions";
        }

        /**
         * ========== CRUD LESSONS ==========
         */
        @GetMapping("/teacher/courses/{courseId}/lessons")
        public String listLessons(@PathVariable Long courseId,
                                Authentication authentication,
                                Model model) {
            User teacher = currentUser(authentication);
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("Khóa học không tồn tại"));
            ensureOwner(teacher, course);

            List<Lesson> lessons = lessonRepository.findByCourseOrderBySortOrderAsc(course);
            model.addAttribute("course", course);
            model.addAttribute("lessons", lessons);
            return "teacher/lessons";
        }

        @GetMapping("/teacher/courses/{courseId}/lessons/new")
        public String showCreateLessonForm(@PathVariable Long courseId,
                                        Authentication authentication,
                                        Model model) {
            User teacher = currentUser(authentication);
            // Sử dụng service để load course với teacher đã được initialize
            Course course = courseService.findByIdWithTeacherInitialized(courseId);
            ensureOwner(teacher, course);

            // Đảm bảo tất cả fields của course được access để tránh lazy loading
            course.getId();
            course.getTitle();
            if (course.getTeacher() != null) {
                course.getTeacher().getId();
                course.getTeacher().getFullName();
            }

            Lesson lesson = new Lesson();
            lesson.setCourse(course);
            model.addAttribute("course", course);
            model.addAttribute("lesson", lesson);
            model.addAttribute("mode", "create");
            return "teacher/lesson-form";
        }

        @PostMapping("/teacher/courses/{courseId}/lessons")
        public String createLesson(@PathVariable Long courseId,
                                Authentication authentication,
                                @Valid @ModelAttribute("lesson") Lesson formLesson,
                                BindingResult bindingResult,
                                @RequestParam(value = "file", required = false) MultipartFile file,
                                Model model) {
            User teacher = currentUser(authentication);
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("Khóa học không tồn tại"));
            ensureOwner(teacher, course);

            if (bindingResult.hasErrors()) {
                // Đảm bảo course được load đầy đủ
                course = courseService.findByIdWithTeacherInitialized(courseId);
                course.getId();
                course.getTitle();
                if (course.getTeacher() != null) {
                    course.getTeacher().getId();
                    course.getTeacher().getFullName();
                }
                model.addAttribute("course", course);
                model.addAttribute("lesson", formLesson);
                model.addAttribute("mode", "create");
                return "teacher/lesson-form";
            }

            // Xử lý file upload
            String resourceUrl = formLesson.getResourceUrl();
            try {
                if (file != null && !file.isEmpty()) {
                    String savedFilePath = saveUploadedFile(file, courseId);
                    if (savedFilePath != null) {
                        resourceUrl = savedFilePath;
                    }
                }
            } catch (IOException e) {
                model.addAttribute("error", "Lỗi khi upload file: " + e.getMessage());
                course = courseService.findByIdWithTeacherInitialized(courseId);
                course.getId();
                course.getTitle();
                if (course.getTeacher() != null) {
                    course.getTeacher().getId();
                    course.getTeacher().getFullName();
                }
                model.addAttribute("course", course);
                model.addAttribute("lesson", formLesson);
                model.addAttribute("mode", "create");
                return "teacher/lesson-form";
            }

            // Tạo Lesson mới thay vì dùng formLesson trực tiếp để tránh vấn đề binding
            Lesson lesson = Lesson.builder()
                    .course(course)
                    .title(formLesson.getTitle())
                    .content(formLesson.getContent())
                    .resourceUrl(resourceUrl)
                    .type(formLesson.getType())
                    .sortOrder(formLesson.getSortOrder())
                    .build();

            // Nếu chưa có sortOrder, set mặc định
            if (lesson.getSortOrder() == null) {
                List<Lesson> existingLessons = lessonRepository.findByCourseOrderBySortOrderAsc(course);
                int maxOrder = existingLessons.stream()
                        .mapToInt(l -> l.getSortOrder() != null ? l.getSortOrder() : 0)
                        .max()
                        .orElse(0);
                lesson.setSortOrder(maxOrder + 1);
            }

            lessonRepository.save(lesson);
            notificationService.notifyCourseUpdate(course);
            return "redirect:/teacher/courses/" + courseId + "/lessons";
        }

        @GetMapping("/teacher/lessons/{id}/edit")
        public String showEditLessonForm(@PathVariable Long id,
                                        Authentication authentication,
                                        Model model) {
            User teacher = currentUser(authentication);
            Lesson lesson = lessonRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Bài học không tồn tại"));
            
            // Đảm bảo lesson.id được access
            lesson.getId();
            lesson.getTitle();
            
            Course course = lesson.getCourse();
            ensureOwner(teacher, course);

            // Đảm bảo course và teacher được load đầy đủ
            if (course != null) {
                course = courseService.findByIdWithTeacherInitialized(course.getId());
                course.getId();
                course.getTitle();
                if (course.getTeacher() != null) {
                    course.getTeacher().getId();
                    course.getTeacher().getFullName();
                }
            }

            model.addAttribute("course", course);
            model.addAttribute("lesson", lesson);
            model.addAttribute("mode", "edit");
            return "teacher/lesson-form";
        }

        @PostMapping("/teacher/lessons/{id}")
        public String updateLesson(@PathVariable Long id,
                                Authentication authentication,
                                @Valid @ModelAttribute("lesson") Lesson formLesson,
                                BindingResult bindingResult,
                                @RequestParam(value = "file", required = false) MultipartFile file,
                                Model model) {
            User teacher = currentUser(authentication);
            Lesson lesson = lessonRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Bài học không tồn tại"));
            ensureOwner(teacher, lesson.getCourse());

            if (bindingResult.hasErrors()) {
                Course course = lesson.getCourse();
                // Đảm bảo course được load đầy đủ
                if (course != null) {
                    course = courseService.findByIdWithTeacherInitialized(course.getId());
                    course.getId();
                    course.getTitle();
                    if (course.getTeacher() != null) {
                        course.getTeacher().getId();
                        course.getTeacher().getFullName();
                    }
                }
                // Đảm bảo lesson.id được access
                lesson.getId();
                lesson.getTitle();
                model.addAttribute("course", course);
                model.addAttribute("lesson", formLesson);
                model.addAttribute("mode", "edit");
                return "teacher/lesson-form";
            }

            Course course = lesson.getCourse();
            // Xử lý file upload
            String resourceUrl = formLesson.getResourceUrl();
            try {
                if (file != null && !file.isEmpty()) {
                    String savedFilePath = saveUploadedFile(file, course.getId());
                    if (savedFilePath != null) {
                        resourceUrl = savedFilePath;
                    }
                }
            } catch (IOException e) {
                model.addAttribute("error", "Lỗi khi upload file: " + e.getMessage());
                if (course != null) {
                    course = courseService.findByIdWithTeacherInitialized(course.getId());
                    course.getId();
                    course.getTitle();
                    if (course.getTeacher() != null) {
                        course.getTeacher().getId();
                        course.getTeacher().getFullName();
                    }
                }
                lesson.getId();
                lesson.getTitle();
                model.addAttribute("course", course);
                model.addAttribute("lesson", formLesson);
                model.addAttribute("mode", "edit");
                return "teacher/lesson-form";
            }

            lesson.setTitle(formLesson.getTitle());
            lesson.setContent(formLesson.getContent());
            lesson.setResourceUrl(resourceUrl);
            lesson.setType(formLesson.getType());
            lesson.setSortOrder(formLesson.getSortOrder());
            lessonRepository.save(lesson);
            notificationService.notifyCourseUpdate(lesson.getCourse());

            return "redirect:/teacher/courses/" + lesson.getCourse().getId() + "/lessons";
        }

        @PostMapping("/teacher/lessons/{id}/delete")
        public String deleteLesson(@PathVariable Long id,
                                Authentication authentication) {
            User teacher = currentUser(authentication);
            Lesson lesson = lessonRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Bài học không tồn tại"));
            ensureOwner(teacher, lesson.getCourse());

            Long courseId = lesson.getCourse().getId();
            lessonRepository.delete(lesson);
            return "redirect:/teacher/courses/" + courseId + "/lessons";
        }

        private User currentUser(Authentication authentication) {
            String username = authentication.getName();
            return userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy user"));
        }

        private void ensureOwner(User teacher, Course course) {
            if (course.getTeacher() == null || !course.getTeacher().getId().equals(teacher.getId())) {
                throw new AccessDeniedException("Bạn không có quyền thao tác với khóa học này");
            }
        }
    }
