package com.example.doan10diem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootApplication
public class DoAn10DiemApplication {

    public static void main(String[] args) throws IOException {
        // Tạo thư mục tạm multipart trước khi Spring khởi động (tránh MultipartException do temp dir)
        var tempDir = Paths.get("uploads_temp").toAbsolutePath();
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }
        SpringApplication.run(DoAn10DiemApplication.class, args);
    }
}

