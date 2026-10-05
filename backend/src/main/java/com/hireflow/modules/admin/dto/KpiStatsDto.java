package com.hireflow.modules.admin.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpiStatsDto {
    private long totalStudents;
    private long activeDrives;
    private long totalApplications;
    private long selectedStudents;
    private double selectionRate;
}
