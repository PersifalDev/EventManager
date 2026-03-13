package ru.haritonenko.eventmanager.location.domain.db.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.haritonenko.eventmanager.location.domain.db.entity.EventLocationEntity;

import java.util.List;

@Repository
public interface EventLocationRepository extends JpaRepository<EventLocationEntity, Long> {

    @Query("""
                SELECT l FROM EventLocationEntity l
                WHERE (:name IS NULL OR l.name = :name)
                AND (:address IS NULL OR l.address = :address)
                ORDER BY l.id asc
            """)
    List<EventLocationEntity> searchLocations(
            @Param("name") String name,
            @Param("address") String address,
            Pageable pageable
    );
}
