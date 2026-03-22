package ru.haritonenko.eventmanager.location.domain.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import ru.haritonenko.eventmanager.AbstractIntegrationTest;
import ru.haritonenko.eventmanager.location.api.dto.filter.EventLocationSearchFilter;
import ru.haritonenko.eventmanager.location.domain.EventLocation;
import ru.haritonenko.eventmanager.location.domain.db.entity.EventLocationEntity;
import ru.haritonenko.eventmanager.location.domain.db.repository.EventLocationRepository;
import ru.haritonenko.eventmanager.location.domain.exception.LocationCountPlacesException;
import ru.haritonenko.eventmanager.location.domain.exception.LocationNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EventLocationServiceIntegrationTest extends AbstractIntegrationTest {

    @Value("${app.location.default-page-size}")
    private int defaultPageSize;

    @Autowired
    private EventLocationService locationService;

    @Autowired
    private EventLocationRepository locationRepository;

    // happy paths

    @Transactional
    @Test
    void shouldSuccessfullyGetLocationById() {
        EventLocationEntity dummyLocationEntity = getSavedDummyLocation();
        EventLocation foundLocation = locationService.getLocationById(dummyLocationEntity.getId());

        assertEquals(dummyLocationEntity.getId(), foundLocation.id());
        assertEquals(dummyLocationEntity.getName(), foundLocation.name());
        assertEquals(dummyLocationEntity.getCapacity(), foundLocation.capacity());
        assertEquals(dummyLocationEntity.getAddress(), foundLocation.address());
        assertEquals(dummyLocationEntity.getDescription(), foundLocation.description());
    }

    @Transactional
    @Test
    void shouldSuccessfullyCreateLocation() {

        EventLocation eventLocationToCreate = EventLocation.builder()
                .name("test-location-1")
                .address("test-address-1")
                .capacity(100)
                .description("test-description-1")
                .build();

        EventLocation createdLocation = locationService.createLocation(eventLocationToCreate);
        Long createdLocationId = createdLocation.id();

        assertNotNull(createdLocationId);
        assertEquals(eventLocationToCreate.name(), createdLocation.name());
        assertEquals(eventLocationToCreate.address(), createdLocation.address());
        assertEquals(eventLocationToCreate.capacity(), createdLocation.capacity());
        assertEquals(eventLocationToCreate.description(), createdLocation.description());

        EventLocationEntity foundLocationEntity = locationRepository.findById(createdLocationId)
                .orElseThrow();

        assertNotNull(foundLocationEntity.getId());
        assertNotNull(foundLocationEntity.getEvents());
        assertTrue(foundLocationEntity.getEvents().isEmpty());
        assertEquals(eventLocationToCreate.name(), foundLocationEntity.getName());
        assertEquals(eventLocationToCreate.capacity(), foundLocationEntity.getCapacity());
        assertEquals(eventLocationToCreate.address(), foundLocationEntity.getAddress());
        assertEquals(eventLocationToCreate.description(), foundLocationEntity.getDescription());
    }

    @Transactional
    @Test
    void shouldSuccessfullyUpdateLocation() {
        EventLocationEntity dummyLocationEntity = getSavedDummyLocation();
        Long dummyLocationEntityId = dummyLocationEntity.getId();

        assertNotNull(dummyLocationEntityId);

        EventLocation locationToUpdate = getLocationToUpdate();
        EventLocation updatedLocationDomain = locationService.
                updateLocation(dummyLocationEntityId, locationToUpdate);

        assertNotNull(updatedLocationDomain.id());
        assertEquals(locationToUpdate.name(), updatedLocationDomain.name());
        assertEquals(locationToUpdate.address(), updatedLocationDomain.address());
        assertEquals(locationToUpdate.capacity(), updatedLocationDomain.capacity());
        assertEquals(locationToUpdate.description(), updatedLocationDomain.description());

        EventLocationEntity updatedLocationEntity = locationRepository.findById(
                dummyLocationEntityId).orElseThrow();

        assertEquals(locationToUpdate.name(), updatedLocationEntity.getName());
        assertEquals(locationToUpdate.capacity(), updatedLocationEntity.getCapacity());
        assertEquals(locationToUpdate.address(), updatedLocationEntity.getAddress());
        assertEquals(locationToUpdate.description(), updatedLocationEntity.getDescription());
    }

    @Transactional
    @Test
    void shouldSuccessfullyDeleteLocationById() {

        EventLocationEntity dummyLocationEntity = getSavedDummyLocation();
        Long dummyLocationEntityId = dummyLocationEntity.getId();

        assertNotNull(dummyLocationEntityId);

        locationService.deleteLocationById(dummyLocationEntityId);
        Optional<EventLocationEntity> foundLocationEntity = locationRepository.
                findById(dummyLocationEntityId);

        assertTrue(foundLocationEntity.isEmpty());

        LocationNotFoundException exception = assertThrows(LocationNotFoundException.class,
                () -> locationService.getLocationById(dummyLocationEntityId)
        );
        assertEquals("No found location by id = %s".formatted(dummyLocationEntityId), exception.getMessage());

    }

    @Transactional
    @Test
    void shouldReturnAllLocationsUsingDefaultPagingWhenPageSizeAndPageNumberAreNull() {

        List<EventLocationEntity> locationEntityList = getLocationList(10);
        List<EventLocationEntity> locationListWithSavedEntities = locationRepository
                .saveAll(locationEntityList);

        EventLocationSearchFilter searchFilter = EventLocationSearchFilter.builder()
                .name(null)
                .address(null)
                .pageNumber(null)
                .pageSize(null)
                .build();

        List<EventLocation> allLocations = locationService.getAllLocations(searchFilter);
        allLocations.forEach(location -> {
            var foundLocationEntity = locationListWithSavedEntities.stream()
                    .filter(locationEntity ->
                            locationEntity.getId().equals(location.id()))
                    .findFirst()
                    .orElseThrow();

            assertNotNull(foundLocationEntity.getId());
            assertNotNull(foundLocationEntity.getEvents());
            assertEquals(foundLocationEntity.getName(), location.name());
            assertEquals(foundLocationEntity.getCapacity(), location.capacity());
            assertEquals(foundLocationEntity.getAddress(), location.address());
            assertEquals(foundLocationEntity.getDescription(), location.description());

        });

        assertEquals(defaultPageSize, allLocations.size());
    }

    @Transactional
    @Test
    void shouldReturnFilteredLocationsWhenNameIsPassed() {
        List<EventLocationEntity> locationEntityList = getLocationList(10);
        EventLocationEntity firstLocationEntityWithCheckedName = EventLocationEntity.builder()
                .name("checked-location-name")
                .address("test-address-12345")
                .capacity(100)
                .description("test-description-12345")
                .events(new ArrayList<>())
                .build();
        EventLocationEntity secondLocationEntityWithCheckedName = EventLocationEntity.builder()
                .name("checked-location-name")
                .address("test-address-123456")
                .capacity(100)
                .description("test-description-123456")
                .events(new ArrayList<>())
                .build();
        locationEntityList.add(firstLocationEntityWithCheckedName);
        locationEntityList.add(secondLocationEntityWithCheckedName);

        locationRepository.saveAll(locationEntityList);

        EventLocationSearchFilter searchFilter = EventLocationSearchFilter.builder()
                .name("checked-location-name")
                .address(null)
                .pageNumber(null)
                .pageSize(null)
                .build();

        List<EventLocation> locationsWithFilter = locationService
                .getAllLocations(searchFilter);

        assertEquals(2, locationsWithFilter.size());
        assertNotEquals(locationsWithFilter.get(0).id(),locationsWithFilter.get(1).id());
        locationsWithFilter.forEach(location ->
                assertEquals(searchFilter.name(), location.name()));
    }


    //negative paths

    @Transactional
    @Test
    void shouldThrowLocationNotFoundExceptionWhenLocationNotFoundById() {

        Long notExistedLocationId = getNotExistedLocationId();
        LocationNotFoundException exception = assertThrows(LocationNotFoundException.class,
                () -> locationService.getLocationById(notExistedLocationId)
        );
        assertEquals("No found location by id = %s".formatted(notExistedLocationId), exception.getMessage());
    }

    @Transactional
    @Test
    void shouldThrowLocationNotFoundExceptionWhenDeletingMissingLocationById() {

        Long notExistedLocationId = getNotExistedLocationId();
        LocationNotFoundException exception = assertThrows(LocationNotFoundException.class,
                () -> locationService.deleteLocationById(notExistedLocationId)
        );
        assertEquals("No found location by id = %s".formatted(notExistedLocationId),
                exception.getMessage());
    }

    @Transactional
    @Test
    void shouldThrowLocationNotFoundExceptionWhenUpdatingMissingLocation() {

        Long notExistedLocationId = getNotExistedLocationId();
        LocationNotFoundException exception = assertThrows(LocationNotFoundException.class,
                () -> locationService.updateLocation(notExistedLocationId, getLocationToUpdate())
        );
        assertEquals("No found location by id = %s".formatted(notExistedLocationId),
                exception.getMessage());

    }

    @Transactional
    @Test
    void shouldThrowLocationCountPlacesExceptionWhenDecreasingCapacity() {

        EventLocationEntity dummyLocationEntity = getSavedDummyLocation();
        EventLocation locationToUpdate = getLocationToUpdate().toBuilder()
                .capacity(50)
                .build();

        LocationCountPlacesException exception = assertThrows(LocationCountPlacesException.class,
                () -> locationService.updateLocation(dummyLocationEntity.getId(), locationToUpdate)
        );
        assertEquals("You can't decrease location capacity, " +
                "because places might be occupied by users", exception.getMessage());

    }

    // helper methods

    private List<EventLocationEntity> getLocationList(int count) {
        List<EventLocationEntity> resultList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            resultList.add(EventLocationEntity.builder()
                    .name("test-location-" + i)
                    .address("test-address-" + i)
                    .capacity(100 + i)
                    .description("test-description-" + i)
                    .events(new ArrayList<>())
                    .build()
            );
        }
        return resultList;
    }

    private EventLocationEntity getSavedDummyLocation() {
        EventLocationEntity eventLocationEntity = EventLocationEntity.builder()
                .name("test-location-1")
                .address("test-address-1")
                .capacity(100)
                .description("test-description-1")
                .events(new ArrayList<>())
                .build();
        return locationRepository.save(eventLocationEntity);
    }

    private EventLocation getLocationToUpdate() {
        return EventLocation.builder()
                .name("updated-test-location-1")
                .address("updated-test-address-1")
                .capacity(200)
                .description("updated-test-description-1")
                .build();
    }

    private Long getNotExistedLocationId() {
        return Long.MAX_VALUE;
    }
}