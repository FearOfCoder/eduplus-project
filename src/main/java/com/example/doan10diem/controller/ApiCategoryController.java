package com.example.doan10diem.controller;

import com.example.doan10diem.model.Category;
import com.example.doan10diem.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class ApiCategoryController {

    private final CategoryRepository categoryRepository;

    /**
     * Trả về danh sách danh mục con (JSON) theo parentId.
     * Dùng cho cascading dropdown: chọn Cha -> load Con.
     */
    @GetMapping("/{parentId}/children")
    public ResponseEntity<List<CategoryOption>> getChildren(@PathVariable Long parentId) {
        List<Category> children = categoryRepository.findByParent_IdOrderByNameAsc(parentId);
        List<CategoryOption> options = children.stream()
                .map(c -> new CategoryOption(c.getId(), c.getName()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(options);
    }

    public static class CategoryOption {
        private final Long id;
        private final String name;

        public CategoryOption(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
    }
}
