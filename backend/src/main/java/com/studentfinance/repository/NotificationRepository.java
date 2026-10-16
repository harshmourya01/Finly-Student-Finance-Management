package com.studentfinance.repository;

import com.studentfinance.entity.Notification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    long countByUser_IdAndReadStatusFalse(Long userId);
    boolean existsByUser_IdAndDeduplicationKey(Long userId, String deduplicationKey);
    Optional<Notification> findByIdAndUser_Id(Long id, Long userId);

    @Modifying
    @Query("update Notification n set n.readStatus = true where n.user.id = :userId and n.readStatus = false")
    int markAllRead(@Param("userId") Long userId);
}
