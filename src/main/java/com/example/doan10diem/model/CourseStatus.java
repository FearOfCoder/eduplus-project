package com.example.doan10diem.model;

public enum CourseStatus {
    DRAFT,           // Giáo viên đang soạn
    PENDING_REVIEW,  // Chờ admin duyệt
    PUBLISHED,       // Đã duyệt, hiển thị cho student
    INACTIVE,        // Đã dừng hoạt động (admin tạm ẩn)
    REJECTED
}

