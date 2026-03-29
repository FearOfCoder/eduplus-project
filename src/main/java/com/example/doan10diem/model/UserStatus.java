package com.example.doan10diem.model;

public enum UserStatus {
    ACTIVE,
    PENDING,   // Chờ duyệt (Giảng viên mới đăng ký)
    REJECTED,  // Đã từ chối (Admin từ chối hồ sơ giảng viên)
    INACTIVE,
    BANNED
}

