package com.example.doan10diem.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    @NotBlank
    @Size(min = 4, max = 50)
    private String username;

    @Column(nullable = false)
    @NotBlank
    private String password;

    @Column(nullable = false, unique = true)
    @NotBlank
    @Email
    private String email;

    @Column(nullable = false, length = 100)
    @NotBlank
    private String fullName;

    /** Đường dẫn file ảnh đại diện (lưu dưới URL /uploads/...) */
    @Column(length = 255)
    private String avatar;

    @Column(length = 20)
    private String phoneNumber;

    @Column(length = 255)
    private String address;

    @Column(length = 100)
    private String occupation;

    /** Giới thiệu ngắn */
    @Column(length = 500)
    private String bio;

    /** Số CCCD (12 số) – dùng cho Giảng viên */
    @Column(length = 12)
    private String cccdNumber;

    /** Link hồ sơ tổng hợp (Google Drive: CV, Chứng chỉ, Dự án...) – Giảng viên */
    @Column(length = 500)
    private String portfolioUrl;

    /** Trường đại học / Cao đẳng / Đơn vị công tác – Giảng viên */
    @Column(length = 200)
    private String university;

    /** Kinh nghiệm giảng dạy – Giảng viên */
    @Column(length = 100)
    private String experience;

    /** Đã xét duyệt (true: Student mặc định, false: Teacher chờ duyệt) */
    @Column(nullable = false)
    private Boolean isVerified = true;

    /** Mã xác thực email (UUID), null sau khi đã verify */
    @Column(length = 36)
    private String verificationCode;

    /** Cờ đánh dấu đã xác thực email chưa */
    @Column(nullable = false)
    private boolean isEmailVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    /**
     * Thông tin thêm cho giảng viên (bio, chuyên môn, ...).
     * Với student có thể để trống.
     */
    @Column(length = 500)
    private String profile;

    /**
     * Tập các khóa học mà student đã ghi danh.
     * Ánh xạ ngược để tiện truy vấn, không bắt buộc sử dụng trong mọi tình huống.
     */
    @OneToMany(mappedBy = "student")
    private Set<Enrollment> enrollments = new HashSet<>();

    @OneToMany(mappedBy = "teacher")
    private Set<Course> teachingCourses = new HashSet<>();

    // --- Constructors ---
    public User() {
        this.isVerified = true;
        this.status = UserStatus.ACTIVE;
        this.enrollments = new HashSet<>();
        this.teachingCourses = new HashSet<>();
    }

    public User(Long id, String username, String password, String email, String fullName, String avatar,
                String phoneNumber, String address, String occupation, String bio, String cccdNumber,
                String portfolioUrl, String university, String experience, Boolean isVerified, Role role,
                UserStatus status, String profile, Set<Enrollment> enrollments, Set<Course> teachingCourses) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
        this.avatar = avatar;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.occupation = occupation;
        this.bio = bio;
        this.cccdNumber = cccdNumber;
        this.portfolioUrl = portfolioUrl;
        this.university = university;
        this.experience = experience;
        this.isVerified = isVerified != null ? isVerified : true;
        this.role = role;
        this.status = status != null ? status : UserStatus.ACTIVE;
        this.profile = profile;
        this.enrollments = enrollments != null ? enrollments : new HashSet<>();
        this.teachingCourses = teachingCourses != null ? teachingCourses : new HashSet<>();
    }

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getCccdNumber() { return cccdNumber; }
    public void setCccdNumber(String cccdNumber) { this.cccdNumber = cccdNumber; }

    public String getPortfolioUrl() { return portfolioUrl; }
    public void setPortfolioUrl(String portfolioUrl) { this.portfolioUrl = portfolioUrl; }

    public String getUniversity() { return university; }
    public void setUniversity(String university) { this.university = university; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified != null ? isVerified : true; }

    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }

    public boolean isEmailVerified() { return isEmailVerified; }
    public void setEmailVerified(boolean emailVerified) { isEmailVerified = emailVerified; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status != null ? status : UserStatus.ACTIVE; }

    public String getProfile() { return profile; }
    public void setProfile(String profile) { this.profile = profile; }

    public Set<Enrollment> getEnrollments() { return enrollments; }
    public void setEnrollments(Set<Enrollment> enrollments) { this.enrollments = enrollments != null ? enrollments : new HashSet<>(); }

    public Set<Course> getTeachingCourses() { return teachingCourses; }
    public void setTeachingCourses(Set<Course> teachingCourses) { this.teachingCourses = teachingCourses != null ? teachingCourses : new HashSet<>(); }

    // --- toString ---
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role=" + role +
                ", status=" + status +
                ", isVerified=" + isVerified +
                '}';
    }
}
