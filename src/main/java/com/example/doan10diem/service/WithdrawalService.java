package com.example.doan10diem.service;

import com.example.doan10diem.model.PaymentStatus;
import com.example.doan10diem.model.User;
import com.example.doan10diem.model.Withdrawal;
import com.example.doan10diem.model.WithdrawalStatus;
import com.example.doan10diem.repository.PaymentRepository;
import com.example.doan10diem.repository.WithdrawalRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private static final String SESSION_OTP = "financeWithdrawalOtp";
    private static final String SESSION_AMOUNT = "financeWithdrawalAmount";
    private static final String SESSION_BANK_NAME = "financeWithdrawalBankName";
    private static final String SESSION_ACCOUNT_NUMBER = "financeWithdrawalAccountNumber";
    private static final String SESSION_ACCOUNT_NAME = "financeWithdrawalAccountName";

    private final WithdrawalRepository withdrawalRepository;
    private final PaymentRepository paymentRepository;
    private final EmailService emailService;

    /**
     * Số dư khả dụng = Tổng thực nhận (teacherShare từ Payment SUCCESS) - Tổng đã rút (SUCCESS + PENDING).
     */
    public double getAvailableBalance(User teacher) {
        BigDecimal totalRevenue = paymentRepository.findByCourseTeacherAndStatus(teacher, PaymentStatus.SUCCESS)
                .stream()
                .map(p -> p.getTeacherShare() != null ? p.getTeacherShare() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Double totalWithdrawn = withdrawalRepository.getTotalWithdrawnByTeacher(teacher.getId());
        if (totalWithdrawn == null) totalWithdrawn = 0.0;
        return totalRevenue.doubleValue() - totalWithdrawn;
    }

    /**
     * Tổng doanh thu tích lũy (tổng teacherShare từ Payment SUCCESS).
     */
    public double getTotalRevenue(User teacher) {
        BigDecimal total = paymentRepository.findByCourseTeacherAndStatus(teacher, PaymentStatus.SUCCESS)
                .stream()
                .map(p -> p.getTeacherShare() != null ? p.getTeacherShare() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.doubleValue();
    }

    /**
     * Khởi tạo yêu cầu rút tiền: kiểm tra số dư, tạo OTP, lưu session, gửi email.
     */
    public void initiateWithdrawal(User teacher, Double amount, String bankName, String accountNumber, String accountName, HttpSession session) throws MessagingException {
        double available = getAvailableBalance(teacher);
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Số tiền rút phải lớn hơn 0.");
        }
        if (amount > available) {
            throw new IllegalArgumentException("Số tiền rút vượt quá số dư khả dụng (" + String.format("%,.0f", available) + " ₫).");
        }
        String otp = String.format("%05d", new SecureRandom().nextInt(100000));
        session.setAttribute(SESSION_OTP, otp);
        session.setAttribute(SESSION_AMOUNT, amount);
        session.setAttribute(SESSION_BANK_NAME, bankName != null ? bankName : "");
        session.setAttribute(SESSION_ACCOUNT_NUMBER, accountNumber != null ? accountNumber : "");
        session.setAttribute(SESSION_ACCOUNT_NAME, accountName != null ? accountName : "");
        emailService.sendWithdrawalOtpEmail(teacher, otp);
    }

    /**
     * Xác thực OTP và tạo giao dịch rút tiền (status SUCCESS).
     */
    @Transactional
    public boolean verifyOtpAndProcess(User teacher, String inputOtp, HttpSession session) {
        String savedOtp = (String) session.getAttribute(SESSION_OTP);
        String input = (inputOtp == null) ? "" : inputOtp.trim();
        if (savedOtp == null || !savedOtp.equals(input)) {
            return false;
        }
        Double amount = (Double) session.getAttribute(SESSION_AMOUNT);
        String bankName = (String) session.getAttribute(SESSION_BANK_NAME);
        String accountNumber = (String) session.getAttribute(SESSION_ACCOUNT_NUMBER);
        String accountName = (String) session.getAttribute(SESSION_ACCOUNT_NAME);
        if (amount == null || amount <= 0) {
            clearWithdrawalSession(session);
            return false;
        }
        Withdrawal w = new Withdrawal();
        w.setTeacher(teacher);
        w.setAmount(amount);
        w.setBankName(bankName != null ? bankName : "");
        w.setAccountNumber(accountNumber != null ? accountNumber : "");
        w.setAccountName(accountName != null ? accountName : "");
        w.setRequestDate(LocalDateTime.now());
        w.setStatus(WithdrawalStatus.SUCCESS);
        withdrawalRepository.save(w);
        clearWithdrawalSession(session);
        return true;
    }

    private void clearWithdrawalSession(HttpSession session) {
        session.removeAttribute(SESSION_OTP);
        session.removeAttribute(SESSION_AMOUNT);
        session.removeAttribute(SESSION_BANK_NAME);
        session.removeAttribute(SESSION_ACCOUNT_NUMBER);
        session.removeAttribute(SESSION_ACCOUNT_NAME);
    }

    public List<Withdrawal> getWithdrawalHistory(User teacher) {
        return withdrawalRepository.findByTeacherOrderByRequestDateDesc(teacher);
    }
}
