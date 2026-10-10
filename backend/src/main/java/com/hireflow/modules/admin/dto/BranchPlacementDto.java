package com.hireflow.modules.admin.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchPlacementDto {
    private String branch;
    private long totalStudents;
    private long placedStudents;
    private double placementRate;
}
