package com.hireflow.modules.auth.dto;

import com.hireflow.modules.user.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponse {
    private Long id;
    private String email;
    private Role role;
    private String fullName;
    private String branch;
    private BigDecimal cgpa;
    private boolean profileComplete;
    private boolean hasResume;
}
