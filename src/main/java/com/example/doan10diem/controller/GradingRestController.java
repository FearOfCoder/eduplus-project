package com.example.doan10diem.controller;

import com.example.doan10diem.dto.GradingResultDTO;
import com.example.doan10diem.model.Submission;
import com.example.doan10diem.model.User;
import com.example.doan10diem.repository.SubmissionRepository;
import com.example.doan10diem.repository.UserRepository;
import com.example.doan10diem.service.OpenAIGradingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * REST API cho chấm điểm bằng AI (Student bấm nút chấm, gọi API riêng).
 */
@RestController
@RequestMapping("/api/grading")
@RequiredArgsConstructor
public class GradingRestController {

    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final OpenAIGradingService openAIGradingService;

    /**
     * Chấm điểm bài nộp bằng AI.
     * Chỉ chủ bài nộp (student) mới được gọi.
     */
    @PostMapping("/{submissionId}")
    public ResponseEntity<?> gradeByAi(@PathVariable Long submissionId, Authentication authentication) {
        User currentUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("User không tồn tại"));

        Submission submission = submissionRepository.findById(submissionId)
                .orElse(null);

        if (submission == null) {
            return ResponseEntity.status(404).body("LỖI AI: Bài nộp không tồn tại (404 Not Found). submissionId=" + submissionId);
        }

        if (!submission.getStudent().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403).body("LỖI AI: Bạn không có quyền chấm bài nộp này (403 Forbidden).");
        }

        var assignment = submission.getAssignment();
        String question = assignment.getTitle() +
                (assignment.getDescription() != null && !assignment.getDescription().trim().isEmpty()
                        ? "\n" + assignment.getDescription()
                        : "");
        String correctAnswer = assignment.getCorrectAnswer();
        String studentAnswer = submission.getStudentAnswer();

        try {
            GradingResultDTO result = openAIGradingService.gradeSubmission(question, studentAnswer, correctAnswer);

            if (result == null || result.getScore() == null || result.getFeedback() == null) {
                return ResponseEntity.badRequest().body("LỖI AI: Không nhận được kết quả chấm từ OpenAI (response null hoặc thiếu score/feedback).");
            }

            submission.setAiGrade(result.getScore());
            submission.setAiFeedback(result.getFeedback());
            submissionRepository.save(submission);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            String detail = e.getClass().getSimpleName() + ": " + e.getMessage();
            return ResponseEntity.badRequest().body("LỖI AI: " + detail);
        }
    }
}
