package com.example.doan10diem.controller;

import com.example.doan10diem.dto.LessonChatRequest;
import com.example.doan10diem.model.Lesson;
import com.example.doan10diem.model.User;
import com.example.doan10diem.repository.EnrollmentRepository;
import com.example.doan10diem.repository.LessonRepository;
import com.example.doan10diem.repository.UserRepository;
import com.example.doan10diem.service.OpenAIGradingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * REST API cho tính năng "Hỏi đáp với AI" theo ngữ cảnh bài học.
 * Chỉ sinh viên đã ghi danh khóa học chứa bài học đó mới được gọi.
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class LessonChatController {

    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final OpenAIGradingService openAIGradingService;

    @PostMapping("/lesson")
    public ResponseEntity<?> chatWithLesson(@RequestBody @Valid LessonChatRequest request, Authentication authentication) {
        User currentUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("User không tồn tại"));

        Optional<Lesson> lessonOpt = lessonRepository.findById(request.getLessonId());
        if (lessonOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Bài học không tồn tại."));
        }

        Lesson lesson = lessonOpt.get();

        if (enrollmentRepository.findByCourseAndStudent(lesson.getCourse(), currentUser).isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("error", "Bạn chưa ghi danh khóa học chứa bài học này."));
        }

        String lessonTitle = lesson.getTitle();
        String lessonContent = lesson.getContent() != null ? lesson.getContent() : "";
        String reply = openAIGradingService.chatWithLessonContext(lessonTitle, lessonContent, request.getQuestion());

        return ResponseEntity.ok(Map.of("reply", reply != null ? reply : ""));
    }
}
