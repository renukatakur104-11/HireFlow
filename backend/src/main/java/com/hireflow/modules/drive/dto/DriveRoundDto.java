package com.hireflow.modules.drive.dto;

import com.hireflow.modules.drive.RoundStatusCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriveRoundDto {
    private Long id;
    private Integer roundOrder;
    private String roundName;
    private RoundStatusCode statusCode;
    private String description;
}
