package ru.haritonenko.eventmanager.event.registration.domain.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.haritonenko.eventmanager.event.registration.domain.db.entity.EventRegistrationEntity;
import ru.haritonenko.eventmanager.event.registration.domain.status.EventRegistrationStatus;

import java.util.Optional;

@Repository
public interface EventRegistrationRepository extends JpaRepository<EventRegistrationEntity, Integer> {

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
