package com.example.doan10diem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProfileUpdateDTO {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100)
    private String fullName;

    @Size(max = 20)
    private String phoneNumber;

    @Size(max = 255)
    private String address;

    @Size(max = 500)
    private String bio;

    @Size(max = 200)
    private String university;

    /** Chỉ dùng cho TEACHER */
    @Size(max = 12)
    private String cccdNumber;

    /** Chỉ dùng cho TEACHER */
    @Size(max = 100)
    private String experience;

    /** Chỉ dùng cho TEACHER */
    @Size(max = 500)
    private String portfolioUrl;

    // --- Constructor không tham số ---
    public ProfileUpdateDTO() {
    }

    // --- Constructor đầy đủ ---
    public ProfileUpdateDTO(String fullName, String phoneNumber, String address, String bio,
                            String university, String cccdNumber, String experience, String portfolioUrl) {
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.bio = bio;
        this.university = university;
        this.cccdNumber = cccdNumber;
        this.experience = experience;
        this.portfolioUrl = portfolioUrl;
    }

    // --- Getters & Setters ---
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getUniversity() {
        return university;
    }

    public void setUniversity(String university) {
        this.university = university;
    }

    public String getCccdNumber() {
        return cccdNumber;
    }

    public void setCccdNumber(String cccdNumber) {
        this.cccdNumber = cccdNumber;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getPortfolioUrl() {
        return portfolioUrl;
    }

    public void setPortfolioUrl(String portfolioUrl) {
        this.portfolioUrl = portfolioUrl;
    }
}
