package com.example.doan10diem.controller.admin;

import com.example.doan10diem.model.Faq;
import com.example.doan10diem.model.FaqCategory;
import com.example.doan10diem.repository.FaqCategoryRepository;
import com.example.doan10diem.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/faq")
@RequiredArgsConstructor
public class AdminFaqController {

    private final FaqRepository faqRepository;
    private final FaqCategoryRepository faqCategoryRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("faqs", faqRepository.findAllByOrderByCategory_IdAscIdAsc());
        model.addAttribute("categories", faqCategoryRepository.findAllWithFaqsOrderByIdAsc());
        return "admin/faq-management";
    }

    @PostMapping
    public String create(@RequestParam String question,
                         @RequestParam String answer,
                         @RequestParam Long categoryId,
                         RedirectAttributes redirectAttributes) {
        FaqCategory category = faqCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại"));
        Faq faq = Faq.builder()
                .question(question)
                .answer(answer)
                .category(category)
                .build();
        faqRepository.save(faq);
        redirectAttributes.addFlashAttribute("message", "Đã thêm câu hỏi mới.");
        return "redirect:/admin/faq";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Faq faq = faqRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("FAQ không tồn tại"));
        model.addAttribute("faq", faq);
        model.addAttribute("faqs", faqRepository.findAllByOrderByCategory_IdAscIdAsc());
        model.addAttribute("categories", faqCategoryRepository.findAllWithFaqsOrderByIdAsc());
        return "admin/faq-management";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @RequestParam String question,
                         @RequestParam String answer,
                         @RequestParam Long categoryId,
                         RedirectAttributes redirectAttributes) {
        Faq faq = faqRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("FAQ không tồn tại"));
        FaqCategory category = faqCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Danh mục không tồn tại"));
        faq.setQuestion(question);
        faq.setAnswer(answer);
        faq.setCategory(category);
        faqRepository.save(faq);
        redirectAttributes.addFlashAttribute("message", "Đã cập nhật câu hỏi.");
        return "redirect:/admin/faq";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        faqRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Đã xóa câu hỏi.");
        return "redirect:/admin/faq";
    }
}
