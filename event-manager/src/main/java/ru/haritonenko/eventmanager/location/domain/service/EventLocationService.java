package ru.haritonenko.eventmanager.location.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.haritonenko.eventmanager.location.domain.exception.LocationCountPlacesException;
import ru.haritonenko.eventmanager.location.domain.exception.LocationNotFoundException;
import ru.haritonenko.eventmanager.location.api.dto.filter.EventLocationSearchFilter;
import ru.haritonenko.eventmanager.location.domain.EventLocation;
import ru.haritonenko.eventmanager.location.domain.db.entity.EventLocationEntity;
import ru.haritonenko.eventmanager.location.domain.db.repository.EventLocationRepository;
import ru.haritonenko.eventmanager.location.domain.mapper.EventLocationEntityMapper;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventLocationService {

    private final EventLocationRepository locationRepository;
    private final EventLocationEntityMapper mapper;

    @Value("${app.location.default-page-size}")
    private int defaultPageSize;

    @Value("${app.location.default-page-number}")
    private int defaultPageNumber;

    @Transactional(readOnly = true)
    public List<EventLocation> getAllLocations(
            EventLocationSearchFilter locationFilter
    ) {
        log.info("Searching locations");
        int pageSize = Objects.nonNull(locationFilter.pageSize())
                ? locationFilter.pageSize() : defaultPageSize;
        int pageNumber = Objects.nonNull(locationFilter.pageNumber())
                ? locationFilter.pageNumber() : defaultPageNumber;

        Pageable pageable = Pageable
                .ofSize(pageSize)
                .withPage(pageNumber);

        return locationRepository.searchLocations(
                        locationFilter.name(),
                        locationFilter.address(),
                        pageable
                )
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Transactional
    public EventLocation createLocation(EventLocation eventLocationToCreate) {
        log.info("Creating a location");
        EventLocationEntity newLocation = mapper.toEntity(eventLocationToCreate);
        newLocation.setEvents(new ArrayList<>());
        var savedLocationEntity = locationRepository.save(newLocation);
        log.info("Location was successfully created");
        return mapper.toDomain(savedLocationEntity);
    }

    @Cacheable(value = "locations", key = "#id")
    @Transactional(readOnly = true)
    public EventLocation getLocationById(Long id) {
        log.info("Getting location by id: {}", id);
        var foundLocation = getLocationByIdOrThrow(id);
        log.info("Location was successfully found by id: {}", id);
        return mapper.toDomain(foundLocation);
    }

    @CachePut(value = "locations", key = "#id")
    @Transactional
    public EventLocation updateLocation(Long id, EventLocation eventLocationToUpdate) {
        log.info("Updating location with id: {}", id);
        var location = getLocationByIdOrThrow(id);
        checkNewLocationCapacityMoreOrEqualsOldOrThrow(id, eventLocationToUpdate);

        location.setName(eventLocationToUpdate.name());
        location.setAddress(eventLocationToUpdate.address());
        location.setCapacity(eventLocationToUpdate.capacity());
        location.setDescription(eventLocationToUpdate.description());
        log.info("Location with id: {} was successfully updated", id);

        return mapper.toDomain(location);
    }

    @CacheEvict(value = "locations", key = "#id")
    @Transactional
    public void deleteLocation(Long id) {
        log.info("Deleting location by id: {}", id);
        checkLocationIsExistedByIdOrThrow(id);
        locationRepository.deleteById(id);
        log.info("Location was successfully deleted by id: {}", id);
    }

    private EventLocationEntity getLocationByIdOrThrow(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Error while searching for location by id: {}", id);
                    return new LocationNotFoundException(
                            "No found location by id = %s".formatted(id));
                });
    }

    private void checkLocationIsExistedByIdOrThrow(
            Long id
    ) {
        if (!locationRepository.existsById(id)) {
            log.warn("Error while finding location by id: {}", id);
            throw new LocationNotFoundException(
                    "No found location by id = %s".formatted(id));
        }
    }

    private void checkNewLocationCapacityMoreOrEqualsOldOrThrow(
            Long id,
            EventLocation eventLocationToUpdate
    ) {
        var oldLocation = locationRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Error while getting location by id: {}", id);
                    return new LocationNotFoundException(
                            "No found location by id = %s".formatted(id));
                });

        if (oldLocation.getCapacity() > eventLocationToUpdate.capacity()) {
            log.warn("Error while changing location capacity ");
            throw new LocationCountPlacesException("You can't decrease location capacity, " +
                    "because places might be occupied by users");
        }
    }
}
