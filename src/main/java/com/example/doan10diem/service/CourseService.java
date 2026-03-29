package com.example.doan10diem.service;

import com.example.doan10diem.model.Course;
import com.example.doan10diem.model.CourseStatus;
import com.example.doan10diem.model.User;
import com.example.doan10diem.model.UserStatus;
import com.example.doan10diem.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;

    /**
     * Kiểm tra giảng viên được phép tạo khóa học (đã duyệt, status = ACTIVE).
     * Nếu đang chờ duyệt (PENDING) hoặc không ACTIVE thì ném exception.
     */
    public void ensureTeacherCanCreateCourse(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Bạn cần đăng nhập để tạo khóa học.");
        }
        if (user.getStatus() == null || user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Tài khoản của bạn đang chờ duyệt, chưa thể tạo khóa học!");
        }
    }

    /** Lấy top N khóa học mới nhất đã PUBLISHED (đã khởi tạo teacher). */
    @Transactional(readOnly = true)
    public List<Course> getTopCourses(int limit) {
        List<Course> courses = courseRepository.findTop8ByStatusOrderByCreatedAtDesc(CourseStatus.PUBLISHED);
        if (limit < courses.size()) {
            courses = new java.util.ArrayList<>(courses.subList(0, limit));
        }
        for (Course course : courses) {
            initializeCourseTeacher(course);
        }
        return courses;
    }

    @Transactional(readOnly = true)
    public List<Course> findAllWithTeacherInitialized() {
        List<Course> courses = courseRepository.findAll();
        // Force initialize teacher cho mỗi course trong transaction
        for (Course course : courses) {
            initializeCourseTeacher(course);
        }
        return courses;
    }

    @Transactional(readOnly = true)
    public Course findByIdWithTeacherInitialized(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khóa học không tồn tại"));
        // Force initialize teacher
        initializeCourseTeacher(course);
        return course;
    }

    private void initializeCourseTeacher(Course course) {
        if (course.getTeacher() != null) {
            // Force initialize teacher proxy
            Hibernate.initialize(course.getTeacher());
            // Truy cập tất cả fields cần thiết để đảm bảo được load
            course.getTeacher().getFullName();
            course.getTeacher().getUsername();
            course.getTeacher().getEmail();
            course.getTeacher().getId();
        }
        // Đảm bảo các fields của course cũng được load
        course.getTitle();
        course.getDescription();
        course.getPrice();
        course.getStatus();
        course.getId();
    }
}
