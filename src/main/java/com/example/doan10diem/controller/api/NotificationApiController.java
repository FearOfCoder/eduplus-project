package com.example.doan10diem.controller.api;

import com.example.doan10diem.dto.NotificationDTO;
import com.example.doan10diem.model.User;
import com.example.doan10diem.service.NotificationService;
import com.example.doan10diem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationApiController {

    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping("/unread")
    public ResponseEntity<Long> getUnreadCount(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.ok(0L);
        }
        User user = userService.findByUsername(auth.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(0L);
        }
        long count = notificationService.getUnreadCount(user.getId());
        return ResponseEntity.ok(count);
    }

    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getAll(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.ok(List.of());
        }
        User user = userService.findByUsername(auth.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(List.of());
        }
        List<NotificationDTO> list = notificationService.getAllNotifications(user.getId()).stream()
                .map(NotificationDTO::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PostMapping("/read/{id}")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        User user = userService.findByUsername(auth.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        notificationService.markAsRead(id, user.getId());
        return ResponseEntity.ok().build();
    }
}
