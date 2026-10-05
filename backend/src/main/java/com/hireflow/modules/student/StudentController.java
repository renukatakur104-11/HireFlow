package com.hireflow.modules.student;

import com.hireflow.common.response.ApiResponse;
import com.hireflow.modules.storage.FileStorageService;
import com.hireflow.modules.student.dto.ResumeUploadResponse;
import com.hireflow.modules.student.dto.StudentProfileDto;
import com.hireflow.modules.student.dto.StudentProfileUpdateRequest;
import com.hireflow.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final FileStorageService fileStorageService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<StudentProfileDto>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(studentService.getMyProfile(principal)));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<StudentProfileDto>> updateMyProfile(
            @Valid @RequestBody StudentProfileUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(studentService.updateMyProfile(request, principal), "Profile updated successfully"));
    }

    @PostMapping(value = "/me/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<ResumeUploadResponse>> uploadResume(
            @RequestParam("resume") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) throws IOException {
        ResumeUploadResponse response = studentService.uploadResume(file, principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Resume uploaded successfully"));
    }

    @GetMapping("/me/resume")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Resource> downloadMyResume(
            @AuthenticationPrincipal UserPrincipal principal) throws MalformedURLException {
        StudentProfile profile = studentService.getProfileEntityByUserId(principal.getId());
        if (profile.getResumeFilePath() == null) {
            return ResponseEntity.notFound().build();
        }

        Path filePath = fileStorageService.getResumePath(profile.getResumeFilePath());
        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String originalName = profile.getResumeOriginalName() != null ? profile.getResumeOriginalName() : "resume.pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + originalName + "\"")
                .body(resource);
    }
}
