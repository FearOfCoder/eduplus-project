package com.example.doan10diem.controller;

import com.example.doan10diem.model.FaqCategory;
import com.example.doan10diem.repository.FaqCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HelpController {

    private final FaqCategoryRepository faqCategoryRepository;

    @GetMapping("/help")
    public String help(Model model) {
        List<FaqCategory> categories = faqCategoryRepository.findAllWithFaqsOrderByIdAsc();
        model.addAttribute("categories", categories);
        return "help";
    }
}
