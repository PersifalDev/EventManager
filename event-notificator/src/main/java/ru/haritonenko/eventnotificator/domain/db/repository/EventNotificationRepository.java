package ru.haritonenko.eventnotificator.domain.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.haritonenko.eventnotificator.domain.db.entity.EventNotificationEntity;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventNotificationRepository extends JpaRepository<EventNotificationEntity, Long> {

    @Query("""
             SELECT n FROM EventNotificationEntity n
             WHERE n.userId = :userId AND n.read = false
             ORDER BY n.createdAt DESC 
            """)
    List<EventNotificationEntity> findAllUnredNotificationsByUserId(
            @Param("userId") Long id
    );

    @Modifying
    @Query("""
                UPDATE EventNotificationEntity n
                SET n.read = true
                WHERE n.userId = :userId
                  AND n.id IN :ids
                  AND n.read = false
            """)
    void markUserNotificationsAsRead(
            @Param("userId") Long userId,
            @Param("ids") List<Long> ids
    );

    long deleteByCreatedAtBefore(LocalDateTime thresholdTime);
}
