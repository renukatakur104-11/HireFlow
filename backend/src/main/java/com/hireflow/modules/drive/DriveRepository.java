package com.hireflow.modules.drive;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DriveRepository extends JpaRepository<Drive, Long> {

    Optional<Drive> findByIdAndDeletedFalse(Long id);

    @Query("""
            SELECT DISTINCT d FROM Drive d
            LEFT JOIN FETCH d.allowedBranches
            LEFT JOIN FETCH d.requiredSkills
            WHERE d.deleted = false
              AND (:search IS NULL OR LOWER(d.companyName) LIKE :searchLike OR LOWER(d.roleTitle) LIKE :searchLike)
              AND (:status IS NULL OR d.status = :status)
              AND (:minPackage IS NULL OR d.packageLpa >= :minPackage)
              AND (:maxPackage IS NULL OR d.packageLpa <= :maxPackage)
              AND (:minCgpaFilter IS NULL OR d.minCgpa <= :minCgpaFilter)
              AND (:deadlineFrom IS NULL OR d.applicationDeadline >= :deadlineFrom)
              AND (:deadlineTo IS NULL OR d.applicationDeadline <= :deadlineTo)
            """)
    Page<Drive> searchDrives(
            @Param("search") String search,
            @Param("searchLike") String searchLike,
            @Param("status") DriveStatus status,
            @Param("minPackage") BigDecimal minPackage,
            @Param("maxPackage") BigDecimal maxPackage,
            @Param("minCgpaFilter") BigDecimal minCgpaFilter,
            @Param("deadlineFrom") LocalDateTime deadlineFrom,
            @Param("deadlineTo") LocalDateTime deadlineTo,
            Pageable pageable
    );

    @Query("SELECT d FROM Drive d WHERE d.status = 'OPEN' AND d.deleted = false AND d.applicationDeadline > :now")
    List<Drive> findActiveDrives(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(d) FROM Drive d WHERE d.status = :status AND d.deleted = false")
    long countByStatusAndDeletedFalse(@Param("status") DriveStatus status);
}
