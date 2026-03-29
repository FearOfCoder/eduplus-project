package com.example.doan10diem.service;

import com.example.doan10diem.model.*;
import com.example.doan10diem.repository.EnrollmentRepository;
import com.example.doan10diem.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public Notification createNotification(User user, String title, String message, String link, NotificationType type) {
        Notification n = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .link(link)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .type(type)
                .build();
        return notificationRepository.save(n);
    }

    @Transactional
    public void markAsRead(Long id, Long userId) {
        notificationRepository.findById(id)
                .filter(n -> n.getUser() != null && userId.equals(n.getUser().getId()))
                .ifPresent(n -> {
                    n.setRead(true);
                    notificationRepository.save(n);
                });
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    public List<Notification> getAllNotifications(Long userId) {
        return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Gửi thông báo cập nhật khóa học cho tất cả học viên đã mua khóa học.
     */
    @Transactional
    public void notifyCourseUpdate(Course course) {
        List<Enrollment> enrollments = enrollmentRepository.findByCourse(course);
        String title = "Khóa học có bài học mới";
        String message = "Khóa học \"" + course.getTitle() + "\" vừa có bài học mới!";
        String link = "/student/my-courses";
        if (course.getId() != null) {
            link = "/student/course/" + course.getId();
        }
        for (Enrollment e : enrollments) {
            User student = e.getStudent();
            if (student != null) {
                createNotification(student, title, message, link, NotificationType.COURSE_UPDATE);
            }
        }
    }
}
