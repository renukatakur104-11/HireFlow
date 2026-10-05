package com.hireflow.modules.student.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfileUpdateRequest {

    @Size(min = 2, max = 150, message = "Full name must be between 2 and 150 characters")
    private String fullName;

    @Pattern(regexp = "^[0-9+()\\-\\s]{10,20}$", message = "Please enter a valid phone number")
    private String phone;

    private String branch;

    @Min(value = 2020, message = "Graduation year must be 2020 or later")
    @Max(value = 2035, message = "Graduation year must be realistic")
    private Integer graduationYear;

    @DecimalMin(value = "0.00", message = "CGPA cannot be negative")
    @DecimalMax(value = "10.00", message = "CGPA cannot exceed 10.00")
    private BigDecimal cgpa;

    @Min(value = 0, message = "Backlogs cannot be negative")
    private Integer backlogs;

    private List<String> skills;
}
