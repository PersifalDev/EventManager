package ru.haritonenko.eventmanager.event.registration.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.haritonenko.eventmanager.event.registration.db.entity.EventRegistrationEntity;
import ru.haritonenko.eventmanager.event.registration.status.EventRegistrationStatus;

import java.util.Optional;

@Repository
public interface EventRegistrationRepository extends JpaRepository<EventRegistrationEntity, Integer> {

    @Transactional(readOnly = true)
    Optional<EventRegistrationEntity> findByUserIdAndEventId(Integer userId, Integer eventId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE EventRegistrationEntity r
            SET r.status = :status
            WHERE r.user.id = :userId AND r.event.id = :eventId
            """)
    void updateStatus(
            @Param("userId") Integer userId,
            @Param("eventId") Integer eventId,
            @Param("status") EventRegistrationStatus status
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE EventRegistrationEntity r
            SET r.status = :newStatus
            WHERE r.event.id = :eventId AND r.status = :oldStatus
            """)
    int updateStatusByEventId(
            @Param("eventId") Integer eventId,
            @Param("newStatus") EventRegistrationStatus newStatus,
            @Param("oldStatus") EventRegistrationStatus oldStatus);

}
