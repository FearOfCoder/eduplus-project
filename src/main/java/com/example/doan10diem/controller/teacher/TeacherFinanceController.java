package com.example.doan10diem.controller.teacher;

import com.example.doan10diem.model.User;
import com.example.doan10diem.repository.UserRepository;
import com.example.doan10diem.service.WithdrawalService;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class TeacherFinanceController {

    private final WithdrawalService withdrawalService;
    private final UserRepository userRepository;

    private User currentUser(Authentication auth) {
        if (auth == null) return null;
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }

    @GetMapping("/teacher/finance")
    public String financePage(Authentication authentication, Model model) {
        User teacher = currentUser(authentication);
        if (teacher == null) return "redirect:/login";
        double availableBalance = withdrawalService.getAvailableBalance(teacher);
        double totalRevenue = withdrawalService.getTotalRevenue(teacher);
        var withdrawalHistory = withdrawalService.getWithdrawalHistory(teacher);
        model.addAttribute("availableBalance", availableBalance);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("withdrawalHistory", withdrawalHistory);
        return "teacher/finance";
    }

    @PostMapping("/teacher/finance/withdraw")
    public String submitWithdraw(
            Authentication authentication,
            @RequestParam("amount") Double amount,
            @RequestParam("bankName") String bankName,
            @RequestParam("accountNumber") String accountNumber,
            @RequestParam("accountName") String accountName,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        User teacher = currentUser(authentication);
        if (teacher == null) return "redirect:/login";
        try {
            withdrawalService.initiateWithdrawal(teacher, amount, bankName, accountNumber, accountName, session);
            return "redirect:/teacher/finance/otp-verify";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("withdrawError", e.getMessage());
            return "redirect:/teacher/finance";
        } catch (MessagingException e) {
            redirectAttributes.addFlashAttribute("withdrawError", "Không thể gửi email OTP. Vui lòng thử lại.");
            return "redirect:/teacher/finance";
        }
    }

    @GetMapping("/teacher/finance/otp-verify")
    public String otpVerifyPage() {
        return "teacher/finance-otp";
    }

    @PostMapping("/teacher/finance/otp-verify")
    public String verifyOtp(
            Authentication authentication,
            @RequestParam("otp") String otp,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        User teacher = currentUser(authentication);
        if (teacher == null) return "redirect:/login";
        boolean success = withdrawalService.verifyOtpAndProcess(teacher, otp, session);
        if (success) {
            return "redirect:/teacher/finance?success=true";
        }
        return "redirect:/teacher/finance/otp-verify?error=true";
    }
}
