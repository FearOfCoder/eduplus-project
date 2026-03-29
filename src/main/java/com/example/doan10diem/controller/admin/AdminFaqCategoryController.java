package com.example.doan10diem.controller.admin;

import com.example.doan10diem.model.FaqCategory;
import com.example.doan10diem.repository.FaqCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/faq/categories")
@RequiredArgsConstructor
public class AdminFaqCategoryController {

    private final FaqCategoryRepository faqCategoryRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", faqCategoryRepository.findAllWithFaqsOrderByIdAsc());
        return "admin/faq-categories";
    }

    @PostMapping
    public String create(@RequestParam String name,
                         @RequestParam(required = false) String icon,
                         RedirectAttributes redirectAttributes) {
        FaqCategory cat = new FaqCategory();
        cat.setName(name);
        cat.setIcon(icon != null && !icon.isBlank() ? icon.trim() : "fas fa-folder");
        faqCategoryRepository.save(cat);
        redirectAttributes.addFlashAttribute("message", "Đã thêm danh mục.");
        return "redirect:/admin/faq/categories";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @RequestParam String name,
                         @RequestParam(required = false) String icon,
                         RedirectAttributes redirectAttributes) {
        FaqCategory cat = faqCategoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại"));
        cat.setName(name);
        cat.setIcon(icon != null && !icon.isBlank() ? icon.trim() : "fas fa-folder");
        faqCategoryRepository.save(cat);
        redirectAttributes.addFlashAttribute("message", "Đã cập nhật danh mục.");
        return "redirect:/admin/faq/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        FaqCategory cat = faqCategoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại"));
        if (!cat.getFaqs().isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Không thể xóa danh mục đang có câu hỏi. Hãy xóa hoặc chuyển câu hỏi trước.");
            return "redirect:/admin/faq/categories";
        }
        faqCategoryRepository.delete(cat);
        redirectAttributes.addFlashAttribute("message", "Đã xóa danh mục.");
        return "redirect:/admin/faq/categories";
    }
}
