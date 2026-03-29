package com.example.doan10diem.service;

import com.example.doan10diem.dto.ChangePasswordDTO;
import com.example.doan10diem.dto.ProfileUpdateDTO;
import com.example.doan10diem.dto.RegisterDTO;
import com.example.doan10diem.model.Role;
import com.example.doan10diem.model.User;
import com.example.doan10diem.model.UserStatus;
import com.example.doan10diem.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.doan10diem.util.PasswordHashUtil.sha256;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${file.avatar.dir:user-photos}")
    private String avatarDir;

    @Value("${app.site-url:http://localhost:8080}")
    private String siteUrl;

    public UserService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public List<User> findByRole(Role role) {
        return userRepository.findByRole(role);
    }

    @Transactional
    public User registerStudent(String username, String rawPassword, String email, String fullName) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username đã tồn tại");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email đã tồn tại");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(sha256(rawPassword));
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole(Role.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        user.setIsVerified(true);

        return userRepository.save(user);
    }

    /**
     * Đăng ký tài khoản Học viên hoặc Giảng viên.
     * TEACHER: bắt buộc CCCD 12 số + link Hồ sơ; isVerified=false, status=PENDING.
     * STUDENT: isVerified=true, status=ACTIVE; các trường teacher để null.
     */
    @Transactional
    public User register(RegisterDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("Username đã tồn tại");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email đã tồn tại");
        }
        if (dto.getPassword() != null && !dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu và xác nhận mật khẩu không khớp");
        }

        // Chỉ validate hồ sơ Giảng viên khi role = TEACHER.
        if (dto.getRole() == Role.TEACHER) {
            String cccd = dto.getCccdNumber() != null ? dto.getCccdNumber().trim() : null;
            if (cccd == null || cccd.length() != 12 || !cccd.matches("^[0-9]{12}$")) {
                throw new IllegalArgumentException("Giảng viên bắt buộc phải nhập CCCD đúng 12 số!");
            }
            String portfolio = dto.getPortfolioUrl() != null ? dto.getPortfolioUrl().trim() : null;
            if (portfolio == null || portfolio.isEmpty()) {
                throw new IllegalArgumentException("Giảng viên bắt buộc nhập link Hồ sơ (Google Drive chứa CV, Chứng chỉ, Dự án...)!");
            }
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(sha256(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setFullName(dto.getFullName());
        user.setRole(dto.getRole());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setAddress(dto.getAddress());
        user.setOccupation(dto.getOccupation());

        // Mã xác thực email (UUID)
        String code = UUID.randomUUID().toString();
        user.setVerificationCode(code);
        user.setEmailVerified(false);

        if (dto.getRole() == Role.STUDENT) {
            user.setStatus(UserStatus.PENDING);  // Chặn đăng nhập cho đến khi xác thực mail
            user.setIsVerified(true);
            user.setCccdNumber(null);
            user.setPortfolioUrl(null);
            user.setUniversity(null);
            user.setExperience(null);
        } else {
            user.setStatus(UserStatus.PENDING);  // Vừa chờ xác thực mail, vừa chờ Admin duyệt
            user.setIsVerified(false);
            user.setCccdNumber(dto.getCccdNumber());
            user.setPortfolioUrl(dto.getPortfolioUrl() != null ? dto.getPortfolioUrl().trim() : null);
            user.setUniversity(dto.getUniversity() != null ? dto.getUniversity().trim() : null);
            user.setExperience(dto.getExperience() != null ? dto.getExperience().trim() : null);
        }

        User savedUser = userRepository.save(user);

        try {
            emailService.sendVerificationEmail(savedUser, siteUrl);
        } catch (MessagingException e) {
            log.warn("Không gửi được email xác thực tới {}: {}", savedUser.getEmail(), e.getMessage());
        }

        return savedUser;
    }

    @Transactional
    public User updateProfile(User user, ProfileUpdateDTO dto) {
        user.setFullName(dto.getFullName());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setAddress(dto.getAddress());
        user.setBio(dto.getBio());
        user.setUniversity(dto.getUniversity());
        if (user.getRole() == Role.TEACHER) {
            if (dto.getExperience() != null) {
                user.setExperience(dto.getExperience().trim().isEmpty() ? null : dto.getExperience().trim());
            }
            // portfolioUrl: không cho phép sửa qua form; chỉ xem, thay đổi qua Admin
            // if (dto.getPortfolioUrl() != null) {
            //     user.setPortfolioUrl(dto.getPortfolioUrl().trim().isEmpty() ? null : dto.getPortfolioUrl().trim());
            // }
        }
        return userRepository.save(user);
    }

    @Transactional
    public User updateAvatar(User user, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Không có file ảnh");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Chỉ chấp nhận file ảnh");
        }
        Path dir = Paths.get(avatarDir).toAbsolutePath();
        Files.createDirectories(dir);
        String ext = contentType.replace("image/", "");
        if (ext.contains("jpeg")) ext = "jpg";
        String filename = UUID.randomUUID().toString() + "." + ext;
        Path target = dir.resolve(filename);
        file.transferTo(target.toFile());
        String avatarUrl = "/uploads/" + filename;
        user.setAvatar(avatarUrl);
        return userRepository.save(user);
    }

    /**
     * Gửi lại email xác thực cho tài khoản chưa verify.
     * @throws IllegalArgumentException nếu email không tồn tại hoặc đã được xác thực
     */
    @Transactional
    public void resendVerificationCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email không tồn tại"));
        if (user.isEmailVerified()) {
            throw new IllegalArgumentException("Tài khoản đã được kích hoạt");
        }
        String code = UUID.randomUUID().toString();
        user.setVerificationCode(code);
        userRepository.save(user);
        try {
            emailService.sendVerificationEmail(user, siteUrl);
        } catch (MessagingException e) {
            log.warn("Không gửi được email xác thực tới {}: {}", user.getEmail(), e.getMessage());
            throw new RuntimeException("Không thể gửi email. Vui lòng thử lại sau.");
        }
    }

    @Transactional
    public void changePassword(User user, ChangePasswordDTO dto) {
        String oldEncoded = sha256(dto.getOldPassword());
        if (!oldEncoded.equals(user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
        }
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới và xác nhận không khớp");
        }
        user.setPassword(sha256(dto.getNewPassword()));
        userRepository.save(user);
    }
}
