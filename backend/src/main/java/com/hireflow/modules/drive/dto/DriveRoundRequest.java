package com.hireflow.modules.drive.dto;

import com.hireflow.modules.drive.RoundStatusCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriveRoundRequest {

    @NotNull
    private Integer roundOrder;

    @NotBlank
    private String roundName;

    @NotNull
    private RoundStatusCode statusCode;

    private String description;
}
