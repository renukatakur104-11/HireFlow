package com.hireflow.modules.admin;

import com.hireflow.modules.admin.dto.BranchPlacementDto;
import com.hireflow.modules.admin.dto.FunnelDataDto;
import com.hireflow.modules.admin.dto.KpiStatsDto;
import com.hireflow.modules.application.Application;
import com.hireflow.modules.application.ApplicationRepository;
import com.hireflow.modules.application.ApplicationStatus;
import com.hireflow.modules.drive.DriveRepository;
import com.hireflow.modules.drive.DriveStatus;
import com.hireflow.modules.student.StudentProfile;
import com.hireflow.modules.student.StudentProfileRepository;
import com.hireflow.modules.user.Role;
import com.hireflow.modules.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminStatsService {
    private final UserRepository userRepository;
    private final DriveRepository driveRepository;
    private final ApplicationRepository applicationRepository;
    private final StudentProfileRepository studentProfileRepository;

    @Transactional(readOnly = true)
    public KpiStatsDto getKpi() {
        long totalStudents = userRepository.countByRole(Role.STUDENT);
        long activeDrives = driveRepository.countByStatusAndDeletedFalse(DriveStatus.OPEN);
        long totalApplications = applicationRepository.count();
        long selected = applicationRepository.countByCurrentStatus(ApplicationStatus.SELECTED);
        double rate = totalApplications > 0 ? (double) selected / totalApplications * 100 : 0;
        return KpiStatsDto.builder().totalStudents(totalStudents).activeDrives(activeDrives)
                .totalApplications(totalApplications).selectedStudents(selected)
                .selectionRate(Math.round(rate * 10.0) / 10.0).build();
    }

    @Transactional(readOnly = true)
    public List<FunnelDataDto> getFunnel() {
        return Arrays.stream(ApplicationStatus.values())
                .map(s -> FunnelDataDto.builder().status(s.name()).count(applicationRepository.countByCurrentStatus(s)).build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BranchPlacementDto> getBranchPlacements() {
        List<StudentProfile> allProfiles = studentProfileRepository.findAll();
        Map<String, List<StudentProfile>> byBranch = allProfiles.stream()
                .filter(sp -> sp.getBranch() != null && !sp.getBranch().isBlank())
                .collect(Collectors.groupingBy(StudentProfile::getBranch));

        List<Application> selectedApps = applicationRepository.findAll().stream()
                .filter(a -> a.getCurrentStatus() == ApplicationStatus.SELECTED)
                .collect(Collectors.toList());
        Set<Long> placedProfileIds = selectedApps.stream()
                .map(a -> a.getStudentProfile().getId())
                .collect(Collectors.toSet());

        return byBranch.entrySet().stream()
                .map(entry -> {
                    String branch = entry.getKey();
                    long total = entry.getValue().size();
                    long placed = entry.getValue().stream()
                            .filter(sp -> placedProfileIds.contains(sp.getId()))
                            .count();
                    double rate = total > 0 ? (double) placed / total * 100 : 0;
                    return BranchPlacementDto.builder()
                            .branch(branch)
                            .totalStudents(total)
                            .placedStudents(placed)
                            .placementRate(Math.round(rate * 10.0) / 10.0)
                            .build();
                })
                .sorted(Comparator.comparing(BranchPlacementDto::getBranch))
                .collect(Collectors.toList());
    }
}

