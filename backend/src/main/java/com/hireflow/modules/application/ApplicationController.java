package com.hireflow.modules.application;

import com.hireflow.common.response.ApiResponse;
import com.hireflow.common.response.PagedResponse;
import com.hireflow.modules.application.dto.*;
import com.hireflow.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    // Student: apply to a drive
    @PostMapping("/api/drives/{driveId}/apply")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<ApplicationDto>> apply(
            @PathVariable Long driveId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(201)
                .body(ApiResponse.success(applicationService.apply(driveId, principal), "Application submitted successfully"));
    }

    // Student: withdraw application
    @PostMapping("/api/applications/{id}/withdraw")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<ApplicationDto>> withdraw(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(applicationService.withdraw(id, principal), "Application withdrawn"));
    }

    // Student: my applications
    @GetMapping("/api/applications/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<ApplicationDto>>> myApplications(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(applicationService.getMyApplications(principal)));
    }

    // Admin: list applicants for a drive
    @GetMapping("/api/admin/drives/{driveId}/applicants")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<ApplicationDto>>> driveApplicants(
            @PathVariable Long driveId,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = applicationService.getDriveApplicants(driveId, status,
                PageRequest.of(page, size, Sort.by("appliedAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(new PagedResponse<>(result)));
    }

    // Admin: update single application status
    @PatchMapping("/api/admin/applications/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ApplicationDto>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationStatusUpdateRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(applicationService.updateStatus(id, req, principal), "Status updated"));
    }

    // Admin: bulk status update
    @PostMapping("/api/admin/applications/bulk-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> bulkUpdateStatus(
            @Valid @RequestBody BulkStatusUpdateRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        int count = applicationService.bulkUpdateStatus(req, principal);
        return ResponseEntity.ok(ApiResponse.success(count + " application(s) updated"));
    }

    // Admin: application history
    @GetMapping("/api/admin/applications/{id}/history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ApplicationStatusHistoryDto>>> history(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(applicationService.getHistory(id)));
    }
}
