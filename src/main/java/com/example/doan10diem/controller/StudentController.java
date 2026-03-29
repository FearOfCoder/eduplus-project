package com.example.doan10diem.controller;

import com.example.doan10diem.model.Assignment;
import com.example.doan10diem.model.Category;
import com.example.doan10diem.model.Course;
import com.example.doan10diem.model.CourseStatus;
import com.example.doan10diem.model.Enrollment;
import com.example.doan10diem.model.Lesson;
import com.example.doan10diem.model.LessonProgress;
import com.example.doan10diem.model.Payment;
import com.example.doan10diem.model.Submission;
import com.example.doan10diem.model.User;
import com.example.doan10diem.repository.AssignmentRepository;
import com.example.doan10diem.repository.CategoryRepository;
import com.example.doan10diem.repository.CourseRepository;
import com.example.doan10diem.repository.CourseSpecifications;
import com.example.doan10diem.repository.EnrollmentRepository;
import com.example.doan10diem.repository.LessonProgressRepository;
import com.example.doan10diem.repository.LessonRepository;
import com.example.doan10diem.repository.PaymentRepository;
import com.example.doan10diem.repository.SubmissionRepository;
import com.example.doan10diem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@Controller
@RequiredArgsConstructor
@Slf4j
public class StudentController {

    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;

    @GetMapping("/student/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        User student = currentUser(authentication);
        List<Enrollment> enrollments = enrollmentRepository.findByStudent(student);
        model.addAttribute("student", student);
        model.addAttribute("enrollments", enrollments);
        return "student/dashboard";
    }

    @GetMapping("/courses")
    public String listCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "sort", required = false) String sort,
            Model model) {
        Specification<Course> spec = Specification.where(CourseSpecifications.statusIs(CourseStatus.PUBLISHED))
                .and(CourseSpecifications.keywordInTitleOrDescription(keyword))
                .and(CourseSpecifications.priceBetween(minPrice, maxPrice));

        List<Category> categoriesFilter = null;
        if (categoryId != null && categoryId > 0) {
            Category category = categoryRepository.findById(categoryId).orElse(null);
            if (category != null) {
                if (category.getParent() == null) {
                    List<Category> list = new ArrayList<>();
                    list.add(category);
                    list.addAll(category.getChildren());
                    categoriesFilter = list;
                } else {
                    categoriesFilter = List.of(category);
                }
            }
        }
        spec = spec.and(CourseSpecifications.categoryIn(categoriesFilter));

        Sort sortObj = Sort.by("createdAt").descending();
        if (sort != null) {
            switch (sort) {
                case "price_asc" -> sortObj = Sort.by("price").ascending();
                case "price_desc" -> sortObj = Sort.by("price").descending();
                case "newest" -> sortObj = Sort.by("createdAt").descending();
                default -> {}
            }
        }

        Pageable pageable = PageRequest.of(page, 6, sortObj);
        Page<Course> coursePage = courseRepository.findAll(spec, pageable);

        model.addAttribute("coursePage", coursePage);
        model.addAttribute("currentPage", page);
        model.addAttribute("rootCategories", categoryRepository.findByParentIsNullOrderByNameAsc());
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("sort", sort);
        return "student/store";
    }

    /** Chuyển về trang giới thiệu khóa học (Sales Page) công khai. */
    @GetMapping("/courses/{id}")
    public String courseDetail(@PathVariable Long id) {
        return "redirect:/course/" + id + "/intro";
    }

    /**
     * Redirect từ URL số ít sang số nhiều để tránh lỗi.
     */
    @GetMapping("/student/my-course")
    public String redirectMyCourse() {
        return "redirect:/student/my-courses";
    }

    /**
     * Danh sách các khóa học mà student đã mua.
     */
    @GetMapping("/student/my-courses")
    public String myCourses(Authentication authentication, Model model) {
        User student = currentUser(authentication);
        List<Enrollment> enrollments = enrollmentRepository.findByStudent(student);
        model.addAttribute("student", student);
        model.addAttribute("enrollments", enrollments);
        return "student/my-courses";
    }

    /**
     * Chi tiết khóa học với kiểm tra enrollment.
     * Hiển thị danh sách lessons và assignments.
     */
    @GetMapping("/student/course/{id}")
    public String studentCourseDetail(@PathVariable Long id,
                                     Authentication authentication,
                                     @RequestParam(value = "lessonId", required = false) Long lessonId,
                                     @RequestParam(value = "assignmentId", required = false) Long assignmentId,
                                     Model model) {
        User student = currentUser(authentication);
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khóa học không tồn tại"));

        // Kiểm tra xem student đã mua khóa học chưa
        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findByCourseAndStudent(course, student);
        if (enrollmentOpt.isEmpty()) {
            // Nếu chưa mua -> redirect về trang giới thiệu
            return "redirect:/courses/" + id;
        }

        Enrollment enrollment = enrollmentOpt.get();

        // Load danh sách lessons và assignments
        List<Lesson> lessons = lessonRepository.findByCourseOrderBySortOrderAsc(course);
        List<Assignment> assignments = assignmentRepository.findByCourse(course);

        // Load danh sách submissions của student trong khóa học này
        List<Submission> allSubmissions = submissionRepository.findByStudent(student);
        java.util.Map<Long, Submission> submissionMap = new java.util.HashMap<>();
        for (Submission submission : allSubmissions) {
            // Chỉ lấy submissions thuộc khóa học hiện tại
            if (submission.getAssignment().getCourse().getId().equals(course.getId())) {
                submissionMap.put(submission.getAssignment().getId(), submission);
            }
        }

        // Tính tiến độ học tập: số bài đã hoàn thành / tổng số bài
        long completedLessonsCount = lessonProgressRepository.countByStudentAndLessonCourseAndIsCompletedTrue(student, course);
        int totalLessons = lessons.size();
        int progressPercentage = totalLessons > 0 
                ? (int) Math.round((completedLessonsCount * 100.0) / totalLessons) 
                : 0;

        // Cập nhật progressPercent trong Enrollment
        enrollment.setProgressPercent(progressPercentage);
        if (progressPercentage == 100 && totalLessons > 0) {
            enrollment.setCompleted(true);
        }
        enrollmentRepository.save(enrollment);

        // Load trạng thái hoàn thành của từng bài học
        java.util.Map<Long, Boolean> lessonCompletedMap = new java.util.HashMap<>();
        for (Lesson lesson : lessons) {
            Optional<LessonProgress> progressOpt = lessonProgressRepository.findByStudentAndLesson(student, lesson);
            lessonCompletedMap.put(lesson.getId(), progressOpt.map(LessonProgress::isCompleted).orElse(false));
        }

        // Kiểm tra trạng thái bài học hiện tại (nếu có)
        Boolean currentLessonCompleted = null;
        Lesson currentLesson = null;
        Long nextLessonId = null;
        if (lessonId != null) {
            currentLesson = lessons.stream()
                    .filter(l -> l.getId().equals(lessonId))
                    .findFirst()
                    .orElse(null);
            if (currentLesson != null) {
                currentLessonCompleted = lessonCompletedMap.get(lessonId);
                
                // Tìm bài học tiếp theo
                for (int i = 0; i < lessons.size(); i++) {
                    if (lessons.get(i).getId().equals(lessonId) && i < lessons.size() - 1) {
                        nextLessonId = lessons.get(i + 1).getId();
                        break;
                    }
                }
            }
        }

        model.addAttribute("student", student);
        model.addAttribute("course", course);
        model.addAttribute("enrollment", enrollment);
        model.addAttribute("lessons", lessons);
        model.addAttribute("assignments", assignments);
        model.addAttribute("lessonCompletedMap", lessonCompletedMap);
        model.addAttribute("submissionMap", submissionMap);
        model.addAttribute("currentLesson", currentLesson);
        model.addAttribute("currentLessonCompleted", currentLessonCompleted);
        model.addAttribute("nextLessonId", nextLessonId);
        model.addAttribute("progressPercentage", progressPercentage);
        return "student/course-detail";
    }

    /**
     * Đánh dấu bài học đã hoàn thành và chuyển sang bài tiếp theo.
     */
    @PostMapping("/student/course/{courseId}/lesson/{lessonId}/complete")
    public String completeLesson(@PathVariable Long courseId,
                                 @PathVariable Long lessonId,
                                 Authentication authentication) {
        User student = currentUser(authentication);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Khóa học không tồn tại"));

        // Kiểm tra enrollment
        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findByCourseAndStudent(course, student);
        if (enrollmentOpt.isEmpty()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Bạn chưa đăng ký khóa học này.");
        }

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Bài học không tồn tại"));

        // Kiểm tra lesson thuộc course này
        if (!lesson.getCourse().getId().equals(courseId)) {
            throw new IllegalArgumentException("Bài học không thuộc khóa học này");
        }

        // Tạo hoặc cập nhật LessonProgress
        LessonProgress progress = lessonProgressRepository.findByStudentAndLesson(student, lesson)
                .orElseGet(() -> {
                    LessonProgress newProgress = new LessonProgress();
                    newProgress.setStudent(student);
                    newProgress.setLesson(lesson);
                    return newProgress;
                });

        if (!progress.isCompleted()) {
            progress.setCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
            lessonProgressRepository.save(progress);
        }

        // Tìm bài học tiếp theo
        List<Lesson> allLessons = lessonRepository.findByCourseOrderBySortOrderAsc(course);
        Lesson nextLesson = null;
        for (int i = 0; i < allLessons.size(); i++) {
            if (allLessons.get(i).getId().equals(lessonId) && i < allLessons.size() - 1) {
                nextLesson = allLessons.get(i + 1);
                break;
            }
        }

        // Redirect đến bài tiếp theo hoặc về trang chi tiết khóa học
        if (nextLesson != null) {
            return "redirect:/student/course/" + courseId + "?lessonId=" + nextLesson.getId();
        } else {
            // Đã hoàn thành tất cả bài học
            return "redirect:/student/course/" + courseId + "?completed=true";
        }
    }

    @GetMapping("/student/payments")
    public String paymentHistory(Authentication authentication, Model model) {
        User student = currentUser(authentication);
        List<Payment> payments = paymentRepository.findByStudentOrderByCreatedAtDesc(student);
        model.addAttribute("payments", payments);
        return "student/payments";
    }

    /**
     * Danh sách bài tập của một khóa học.
     */
    @GetMapping("/student/courses/{courseId}/assignments")
    public String listAssignments(@PathVariable Long courseId,
                                  Authentication authentication,
                                  Model model) {
        User student = currentUser(authentication);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Khóa học không tồn tại"));

        // Kiểm tra xem student đã enroll chưa
        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findByCourseAndStudent(course, student);
        if (enrollmentOpt.isEmpty()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Bạn chưa đăng ký khóa học này. Vui lòng mua khóa học trước khi xem bài tập.");
        }

        List<Assignment> assignments = assignmentRepository.findByCourse(course);
        model.addAttribute("student", student);
        model.addAttribute("course", course);
        model.addAttribute("assignments", assignments);
        return "student/assignments";
    }

    /**
     * Xem chi tiết 1 bài tập và trạng thái nộp/chấm điểm.
     */
    @GetMapping("/student/assignments/{assignmentId}")
    public String assignmentDetail(@PathVariable Long assignmentId,
                                   Authentication authentication,
                                   Model model) {
        User student = currentUser(authentication);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Bài tập không tồn tại"));

        Course course = assignment.getCourse();
        // Kiểm tra xem student đã enroll chưa
        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findByCourseAndStudent(course, student);
        if (enrollmentOpt.isEmpty()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Bạn chưa đăng ký khóa học này. Vui lòng mua khóa học trước khi xem bài tập.");
        }

        Optional<Submission> submissionOpt = submissionRepository.findByAssignmentAndStudent(assignment, student);

        model.addAttribute("student", student);
        model.addAttribute("assignment", assignment);
        model.addAttribute("submission", submissionOpt.orElse(null));
        return "student/assignment-detail";
    }

    /**
     * Nộp bài hoặc cập nhật lại bài nộp.
     */
    @PostMapping("/student/assignments/{assignmentId}/submit")
    public String submitAssignment(@PathVariable Long assignmentId,
                                   Authentication authentication,
                                   @RequestParam("studentAnswer") String studentAnswer,
                                   RedirectAttributes redirectAttributes) {
        User student = currentUser(authentication);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Bài tập không tồn tại"));

        Course course = assignment.getCourse();
        // Kiểm tra xem student đã enroll chưa
        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findByCourseAndStudent(course, student);
        if (enrollmentOpt.isEmpty()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Bạn chưa đăng ký khóa học này. Vui lòng mua khóa học trước khi nộp bài.");
        }

        // Validate input - BẮT BUỘC phải có nội dung bài làm
        String trimmedAnswer = studentAnswer != null ? studentAnswer.trim() : "";
        if (trimmedAnswer.isEmpty()) {
            throw new IllegalArgumentException("Nội dung bài làm không được để trống");
        }

        Submission submission = submissionRepository.findByAssignmentAndStudent(assignment, student)
                .orElseGet(() -> {
                    Submission newSubmission = new Submission();
                    newSubmission.setAssignment(assignment);
                    newSubmission.setStudent(student);
                    return newSubmission;
                });

        // BẮT BUỘC phải set studentAnswer trước khi save
        submission.setStudentAnswer(trimmedAnswer);
        // Đồng bộ với cột cũ 'content' để tương thích với database
        submission.setContent(trimmedAnswer);
        submission.setSubmittedAt(LocalDateTime.now());
        // Khi nộp lại thì reset trạng thái chấm điểm để giảng viên xem lại
        submission.setGrade(null);
        submission.setTeacherFeedback(null);
        submission.setGradedAt(null);
        submission.setGradedBy(null);

        submissionRepository.save(submission);

        redirectAttributes.addFlashAttribute("message", "Nộp bài thành công! Bạn có thể bấm \"Chấm điểm ngay bằng AI\" để chấm hoặc chờ giảng viên chấm.");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/student/assignments/" + assignmentId;
    }

    private User currentUser(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy user"));
    }
}

