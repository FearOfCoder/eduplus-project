package com.example.doan10diem.controller.student;

import com.example.doan10diem.model.Course;
import com.example.doan10diem.model.Payment;
import com.example.doan10diem.model.User;
import com.example.doan10diem.repository.EnrollmentRepository;
import com.example.doan10diem.service.CourseService;
import com.example.doan10diem.service.PaymentService;
import com.example.doan10diem.service.UserService;
import jakarta.servlet.http.HttpSession;
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

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final CourseService courseService;
    private final UserService userService;
    private final EnrollmentRepository enrollmentRepository;

    /**
     * Hiển thị trang nhập thông tin thẻ thanh toán.
     * Endpoint: GET /courses/{id}/purchase
     */
    @GetMapping("/courses/{id}/purchase")
    public String showCheckoutPage(@PathVariable("id") Long courseId,
                                    Authentication authentication,
                                    Model model) {
        try {
            // 1. Lấy thông tin khóa học
            Course course = courseService.findByIdWithTeacherInitialized(courseId);
            
            // 2. Lấy username từ Authentication
            String username = authentication.getName();
            
            // 3. Tìm User từ Service
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy user"));
            
            // Kiểm tra đã mua khóa học này chưa
            if (enrollmentRepository.findByCourseAndStudent(course, user).isPresent()) {
                return "redirect:/student/course/" + courseId;
            }
            
            // 4. Add course và user vào Model
            model.addAttribute("course", course);
            model.addAttribute("user", user);
            
            // 5. Return view checkout
            return "student/payment/checkout";
            
        } catch (IllegalArgumentException e) {
            log.error("Course not found: {}", courseId, e);
            return "redirect:/courses?error=not_found";
        } catch (Exception e) {
            log.error("Error loading checkout page for courseId: {}", courseId, e);
            return "redirect:/courses?error=server_error";
        }
    }

    /**
     * Xử lý thanh toán - khởi tạo OTP.
     * Endpoint: POST /payment/process (tương thích với form HTML hiện tại)
     * Hoặc: POST /courses/purchase (theo yêu cầu mới)
     */
    @PostMapping({"/payment/process", "/courses/purchase"})
    public String processPayment(@RequestParam("courseId") Long courseId,
                                 @RequestParam("bankCode") String bankCode,
                                 @RequestParam(value = "cardNumber", required = false) String cardNumber,
                                 @RequestParam(value = "cardHolder", required = false) String cardHolder,
                                 Authentication authentication,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        try {
            // 1. Lấy User hiện tại
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy user"));
            
            // 2. Lấy Course theo ID
            Course course = courseService.findByIdWithTeacherInitialized(courseId);
            
            // Kiểm tra đã mua khóa học này chưa
            if (enrollmentRepository.findByCourseAndStudent(course, user).isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Bạn đã tham gia khóa học này rồi.");
                return "redirect:/courses/" + courseId + "/purchase";
            }
            
            // Validate thẻ (mock - chỉ kiểm tra độ dài nếu có)
            if (cardNumber != null && cardNumber.replaceAll("\\s+", "").length() < 12) {
                redirectAttributes.addFlashAttribute("error", "Số thẻ không hợp lệ (yêu cầu >= 12 ký tự).");
                return "redirect:/courses/" + courseId + "/purchase";
            }
            
            // 3. Gọi paymentService.initiatePayment(user, course, bankCode, session)
            paymentService.initiatePayment(user, course, bankCode, session);
            
            // 4. Redirect sang trang nhập OTP
            redirectAttributes.addFlashAttribute("message", "Mã OTP đã được gửi tới email của bạn.");
            return "redirect:/payment/otp-verify";
            
        } catch (IllegalStateException e) {
            log.error("Error processing payment: ", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/courses/" + courseId + "/purchase";
        } catch (IllegalArgumentException e) {
            log.error("Course not found: {}", courseId, e);
            redirectAttributes.addFlashAttribute("error", "Khóa học không tồn tại.");
            return "redirect:/courses";
        } catch (Exception e) {
            log.error("Error processing payment for courseId: {}", courseId, e);
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/courses/" + courseId + "/purchase";
        }
    }

    /**
     * Hiển thị trang nhập OTP.
     * Endpoint: GET /payment/otp-verify
     */
    @GetMapping("/payment/otp-verify")
    public String showOtpVerification(Authentication authentication,
                                       HttpSession session,
                                       Model model) {
        // Kiểm tra Session có hợp lệ không
        Long userId = (Long) session.getAttribute("payment_user_id");
        if (userId == null) {
            return "redirect:/courses";
        }

        try {
            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy user"));
            
            if (!userId.equals(user.getId())) {
                session.invalidate();
                return "redirect:/courses";
            }

            model.addAttribute("user", user);
            return "student/payment/otp";
            
        } catch (Exception e) {
            log.error("Error loading OTP verification page: ", e);
            return "redirect:/courses";
        }
    }

    /**
     * Xử lý xác thực OTP và hoàn tất thanh toán.
     * Endpoint: POST /payment/otp-verify
     */
    @PostMapping("/payment/otp-verify")
    public String verifyOtp(@RequestParam String otp,
                            Authentication authentication,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy user"));

            // Xác thực OTP và xử lý thanh toán
            Payment payment = paymentService.verifyOtpAndProcess(user, otp, session);

            // Redirect sang trang thành công
            return "redirect:/payment/success?courseId=" + payment.getCourse().getId();

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/payment/otp-verify";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/courses";
        } catch (Exception e) {
            log.error("Error verifying OTP: ", e);
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/payment/otp-verify";
        }
    }

    /**
     * Hiển thị trang thành công sau khi thanh toán.
     * Endpoint: GET /payment/success
     */
    @GetMapping("/payment/success")
    public String showSuccessPage(@RequestParam("courseId") Long courseId,
                                   Authentication authentication,
                                   Model model) {
        try {
            // 1. Lấy thông tin khóa học và giảng viên từ DB theo courseId
            Course course = courseService.findByIdWithTeacherInitialized(courseId);
            
            // 2. Kiểm tra user đã mua khóa học này chưa
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy user"));
            
            if (!enrollmentRepository.findByCourseAndStudent(course, user).isPresent()) {
                // Nếu chưa có enrollment, redirect về trang khóa học
                return "redirect:/courses/" + courseId;
            }
            
            // 3. Add vào model
            model.addAttribute("course", course);
            
            // 4. Trả về view
            return "student/payment/success";
            
        } catch (IllegalArgumentException e) {
            log.error("Course not found: {}", courseId, e);
            return "redirect:/courses?error=not_found";
        } catch (Exception e) {
            log.error("Error loading success page for courseId: {}", courseId, e);
            return "redirect:/courses";
        }
    }

    /**
     * Hiển thị lịch sử giao dịch.
     * Endpoint: GET /student/payment-history
     */
    @GetMapping("/student/payment-history")
    public String paymentHistory(Authentication authentication, Model model) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy user"));
            
            List<Payment> payments = paymentService.getPaymentHistory(user);

            model.addAttribute("user", user);
            model.addAttribute("payments", payments);
            return "student/payment/history";
            
        } catch (Exception e) {
            log.error("Error loading payment history: ", e);
            return "redirect:/courses";
        }
    }
}
