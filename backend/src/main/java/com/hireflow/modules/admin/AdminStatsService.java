package com.hireflow.modules.admin;

import com.hireflow.modules.admin.dto.FunnelDataDto;
import com.hireflow.modules.admin.dto.KpiStatsDto;
import com.hireflow.modules.application.ApplicationRepository;
import com.hireflow.modules.application.ApplicationStatus;
import com.hireflow.modules.drive.DriveRepository;
import com.hireflow.modules.drive.DriveStatus;
import com.hireflow.modules.user.Role;
import com.hireflow.modules.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminStatsService {
    private final UserRepository userRepository;
    private final DriveRepository driveRepository;
    private final ApplicationRepository applicationRepository;

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
}
