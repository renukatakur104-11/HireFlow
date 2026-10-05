package com.hireflow.modules.admin.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunnelDataDto {
    private String status;
    private long count;
}
