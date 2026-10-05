package com.hireflow.modules.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, Long> {

    Page<NotificationRecipient> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndReadFalse(Long userId);

    Optional<NotificationRecipient> findByNotificationIdAndUserId(Long notificationId, Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE NotificationRecipient nr SET nr.read = true, nr.readAt = :now WHERE nr.user.id = :userId AND nr.read = false")
    int markAllReadForUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
