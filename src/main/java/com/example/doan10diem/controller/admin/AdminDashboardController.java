package com.example.doan10diem.controller.admin;

import com.example.doan10diem.model.AdminWithdrawal;
import com.example.doan10diem.model.Payment;
import com.example.doan10diem.model.PaymentStatus;
import com.example.doan10diem.repository.AdminWithdrawalRepository;
import com.example.doan10diem.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class AdminDashboardController {

    private final PaymentRepository paymentRepository;
    private final AdminWithdrawalRepository adminWithdrawalRepository;

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        List<Payment> successPayments = paymentRepository.findByStatusOrderByCreatedAtDesc(PaymentStatus.SUCCESS);

        BigDecimal totalPlatformRevenue = successPayments.stream()
                .map(p -> p.getPlatformShare() != null ? p.getPlatformShare() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Double totalAdminWithdrawn = adminWithdrawalRepository.getTotalAdminWithdrawn();
        if (totalAdminWithdrawn == null) totalAdminWithdrawn = 0.0;

        double availableBalance = totalPlatformRevenue.doubleValue() - totalAdminWithdrawn;

        List<AdminWithdrawal> adminWithdrawalHistory = adminWithdrawalRepository.findAllByOrderByRequestDateDesc();

        model.addAttribute("totalPlatformRevenue", totalPlatformRevenue);
        model.addAttribute("availableBalance", availableBalance);
        model.addAttribute("successPayments", successPayments);
        model.addAttribute("adminWithdrawalHistory", adminWithdrawalHistory);
        return "admin/dashboard";
    }

    @PostMapping("/admin/withdraw")
    public String withdraw(
            @RequestParam("amount") Double amount,
            @RequestParam(value = "reason", required = false) String reason,
            RedirectAttributes redirectAttributes) {
        Double totalAdminWithdrawn = adminWithdrawalRepository.getTotalAdminWithdrawn();
        if (totalAdminWithdrawn == null) totalAdminWithdrawn = 0.0;
        BigDecimal totalPlatformRevenue = paymentRepository.findByStatusOrderByCreatedAtDesc(PaymentStatus.SUCCESS)
                .stream()
                .map(p -> p.getPlatformShare() != null ? p.getPlatformShare() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        double availableBalance = totalPlatformRevenue.doubleValue() - totalAdminWithdrawn;

        if (amount == null || amount <= 0) {
            redirectAttributes.addFlashAttribute("withdrawError", "Số tiền rút phải lớn hơn 0.");
            return "redirect:/admin/dashboard";
        }
        if (amount > availableBalance) {
            redirectAttributes.addFlashAttribute("withdrawError", "Số tiền vượt quá số dư khả dụng.");
            return "redirect:/admin/dashboard";
        }

        AdminWithdrawal w = new AdminWithdrawal();
        w.setAmount(amount);
        w.setRequestDate(LocalDateTime.now());
        w.setStatus("SUCCESS");
        w.setReason(reason != null ? reason.trim() : null);
        adminWithdrawalRepository.save(w);
        return "redirect:/admin/dashboard?withdrawSuccess";
    }
}
