package com.hireflow.modules.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    boolean existsByDriveIdAndStudentProfileId(Long driveId, Long studentProfileId);
    Optional<Application> findByDriveIdAndStudentProfileId(Long driveId, Long studentProfileId);
    List<Application> findByStudentProfileId(Long studentProfileId);
    Page<Application> findByDriveId(Long driveId, Pageable pageable);
    Page<Application> findByDriveIdAndCurrentStatus(Long driveId, ApplicationStatus status, Pageable pageable);

    @Query("SELECT COUNT(a) FROM Application a WHERE a.drive.id = :driveId")
    long countByDriveId(@Param("driveId") Long driveId);

    @Query("SELECT COUNT(a) FROM Application a WHERE a.currentStatus = :status")
    long countByCurrentStatus(@Param("status") ApplicationStatus status);

    @Query("SELECT a FROM Application a JOIN FETCH a.drive JOIN FETCH a.studentProfile WHERE a.studentProfile.id = :profileId ORDER BY a.appliedAt DESC")
    List<Application> findByStudentProfileIdWithDetails(@Param("profileId") Long profileId);
}
