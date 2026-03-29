package com.example.doan10diem.config;

import com.example.doan10diem.model.*;
import com.example.doan10diem.repository.CourseRepository;
import com.example.doan10diem.repository.SystemConfigRepository;
import com.example.doan10diem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.example.doan10diem.util.PasswordHashUtil.sha256;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final SystemConfigRepository systemConfigRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        // Tạo user mẫu: admin, teacher, student (đã xác thực email để đăng nhập ngay)
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(sha256("admin123"));
        admin.setEmail("admin@example.com");
        admin.setFullName("Admin");
        admin.setRole(Role.ADMIN);
        admin.setEmailVerified(true);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setIsVerified(true);

        User teacher = new User();
        teacher.setUsername("teacher");
        teacher.setPassword(sha256("teacher123"));
        teacher.setEmail("teacher@example.com");
        teacher.setFullName("Giảng viên A");
        teacher.setRole(Role.TEACHER);
        teacher.setEmailVerified(true);
        teacher.setStatus(UserStatus.ACTIVE);
        teacher.setIsVerified(true);

        User student = new User();
        student.setUsername("student");
        student.setPassword(sha256("student123"));
        student.setEmail("student@example.com");
        student.setFullName("Sinh viên B");
        student.setRole(Role.STUDENT);
        student.setEmailVerified(true);
        student.setStatus(UserStatus.ACTIVE);
        student.setIsVerified(true);

        userRepository.save(admin);
        userRepository.save(teacher);
        userRepository.save(student);

        // Tạo vài khóa học mẫu cho teacher
        Course course1 = Course.builder()
                .title("Lập trình Java cơ bản")
                .description("Khóa học Java từ cơ bản đến nâng cao.")
                .teacher(teacher)
                .status(CourseStatus.PUBLISHED)
                .price(BigDecimal.valueOf(499000))
                .platformFeePercent(20)
                .createdAt(LocalDateTime.now())
                .publishedAt(LocalDateTime.now())
                .build();

        Course course2 = Course.builder()
                .title("Spring Boot cho người mới bắt đầu")
                .description("Xây dựng ứng dụng web với Spring Boot và Thymeleaf.")
                .teacher(teacher)
                .status(CourseStatus.PUBLISHED)
                .price(BigDecimal.valueOf(699000))
                .platformFeePercent(20)
                .createdAt(LocalDateTime.now())
                .publishedAt(LocalDateTime.now())
                .build();

        courseRepository.save(course1);
        courseRepository.save(course2);

        // Seed SystemConfig: Điều khoản cho Teacher
        if (systemConfigRepository.findByConfigKey("TEACHER_TERMS").isEmpty()) {
            SystemConfig teacherTerms = SystemConfig.builder()
                    .configKey("TEACHER_TERMS")
                    .configValue("1. Phí nền tảng là 20% doanh thu.\n2. Giảng viên cam kết nội dung không vi phạm bản quyền.\n3. Nghiêm cấm nội dung chống phá nhà nước hoặc vi phạm pháp luật.")
                    .build();
            systemConfigRepository.save(teacherTerms);
        }

        // Seed SystemConfig: Phí nền tảng (Platform Fee)
        if (systemConfigRepository.findByConfigKey("PLATFORM_FEE").isEmpty()) {
            SystemConfig platformFee = SystemConfig.builder()
                    .configKey("PLATFORM_FEE")
                    .configValue("20")
                    .build();
            systemConfigRepository.save(platformFee);
        }
    }
}

