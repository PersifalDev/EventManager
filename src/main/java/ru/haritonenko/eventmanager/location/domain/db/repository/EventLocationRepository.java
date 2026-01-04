package ru.haritonenko.eventmanager.location.domain.db.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.haritonenko.eventmanager.location.domain.db.entity.EventLocationEntity;

import java.util.List;

@Repository
public interface EventLocationRepository extends JpaRepository<EventLocationEntity, Integer> {

    @Transactional(readOnly = true)
    @Query("""
                SELECT l FROM EventLocationEntity l
                WHERE (:name IS NULL OR l.name = :name)
                AND (:address IS NULL OR l.address = :address)
            """)
    List<EventLocationEntity> searchBooks(
            String name,
            String address,
            Pageable pageable
    );

    @Transactional
    @Modifying
    @Query("""
                UPDATE EventLocationEntity l
                SET l.name = :name,
                    l.address = :address,
                    l.capacity = :capacity,
                    l.description = :description
               WHERE l.id = :id
            """)
    void updateLocation(
            @Param("id") Integer id,
            @Param("name") String name,
            @Param("address") String address,
            @Param("capacity") Integer capacity,
            @Param("description") String description
    );
}
