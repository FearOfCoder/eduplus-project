package com.example.doan10diem.config;

import com.example.doan10diem.model.Category;
import com.example.doan10diem.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@RequiredArgsConstructor
public class CategorySeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(String... args) {
        if (categoryRepository.count() > 0) {
            return;
        }

        // CNTT & Lập trình (Cha) -> Java, Python, HTML/CSS, JavaScript (Con)
        Category cntt = Category.builder()
                .name("CNTT & Lập trình")
                .description("Các khóa học về công nghệ thông tin và lập trình")
                .parent(null)
                .build();
        categoryRepository.save(cntt);

        categoryRepository.save(Category.builder().name("Java").description("Lập trình Java").parent(cntt).build());
        categoryRepository.save(Category.builder().name("Python").description("Lập trình Python").parent(cntt).build());
        categoryRepository.save(Category.builder().name("HTML/CSS").description("Web cơ bản").parent(cntt).build());
        categoryRepository.save(Category.builder().name("JavaScript").description("JavaScript & Frontend").parent(cntt).build());

        // Ngoại ngữ (Cha) -> Tiếng Anh, Tiếng Đức, Tiếng Pháp, Tiếng Trung (Con)
        Category ngoaiNgu = Category.builder()
                .name("Ngoại ngữ")
                .description("Các khóa học ngoại ngữ")
                .parent(null)
                .build();
        categoryRepository.save(ngoaiNgu);

        categoryRepository.save(Category.builder().name("Tiếng Anh").description("Học tiếng Anh").parent(ngoaiNgu).build());
        categoryRepository.save(Category.builder().name("Tiếng Đức").description("Học tiếng Đức").parent(ngoaiNgu).build());
        categoryRepository.save(Category.builder().name("Tiếng Pháp").description("Học tiếng Pháp").parent(ngoaiNgu).build());
        categoryRepository.save(Category.builder().name("Tiếng Trung").description("Học tiếng Trung").parent(ngoaiNgu).build());

        // Marketing (Cha) -> Content Marketing, SEO, Digital Ads (Con)
        Category marketing = Category.builder()
                .name("Marketing")
                .description("Marketing và quảng bá")
                .parent(null)
                .build();
        categoryRepository.save(marketing);

        categoryRepository.save(Category.builder().name("Content Marketing").description("Content Marketing").parent(marketing).build());
        categoryRepository.save(Category.builder().name("SEO").description("Tối ưu hóa công cụ tìm kiếm").parent(marketing).build());
        categoryRepository.save(Category.builder().name("Digital Ads").description("Quảng cáo số").parent(marketing).build());

        // Thiết kế (Cha) -> Photoshop, Illustrator, UI/UX (Con)
        Category thietKe = Category.builder()
                .name("Thiết kế")
                .description("Thiết kế đồ họa và giao diện")
                .parent(null)
                .build();
        categoryRepository.save(thietKe);

        categoryRepository.save(Category.builder().name("Photoshop").description("Adobe Photoshop").parent(thietKe).build());
        categoryRepository.save(Category.builder().name("Illustrator").description("Adobe Illustrator").parent(thietKe).build());
        categoryRepository.save(Category.builder().name("UI/UX").description("Thiết kế giao diện và trải nghiệm").parent(thietKe).build());
    }
}
