package com.example.doan10diem.config;

import com.example.doan10diem.model.Faq;
import com.example.doan10diem.model.FaqCategory;
import com.example.doan10diem.repository.FaqCategoryRepository;
import com.example.doan10diem.repository.FaqRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class FaqDataSeeder implements CommandLineRunner {

    private final FaqCategoryRepository categoryRepo;
    private final FaqRepository faqRepo;

    public FaqDataSeeder(FaqCategoryRepository categoryRepo, FaqRepository faqRepo) {
        this.categoryRepo = categoryRepo;
        this.faqRepo = faqRepo;
    }

    @Override
    public void run(String... args) {
        if (categoryRepo.count() > 0) {
            return;
        }

        // 1. Danh mục Thanh toán
        FaqCategory payment = new FaqCategory();
        payment.setName("Thanh toán & Nạp tiền");
        payment.setIcon("fas fa-wallet");
        payment = categoryRepo.save(payment);
        faqRepo.save(Faq.builder()
                .question("Hệ thống hỗ trợ những phương thức thanh toán nào?")
                .answer("Chúng tôi hỗ trợ thanh toán qua chuyển khoản ngân hàng (Techcombank) và các ví điện tử phổ biến. Mọi giao dịch đều được mã hóa an toàn.")
                .category(payment)
                .build());
        faqRepo.save(Faq.builder()
                .question("Làm sao để biết tôi đã thanh toán thành công?")
                .answer("Ngay sau khi giao dịch hoàn tất, hệ thống sẽ gửi email xác nhận và khóa học sẽ tự động được mở trong mục 'Khóa học của tôi'.")
                .category(payment)
                .build());

        // 2. Danh mục Chấm điểm AI
        FaqCategory aiGrading = new FaqCategory();
        aiGrading.setName("Chấm điểm bằng AI");
        aiGrading.setIcon("fas fa-robot");
        aiGrading = categoryRepo.save(aiGrading);
        faqRepo.save(Faq.builder()
                .question("Hệ thống AI chấm điểm hoạt động như thế nào?")
                .answer("EduPlus tích hợp mô hình AI tiên tiến để phân tích mã nguồn (code) và câu trả lời tự luận của bạn. AI không chỉ chấm điểm mà còn chỉ ra lỗi sai và gợi ý cách tối ưu thuật toán.")
                .category(aiGrading)
                .build());
        faqRepo.save(Faq.builder()
                .question("Điểm do AI chấm có chính xác tuyệt đối không?")
                .answer("AI mang tính chất đánh giá khách quan dựa trên testcase và ngữ nghĩa. Giảng viên vẫn có quyền xem xét lại và điều chỉnh điểm nếu cần thiết.")
                .category(aiGrading)
                .build());

        // 3. Danh mục Tài khoản
        FaqCategory account = new FaqCategory();
        account.setName("Quản lý Tài khoản");
        account.setIcon("fas fa-user-shield");
        account = categoryRepo.save(account);
        faqRepo.save(Faq.builder()
                .question("Tôi có thể chia sẻ tài khoản cho người khác học cùng không?")
                .answer("Theo chính sách bảo mật, mỗi tài khoản chỉ dành cho một người dùng. Hệ thống sẽ cảnh báo nếu phát hiện đăng nhập từ quá nhiều thiết bị.")
                .category(account)
                .build());
    }
}
