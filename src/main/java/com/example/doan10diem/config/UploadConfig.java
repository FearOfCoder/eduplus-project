package com.example.doan10diem.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class UploadConfig {

    // 1. Cấu hình Upload (Dung lượng & Đường dẫn) - Dùng Code thuần Jakarta
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        // Thay đường dẫn tuyệt đối của bạn vào đây (Dùng dấu /)
        String location = "D:/DoAn10diem/uploads_temp";

        File tmpFile = new File(location);
        if (!tmpFile.exists()) {
            tmpFile.mkdirs(); // Tự tạo thư mục để tránh lỗi 500
        }

        // Location, MaxFileSize, MaxRequestSize, FileSizeThreshold
        return new MultipartConfigElement(
                location,
                100L * 1024L * 1024L, // 100MB Max File
                200L * 1024L * 1024L, // 200MB Max Request
                2 * 1024 // 2KB Threshold
        );
    }

    // 2. Cấu hình Tomcat (Fix lỗi FileCountLimitExceededException - Tomcat 11 giới hạn số part/param)
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> containerCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector -> {
            // Nới lỏng giới hạn số tham số / part trong request (Tomcat 11 mặc định thấp)
            connector.setMaxParameterCount(10000);
            // Nới lỏng kích thước POST (bytes), -1 = không giới hạn
            connector.setMaxPostSize(-1);
        });
    }
}
