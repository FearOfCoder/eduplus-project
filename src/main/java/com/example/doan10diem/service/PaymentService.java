package com.example.doan10diem.service;

import com.example.doan10diem.model.*;
import com.example.doan10diem.repository.CourseRepository;
import com.example.doan10diem.repository.EnrollmentRepository;
import com.example.doan10diem.repository.PaymentRepository;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final Random random = new Random();

    /**
     * Khởi tạo quá trình thanh toán: tạo OTP và gửi email.
     *
     * @param user     User đang thanh toán
     * @param course   Khóa học cần thanh toán
     * @param bankCode Mã ngân hàng (VCB, MB, VISA, etc.)
     * @param session  HttpSession để lưu thông tin tạm thời
     * @return Mã OTP đã được tạo (để test, trong thực tế không nên trả về)
     */
    public String initiatePayment(User user, Course course, String bankCode, HttpSession session) {
        // Kiểm tra đã mua khóa học này chưa
        if (enrollmentRepository.findByCourseAndStudent(course, user).isPresent()) {
            throw new IllegalStateException("Bạn đã tham gia khóa học này rồi.");
        }

        // Tạo OTP ngẫu nhiên 5 chữ số
        String otp = String.format("%05d", random.nextInt(100000));

        // Tạo mã giao dịch duy nhất
        String transactionId = "TXN_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "_" + user.getId();

        // Lưu thông tin thanh toán tạm thời vào Session
        session.setAttribute("payment_user_id", user.getId());
        session.setAttribute("payment_course_id", course.getId());
        session.setAttribute("payment_bank_code", bankCode);
        session.setAttribute("payment_amount", course.getPrice());
        session.setAttribute("payment_otp", otp);
        session.setAttribute("payment_transaction_id", transactionId);
        session.setAttribute("payment_created_at", LocalDateTime.now());

        // Gửi email chứa OTP
        try {
            emailService.sendOtpEmail(user, otp, course.getTitle());
            log.info("OTP email sent to {} for course {}", user.getEmail(), course.getTitle());
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to {}: {}", user.getEmail(), e.getMessage());
            throw new RuntimeException("Không thể gửi email OTP. Vui lòng thử lại sau.", e);
        }

        return otp; // Trả về để test, trong production không nên trả về
    }

    /**
     * Xác thực OTP và xử lý thanh toán.
     *
     * @param user      User đang thanh toán
     * @param inputOtp  OTP do user nhập
     * @param session   HttpSession chứa thông tin thanh toán tạm thời
     * @return Payment đã được tạo
     */
    @Transactional
    public Payment verifyOtpAndProcess(User user, String inputOtp, HttpSession session) {
        // Lấy thông tin từ Session
        Long userId = (Long) session.getAttribute("payment_user_id");
        Long courseId = (Long) session.getAttribute("payment_course_id");
        String bankCode = (String) session.getAttribute("payment_bank_code");
        BigDecimal amount = (BigDecimal) session.getAttribute("payment_amount");
        String storedOtp = (String) session.getAttribute("payment_otp");
        String transactionId = (String) session.getAttribute("payment_transaction_id");
        LocalDateTime createdAt = (LocalDateTime) session.getAttribute("payment_created_at");

        // Kiểm tra Session có hợp lệ không
        if (userId == null || courseId == null || storedOtp == null) {
            throw new IllegalStateException("Phiên thanh toán đã hết hạn. Vui lòng thực hiện lại.");
        }

        // Kiểm tra user có khớp không
        if (!userId.equals(user.getId())) {
            throw new IllegalStateException("Phiên thanh toán không hợp lệ.");
        }

        // Kiểm tra OTP
        if (!storedOtp.equals(inputOtp)) {
            throw new IllegalArgumentException("Mã OTP không đúng. Vui lòng thử lại.");
        }

        // Kiểm tra OTP đã hết hạn chưa (10 phút)
        if (createdAt != null && createdAt.plusMinutes(10).isBefore(LocalDateTime.now())) {
            session.invalidate();
            throw new IllegalStateException("Mã OTP đã hết hạn. Vui lòng thực hiện lại thanh toán.");
        }

        // Lấy thông tin Course
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Khóa học không tồn tại"));

        // Kiểm tra lại đã mua chưa (tránh race condition)
        if (enrollmentRepository.findByCourseAndStudent(course, user).isPresent()) {
            session.invalidate();
            throw new IllegalStateException("Bạn đã tham gia khóa học này rồi.");
        }

        // Tính toán platform share và teacher share
        BigDecimal platformShare = amount
                .multiply(BigDecimal.valueOf(course.getPlatformFeePercent()))
                .divide(BigDecimal.valueOf(100));
        BigDecimal teacherShare = amount.subtract(platformShare);

        // Tạo Payment với status SUCCESS
        Payment payment = Payment.builder()
                .student(user)
                .course(course)
                .amount(amount)
                .bankCode(bankCode)
                .transactionId(transactionId != null ? transactionId : "TXN_" + System.currentTimeMillis())
                .status(PaymentStatus.SUCCESS)
                .createdAt(createdAt != null ? createdAt : LocalDateTime.now())
                .paymentDate(LocalDateTime.now())
                .description("Thanh toán khóa học: " + course.getTitle())
                .platformShare(platformShare)
                .teacherShare(teacherShare)
                .build();

        paymentRepository.save(payment);

        // Tạo Enrollment
        Enrollment enrollment = Enrollment.builder()
                .course(course)
                .student(user)
                .enrolledAt(LocalDateTime.now())
                .progressPercent(0)
                .completed(false)
                .build();
        enrollmentRepository.save(enrollment);

        // Gửi thông báo thanh toán thành công
        notificationService.createNotification(
                user,
                "Thanh toán thành công",
                "Thanh toán thành công khóa học \"" + course.getTitle() + "\". Vào học ngay!",
                "/student/course/" + course.getId(),
                NotificationType.PAYMENT
        );

        // Xóa Session sau khi xử lý thành công
        session.removeAttribute("payment_user_id");
        session.removeAttribute("payment_course_id");
        session.removeAttribute("payment_bank_code");
        session.removeAttribute("payment_amount");
        session.removeAttribute("payment_otp");
        session.removeAttribute("payment_transaction_id");
        session.removeAttribute("payment_created_at");

        log.info("Payment successful: TransactionId={}, User={}, Course={}", transactionId, user.getUsername(), course.getTitle());

        return payment;
    }

    /**
     * Lấy danh sách lịch sử thanh toán của user.
     *
     * @param user User cần lấy lịch sử
     * @return Danh sách Payment
     */
    public java.util.List<Payment> getPaymentHistory(User user) {
        return paymentRepository.findByStudentOrderByCreatedAtDesc(user);
    }
}
