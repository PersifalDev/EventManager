package ru.haritonenko.eventmanager.location.domain.db.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Pageable;
import ru.haritonenko.eventmanager.location.domain.db.AbstractJpaTest;
import ru.haritonenko.eventmanager.location.domain.db.entity.EventLocationEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class EventLocationRepositoryTest extends AbstractJpaTest {

    private static final int DEFAULT_FIRST_PAGE_NUMBER = 0;
    private static final int DEFAULT_SECOND_PAGE_NUMBER = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;

    @Autowired
    private EventLocationRepository eventLocationRepository;

    private EventLocationEntity testLocation;

    @BeforeEach
    void setUp() {
        testLocation = EventLocationEntity.builder()
                .name("test-location")
                .address("test-address")
                .capacity(100)
                .description("test-description")
                .events(new ArrayList<>())
                .build();
    }

    @Test
    void shouldSaveLocationAndGenerateId() {

        EventLocationEntity savedLocation = saveDummyLocation(testLocation);

        assertNotNull(savedLocation.getId());

        assertEquals("test-location", savedLocation.getName());
        assertEquals("test-address", savedLocation.getAddress());
        assertEquals(100, savedLocation.getCapacity());
        assertEquals("test-description", savedLocation.getDescription());

    }

    @Test
    void shouldFindSavedLocationById() {

        EventLocationEntity savedLocation = saveDummyLocation(testLocation);

        Optional<EventLocationEntity> foundLocationOpt = findDummyLocation(savedLocation.getId());

        assertTrue(foundLocationOpt.isPresent());

        EventLocationEntity foundLocation = foundLocationOpt.get();

        assertEquals("test-location", foundLocation.getName());
        assertEquals("test-address", foundLocation.getAddress());
        assertEquals(100, foundLocation.getCapacity());
        assertEquals("test-description", foundLocation.getDescription());
    }

    @Test
    void shouldReturnEmptyOptionalIfLocationNotFound() {

        EventLocationEntity savedLocation = saveDummyLocation(testLocation);

        Long wrongId = savedLocation.getId() + 1;
        Optional<EventLocationEntity> foundLocationOpt = findDummyLocation(wrongId);

        assertTrue(foundLocationOpt.isEmpty());
    }

    @Test
    void shouldSuccessfullyDeleteLocation() {

        EventLocationEntity savedLocation = saveDummyLocation(testLocation);

        Optional<EventLocationEntity> foundLocationFirstTime = findDummyLocation(savedLocation.getId());

        assertTrue(foundLocationFirstTime.isPresent());

        eventLocationRepository.deleteById(savedLocation.getId());
        Optional<EventLocationEntity> foundLocationSecondTime = findDummyLocation(savedLocation.getId());

        assertTrue(foundLocationSecondTime.isEmpty());

    }

    @Test
    void shouldUpdateLocationSuccessfully() {

        EventLocationEntity savedLocation = saveDummyLocation(testLocation);

        EventLocationEntity foundLocationEntity = eventLocationRepository
                .findById(savedLocation.getId()).orElseThrow();

        foundLocationEntity.setName("updated-test-location");
        foundLocationEntity.setAddress("updated-test-address");
        foundLocationEntity.setDescription("updated-test-description");
        foundLocationEntity.setCapacity(200);

        eventLocationRepository.save(foundLocationEntity);

        EventLocationEntity updatedLocationEntity = eventLocationRepository
                .findById(savedLocation.getId()).orElseThrow();

        assertEquals("updated-test-location", updatedLocationEntity.getName());
        assertEquals("updated-test-address", updatedLocationEntity.getAddress());
        assertEquals("updated-test-description", updatedLocationEntity.getDescription());
        assertEquals(200, updatedLocationEntity.getCapacity());

    }

    @Test
    void shouldReturnFirstPageOfLocationsWhenNameAndAddressAreNull() {

        createUniqueLocations(100);

        List<EventLocationEntity> locationEntityList = eventLocationRepository.searchLocations(
                null,
                null,
                getPageable(DEFAULT_PAGE_SIZE, DEFAULT_FIRST_PAGE_NUMBER)
        );
        locationEntityList.forEach(Assertions::assertNotNull);
        assertEquals(DEFAULT_PAGE_SIZE, locationEntityList.size());
    }

    @Test
    void shouldReturnFullFirstPageWithLocationsAndRemainsOnSecondPage() {

        createUniqueLocations(15);

        List<EventLocationEntity> locationEntityFirstPageList = eventLocationRepository.searchLocations(
                null,
                null,
                getPageable(DEFAULT_PAGE_SIZE, DEFAULT_FIRST_PAGE_NUMBER)
        );

        List<EventLocationEntity> locationEntitySecondPageList = eventLocationRepository.searchLocations(
                null,
                null,
                getPageable(DEFAULT_PAGE_SIZE, DEFAULT_SECOND_PAGE_NUMBER)
        );

        locationEntityFirstPageList.forEach(Assertions::assertNotNull);
        assertEquals(DEFAULT_PAGE_SIZE, locationEntityFirstPageList.size());

        locationEntitySecondPageList.forEach(Assertions::assertNotNull);
        assertEquals(5, locationEntitySecondPageList.size());


    }

    @Test
    void shouldReturnDifferentLocationsForDifferentPagesWhenFiltersAreNull() {


        createUniqueLocations(100);

        List<EventLocationEntity> locationEntityFirstList = eventLocationRepository.searchLocations(
                null,
                null,
                getPageable(DEFAULT_PAGE_SIZE, DEFAULT_FIRST_PAGE_NUMBER)
        );
        List<EventLocationEntity> locationEntitySecondList = eventLocationRepository.searchLocations(
                null,
                null,
                getPageable(DEFAULT_PAGE_SIZE, DEFAULT_SECOND_PAGE_NUMBER)
        );

        List<Long> firstPageIds = locationEntityFirstList.stream()
                .map(EventLocationEntity::getId)
                .toList();

        List<Long> secondPageIds = locationEntitySecondList.stream()
                .map(EventLocationEntity::getId)
                .toList();

        assertEquals(DEFAULT_PAGE_SIZE, locationEntityFirstList.size());
        assertEquals(DEFAULT_PAGE_SIZE, locationEntitySecondList.size());

        assertNotEquals(firstPageIds, secondPageIds);

    }

    @Test
    void shouldReturnLocationListOnFirstPageUsingOnlyNameFilter() {


        createUniqueLocations(10);

        List<EventLocationEntity> locationEntityList = eventLocationRepository.searchLocations(
                "test-location-name-1",
                null,
                getPageable(DEFAULT_PAGE_SIZE, DEFAULT_FIRST_PAGE_NUMBER)
        );
        locationEntityList.forEach(Assertions::assertNotNull);
        assertEquals(1, locationEntityList.size());
    }

    @Test
    void shouldReturnLocationListOnFirstPageUsingOnlyAddressFilter() {

        createUniqueLocations(10);

        List<EventLocationEntity> locationEntityList = eventLocationRepository.searchLocations(
                null,
                "test-location-address-1",
                getPageable(DEFAULT_PAGE_SIZE, DEFAULT_FIRST_PAGE_NUMBER)
        );
        locationEntityList.forEach(Assertions::assertNotNull);
        assertEquals(1, locationEntityList.size());
    }

    @Test
    void shouldReturnLocationListOnFirstPageUsingNameAndAddressFilter() {

        createUniqueLocations(100);

        List<EventLocationEntity> locationEntityList = eventLocationRepository.searchLocations(
                "test-location-name-99",
                "test-location-address-99",
                getPageable(DEFAULT_PAGE_SIZE, DEFAULT_FIRST_PAGE_NUMBER)
        );
        locationEntityList.forEach(Assertions::assertNotNull);
        assertEquals(1, locationEntityList.size());
    }

    @Test
    void shouldReturnEmptyLocationListOnFirstPageUsingInvalidNameFilter() {


        createUniqueLocations(100);

        List<EventLocationEntity> locationEntityList = eventLocationRepository.searchLocations(
                "test-location-address-99",
                null,
                getPageable(DEFAULT_PAGE_SIZE, DEFAULT_FIRST_PAGE_NUMBER)
        );
        assertTrue(locationEntityList.isEmpty());
    }

    @Test
    void shouldReturnEmptyLocationListOnFirstPageUsingInvalidAddressFilter() {


        createUniqueLocations(100);

        List<EventLocationEntity> locationEntityList = eventLocationRepository.searchLocations(
                null,
                "test-location-name-99",
                getPageable(DEFAULT_PAGE_SIZE, DEFAULT_FIRST_PAGE_NUMBER)
        );
        assertTrue(locationEntityList.isEmpty());
    }

    @Test
    void shouldReturnEmptyLocationListOnFirstPageUsingValidNameAndInvalidAddressFilter() {

        createUniqueLocations(100);

        List<EventLocationEntity> locationEntityList = eventLocationRepository.searchLocations(
                "test-location-name-99",
                "test-location-address-300",
                getPageable(DEFAULT_PAGE_SIZE, DEFAULT_FIRST_PAGE_NUMBER)
        );

        assertTrue(locationEntityList.isEmpty());
    }

    @Test
    void shouldReturnEmptyLocationListOnFirstPageUsingInvalidNameAndValidAddressFilter() {


        createUniqueLocations(100);

        List<EventLocationEntity> locationEntityList = eventLocationRepository.searchLocations(
                "test-location-name-300",
                "test-location-address-99",
                getPageable(DEFAULT_PAGE_SIZE, DEFAULT_FIRST_PAGE_NUMBER)
        );

        assertTrue(locationEntityList.isEmpty());
    }

    @Test
    void shouldReturnLocationsSortedByIdAsc() {

        createUniqueLocations(100);
        List<EventLocationEntity> locationEntityList = eventLocationRepository.searchLocations(
                null,
                null,
                getPageable(DEFAULT_PAGE_SIZE, DEFAULT_FIRST_PAGE_NUMBER)
        );

        List<Long> locationEntityListIds = locationEntityList.stream()
                .map(EventLocationEntity::getId)
                .toList();

        List<Long> sortedLocationEntityListIds = locationEntityList.stream()
                .sorted(Comparator.comparing(EventLocationEntity::getId))
                .map(EventLocationEntity::getId)
                .toList();

        assertEquals(sortedLocationEntityListIds, locationEntityListIds);

    }


    private EventLocationEntity saveDummyLocation(EventLocationEntity location) {
        return eventLocationRepository.save(location);
    }

    private Optional<EventLocationEntity> findDummyLocation(Long locationId) {
        return eventLocationRepository
                .findById(locationId);
    }

    private void createUniqueLocations(int count) {
        for (int i = 0; i < count; i++) {
            eventLocationRepository.save(
                    EventLocationEntity.builder()
                            .name("test-location-name-" + i)
                            .address("test-location-address-" + i)
                            .capacity(100)
                            .description("test-description-" + i)
                            .events(new ArrayList<>())
                            .build()
            );
        }
    }

    private Pageable getPageable(
            int pageSize,
            int pageNumber
    ) {
        return Pageable
                .ofSize(pageSize)
                .withPage(pageNumber);
    }
}