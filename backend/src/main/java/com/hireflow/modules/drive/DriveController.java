package com.hireflow.modules.drive;

import com.hireflow.common.response.ApiResponse;
import com.hireflow.common.response.PagedResponse;
import com.hireflow.modules.drive.dto.*;
import com.hireflow.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class DriveController {

    private final DriveService driveService;

    // ── Student & Public Drive endpoints ────────────────────────────────────

    @GetMapping("/api/drives")
    public ResponseEntity<ApiResponse<PagedResponse<DriveDto>>> listDrives(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) DriveStatus status,
            @RequestParam(required = false) BigDecimal minPackage,
            @RequestParam(required = false) BigDecimal maxPackage,
            @RequestParam(required = false) BigDecimal maxCgpa,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime deadlineFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime deadlineTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "applicationDeadline") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @AuthenticationPrincipal UserPrincipal principal) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        var result = driveService.listDrives(search, status, minPackage, maxPackage, maxCgpa,
                deadlineFrom, deadlineTo, PageRequest.of(page, size, sort), principal);
        return ResponseEntity.ok(ApiResponse.success(new PagedResponse<>(result)));
    }

    @GetMapping("/api/drives/{id}")
    public ResponseEntity<ApiResponse<DriveDto>> getDrive(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(driveService.getDriveById(id, principal)));
    }

    // ── Admin Drive management endpoints ────────────────────────────────────

    @PostMapping("/api/admin/drives")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DriveDto>> createDrive(
            @Valid @RequestBody DriveCreateRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        DriveDto created = driveService.createDrive(req, principal);
        return ResponseEntity.status(201).body(ApiResponse.success(created, "Drive created successfully"));
    }

    @PutMapping("/api/admin/drives/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DriveDto>> updateDrive(
            @PathVariable Long id,
            @Valid @RequestBody DriveCreateRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(driveService.updateDrive(id, req, principal), "Drive updated"));
    }

    @DeleteMapping("/api/admin/drives/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteDrive(@PathVariable Long id) {
        driveService.deleteDrive(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Drive deleted"));
    }

    @PatchMapping("/api/admin/drives/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DriveDto>> updateDriveStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        DriveStatus newStatus = DriveStatus.valueOf(body.get("status").toUpperCase());
        return ResponseEntity.ok(ApiResponse.success(driveService.updateStatus(id, newStatus), "Status updated"));
    }

    @GetMapping("/api/admin/drives")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<DriveDto>>> adminListDrives(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) DriveStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        Sort sort = Sort.by("createdAt").descending();
        var result = driveService.listDrives(search, status, null, null, null, null, null,
                PageRequest.of(page, size, sort), principal);
        return ResponseEntity.ok(ApiResponse.success(new PagedResponse<>(result)));
    }
}
