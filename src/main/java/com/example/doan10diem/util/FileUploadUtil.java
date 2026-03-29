package com.example.doan10diem.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Tiện ích hỗ trợ upload file.
 */
public class FileUploadUtil {

    private FileUploadUtil() {
    }

    /**
     * Lưu file vào thư mục chỉ định và trả về đường dẫn tương đối để lưu vào database.
     *
     * @param uploadDir Thư mục upload (ví dụ: "course-images")
     * @param fileName Tên file (có thể null, sẽ tự động tạo UUID)
     * @param multipartFile File cần upload
     * @return Đường dẫn tương đối (ví dụ: "/course-images/123/image.jpg") hoặc null nếu file rỗng
     * @throws IOException Nếu có lỗi khi lưu file
     */
    public static String saveFile(String uploadDir, String fileName, MultipartFile multipartFile) throws IOException {
        if (multipartFile == null || multipartFile.isEmpty()) {
            return null;
        }

        // Tạo thư mục upload nếu chưa có
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Tạo tên file unique nếu chưa có
        String finalFileName = fileName;
        if (finalFileName == null || finalFileName.isEmpty()) {
            String originalFilename = multipartFile.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            finalFileName = UUID.randomUUID().toString() + extension;
        }

        // Lưu file
        Path filePath = uploadPath.resolve(finalFileName);
        Files.copy(multipartFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Trả về đường dẫn relative để lưu vào database
        return "/" + uploadDir.replace("\\", "/") + "/" + finalFileName;
    }

    /**
     * Lưu file vào thư mục con (ví dụ: course-images/{courseId}/image.jpg).
     *
     * @param uploadDir Thư mục upload gốc (ví dụ: "course-images")
     * @param subDir Tên thư mục con (ví dụ: courseId.toString())
     * @param fileName Tên file (có thể null, sẽ tự động tạo UUID)
     * @param multipartFile File cần upload
     * @return Đường dẫn tương đối (ví dụ: "/course-images/123/image.jpg") hoặc null nếu file rỗng
     * @throws IOException Nếu có lỗi khi lưu file
     */
    public static String saveFile(String uploadDir, String subDir, String fileName, MultipartFile multipartFile) throws IOException {
        if (multipartFile == null || multipartFile.isEmpty()) {
            return null;
        }

        // Tạo đường dẫn đầy đủ: uploadDir/subDir
        String fullUploadDir = subDir != null && !subDir.isEmpty() 
            ? uploadDir + "/" + subDir 
            : uploadDir;

        return saveFile(fullUploadDir, fileName, multipartFile);
    }
}
