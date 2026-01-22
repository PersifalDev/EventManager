package ru.haritonenko.eventmanager.event.domain.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.haritonenko.eventmanager.event.domain.status.EventStatus;
import ru.haritonenko.eventmanager.event.domain.db.entity.EventEntity;

import org.springframework.data.domain.Pageable;
import ru.haritonenko.eventmanager.event.registration.domain.status.EventRegistrationStatus;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, Long> {

    @Query("""
             SELECT e FROM EventEntity e
             WHERE e.owner.id = :id  
            """)
    List<EventEntity> searchCreatedEventsByUserId(
            @Param("id") Long ownerId,
            Pageable pageable
    );

    @Query(value = """
            SELECT r.event
            FROM EventRegistrationEntity r
            WHERE r.user.id = :id AND r.status = :status
            """)
    List<EventEntity> searchBookedEventsByUserId(
            @Param("id") Long userId,
            @Param("status") EventRegistrationStatus status,
            Pageable pageable
    );

    @Query("""
                SELECT e FROM EventEntity e
                WHERE (:name IS NULL OR e.name = :name)
                AND (:placesMin IS NULL OR e.maxPlaces >= :placesMin)
                AND (:placesMax IS NULL OR e.maxPlaces <= :placesMax)
                AND (:dateStartAfter IS NULL OR e.date >= :dateStartAfter)
                AND (:dateStartBefore IS NULL OR e.date <= :dateStartBefore)
                AND (:costMin IS NULL OR e.cost >= :costMin)
                AND (:costMax IS NULL OR e.cost <= :costMax)
                AND (:durationMin IS NULL OR e.duration >= :durationMin)
                AND (:durationMax IS NULL OR e.duration <= :durationMax)
                AND (:locationId IS NULL OR e.location.id = :locationId)
                AND (:eventStatus IS NULL OR e.status = :eventStatus)
            """)
    List<EventEntity> searchEventsWithFilter(
            @Param("name") String name,
            @Param("placesMin") Integer minPlacesCount,
            @Param("placesMax") Integer maxPlacesCount,
            @Param("dateStartAfter") String firstDate,
            @Param("dateStartBefore") String lastDate,
            @Param("costMin") BigDecimal costMin,
            @Param("costMax") BigDecimal costMax,
            @Param("durationMin") Integer durationMin,
            @Param("durationMax") Integer durationMax,
            @Param("locationId") Long locationId,
            @Param("eventStatus") EventStatus status,
            Pageable pageable
    );


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
                UPDATE EventEntity e
                SET e.occupiedPlaces = e.occupiedPlaces + 1
                WHERE e.id = :eventId AND e.occupiedPlaces < e.maxPlaces
            """)
    int incOccupiedPlaces(@Param("eventId") Long eventId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
                UPDATE EventEntity e
                SET e.occupiedPlaces = e.occupiedPlaces - 1
                WHERE e.id = :eventId AND e.occupiedPlaces > 0
            """)
    int decOccupiedPlaces(@Param("eventId") Long eventId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
                UPDATE EventEntity e
                SET e.occupiedPlaces = 0
                WHERE e.id = :eventId
            """)
    int resetOccupiedPlaces(@Param("eventId") Long eventId);

    List<EventEntity> findByStatusIn(Collection<EventStatus> statuses);
}
