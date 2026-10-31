package com.campus.events.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserProfileDto {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 150, message = "Name must be between 2 and 150 characters")
    private String fullName;

    @Size(max = 50, message = "Student ID cannot exceed 50 characters")
    private String studentIdNumber;

    @Size(max = 25, message = "Phone cannot exceed 25 characters")
    private String phone;

    @Size(max = 100, message = "Department cannot exceed 100 characters")
    private String department;

    public UserProfileDto() {
    }

    public UserProfileDto(String fullName, String studentIdNumber, String phone, String department) {
        this.fullName = fullName;
        this.studentIdNumber = studentIdNumber;
        this.phone = phone;
        this.department = department;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getStudentIdNumber() {
        return studentIdNumber;
    }

    public void setStudentIdNumber(String studentIdNumber) {
        this.studentIdNumber = studentIdNumber;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}

