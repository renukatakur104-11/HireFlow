package com.hireflow.modules.student.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkImportResponse {
    private int totalRows;
    private int successfulCount;
    private int failedCount;
    private List<RowErrorDto> errors;
}
