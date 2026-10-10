package com.hireflow.modules.admin;

import com.hireflow.common.exception.ResourceNotFoundException;
import com.hireflow.common.response.ApiResponse;
import com.hireflow.modules.admin.dto.BranchPlacementDto;
import com.hireflow.modules.admin.dto.FunnelDataDto;
import com.hireflow.modules.admin.dto.KpiStatsDto;
import com.hireflow.modules.audit.AuditService;
import com.hireflow.modules.student.StudentProfile;
import com.hireflow.modules.student.StudentProfileRepository;
import com.hireflow.modules.student.StudentService;
import com.hireflow.modules.student.dto.StudentProfileDto;
import com.hireflow.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {
    private final AdminStatsService statsService;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentService studentService;
    private final AuditService auditService;

    @GetMapping("/stats/kpi")
    public ResponseEntity<ApiResponse<KpiStatsDto>> kpi() {
        return ResponseEntity.ok(ApiResponse.success(statsService.getKpi()));
    }

    @GetMapping("/stats/funnel")
    public ResponseEntity<ApiResponse<List<FunnelDataDto>>> funnel() {
        return ResponseEntity.ok(ApiResponse.success(statsService.getFunnel()));
    }

    @GetMapping("/stats/branch-placements")
    public ResponseEntity<ApiResponse<List<BranchPlacementDto>>> branchPlacements() {
        return ResponseEntity.ok(ApiResponse.success(statsService.getBranchPlacements()));
    }

    @GetMapping("/students")
    public ResponseEntity<ApiResponse<List<StudentProfileDto>>> listStudents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String branch,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<StudentProfileDto> students = studentProfileRepository.findAll(
                        PageRequest.of(page, size, Sort.by("fullName").ascending()))
                .getContent().stream()
                .filter(sp -> search == null || sp.getFullName().toLowerCase().contains(search.toLowerCase()) || sp.getUser().getEmail().toLowerCase().contains(search.toLowerCase()))
                .filter(sp -> branch == null || branch.equalsIgnoreCase(sp.getBranch()))
                .map(studentService::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(students));
    }

    @GetMapping("/students/{id}")
    public ResponseEntity<ApiResponse<StudentProfileDto>> getStudent(@PathVariable Long id) {
        StudentProfile sp = studentProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));
        return ResponseEntity.ok(ApiResponse.success(studentService.toDto(sp)));
    }

    @GetMapping("/students/{id}/resume")
    public ResponseEntity<org.springframework.core.io.Resource> getStudentResume(@PathVariable Long id) throws java.net.MalformedURLException {
        StudentProfile sp = studentProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));
        if (sp.getResumeFilePath() == null) {
            return ResponseEntity.notFound().build();
        }
        java.nio.file.Path filePath = studentService.getResumePath(sp.getResumeFilePath());
        org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(filePath.toUri());
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        String originalName = sp.getResumeOriginalName() != null ? sp.getResumeOriginalName() : "resume.pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + originalName + "\"")
                .body(resource);
    }

    @GetMapping("/students/export")
    public ResponseEntity<InputStreamResource> exportStudents() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(baos, true, StandardCharsets.UTF_8);
        pw.println("Name,Email,Branch,Year,CGPA,Backlogs,Skills,ProfileComplete,HasResume");
        studentProfileRepository.findAll().forEach(sp -> {
            String skills = sp.getSkills().stream().map(s -> s.getName()).collect(Collectors.joining("|"));
            pw.printf("\"%s\",\"%s\",\"%s\",%d,%.2f,%d,\"%s\",%s,%s%n",
                    sp.getFullName(), sp.getUser().getEmail(), sp.getBranch(),
                    sp.getGraduationYear() != null ? sp.getGraduationYear() : 0,
                    sp.getCgpa() != null ? sp.getCgpa() : java.math.BigDecimal.ZERO,
                    sp.getBacklogs() != null ? sp.getBacklogs() : 0,
                    skills, sp.isProfileComplete(), sp.getResumeFilePath() != null);
        });
        pw.flush();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=students.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new InputStreamResource(new ByteArrayInputStream(baos.toByteArray())));
    }

    @PostMapping(value = "/students/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<com.hireflow.modules.student.dto.BulkImportResponse>> importStudents(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) throws java.io.IOException {
        com.hireflow.modules.student.dto.BulkImportResponse response = studentService.bulkImportStudents(file);
        if (principal != null) {
            auditService.logStudentBulkImported(principal.getId(), response.getTotalRows(),
                    response.getSuccessfulCount(), response.getFailedCount(),
                    request != null ? request.getRemoteAddr() : null);
        }
        return ResponseEntity.ok(ApiResponse.success(response, "Bulk import completed"));
    }

    @GetMapping("/students/template")
    public ResponseEntity<InputStreamResource> downloadTemplate() {
        String templateContent = "Name,Email,Phone,Branch,GraduationYear,CGPA,Backlogs,Skills,Password\n" +
                "Rahul Verma,rahul.verma@college.edu,9876543210,Computer Science,2026,8.5,0,Java|React|MySQL,Student@123\n" +
                "Ananya Roy,ananya.roy@college.edu,9876543211,Information Technology,2026,7.9,0,Python|Data Structures,Student@123\n";
        ByteArrayInputStream bais = new ByteArrayInputStream(templateContent.getBytes(StandardCharsets.UTF_8));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=students_import_template.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new InputStreamResource(bais));
    }
}

