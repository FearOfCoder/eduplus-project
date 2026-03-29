package com.example.doan10diem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.avatar.dir:user-photos}")
    private String avatarDir;

    @Value("${file.teacher-docs.dir:uploads/teacher-docs}")
    private String teacherDocsDir;

    @Value("${file.course-images.dir:course-images}")
    private String courseImagesDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map /uploads/teacher-docs/** tới thư mục CV & chứng chỉ (ưu tiên trước)
        String teacherDocsPath = Paths.get(teacherDocsDir).toAbsolutePath().toString();
        registry.addResourceHandler("/uploads/teacher-docs/**")
                .addResourceLocations("file:" + teacherDocsPath + "/");
        // Map URL /course-images/** tới thư mục ảnh khóa học
        String courseImagesPath = Paths.get(courseImagesDir).toAbsolutePath().toString();
        registry.addResourceHandler("/course-images/**")
                .addResourceLocations("file:" + courseImagesPath + "/");
        // Map URL /uploads/** tới thư mục ảnh đại diện user-photos
        String avatarPath = Paths.get(avatarDir).toAbsolutePath().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + avatarPath + "/");
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
