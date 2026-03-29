package com.example.doan10diem.dto;

import com.example.doan10diem.model.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO cho form đăng ký (Học viên / Giảng viên).
 * Getter/Setter thủ công để đảm bảo compile khi Lombok chưa xử lý.
 */
public class RegisterDTO {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100)
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Username không được để trống")
    @Size(min = 4, max = 50)
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 4, message = "Mật khẩu tối thiểu 4 ký tự")
    private String password;

    private String confirmPassword;

    @NotNull(message = "Vui lòng chọn vai trò")
    private Role role;

    @Size(max = 20)
    private String phoneNumber;

    @Size(max = 255)
    private String address;

    @Size(max = 100)
    private String occupation;

    /** CCCD 12 số – chỉ bắt buộc khi role = TEACHER (validate trong UserService). */
    private String cccdNumber;

    /** Link hồ sơ tổng hợp (Google Drive) – chỉ dùng khi role = TEACHER. */
    @Size(max = 500)
    private String portfolioUrl;

    /** Trường đại học / Đơn vị công tác – chỉ dùng khi role = TEACHER. */
    @Size(max = 200)
    private String university;

    /** Kinh nghiệm giảng dạy – chỉ dùng khi role = TEACHER. */
    @Size(max = 100)
    private String experience;

    // --- Constructors ---
    public RegisterDTO() {
    }

    public RegisterDTO(String fullName, String email, String username, String password, String confirmPassword,
                        String phoneNumber, String address, Role role, String cccdNumber, String portfolioUrl,
                        String university, String experience) {
        this.fullName = fullName;
        this.email = email;
        this.username = username;
        this.password = password;
        this.confirmPassword = confirmPassword;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.occupation = null;
        this.role = role;
        this.cccdNumber = cccdNumber;
        this.portfolioUrl = portfolioUrl;
        this.university = university;
        this.experience = experience;
    }

    // --- Getters & Setters ---
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }

    public String getCccdNumber() { return cccdNumber; }
    public void setCccdNumber(String cccdNumber) { this.cccdNumber = cccdNumber; }

    public String getPortfolioUrl() { return portfolioUrl; }
    public void setPortfolioUrl(String portfolioUrl) { this.portfolioUrl = portfolioUrl; }

    public String getUniversity() { return university; }
    public void setUniversity(String university) { this.university = university; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }
}
