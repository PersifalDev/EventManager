package ru.haritonenko.eventmanager.location.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import ru.haritonenko.eventmanager.location.api.dto.filter.EventLocationSearchFilter;
import ru.haritonenko.eventmanager.location.domain.EventLocation;
import ru.haritonenko.eventmanager.location.domain.db.entity.EventLocationEntity;
import ru.haritonenko.eventmanager.location.domain.db.repository.EventLocationRepository;
import ru.haritonenko.eventmanager.location.domain.exception.LocationCountPlacesException;
import ru.haritonenko.eventmanager.location.domain.exception.LocationNotFoundException;
import ru.haritonenko.eventmanager.location.domain.mapper.EventLocationEntityMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventLocationServiceUnitTest {

    @Mock
    private EventLocationRepository locationRepository;

    @Mock
    private EventLocationEntityMapper mapper;

    @InjectMocks
    private EventLocationService eventLocationService;

    private EventLocationEntity locationEntity;
    private EventLocation locationDomain;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(eventLocationService, "defaultPageSize", 20);
        ReflectionTestUtils.setField(eventLocationService, "defaultPageNumber", 0);

        locationEntity = EventLocationEntity.builder()
                .id(1L)
                .name("test-location")
                .address("test-address")
                .capacity(100)
                .description("test-description")
                .events(new ArrayList<>())
                .build();

        locationDomain = EventLocation.builder()
                .id(1L)
                .name("test-location")
                .address("test-address")
                .capacity(100)
                .description("test-description")
                .build();
    }

    @Test
    void shouldSuccessfullyGetLocationById() {
        when(locationRepository.findById(1L)).thenReturn(Optional.of(locationEntity));
        when(mapper.toDomain(locationEntity)).thenReturn(locationDomain);

        EventLocation foundLocation = eventLocationService.getLocationById(1L);

        assertEquals(locationDomain.id(), foundLocation.id());
        assertEquals(locationDomain.name(), foundLocation.name());
        assertEquals(locationDomain.address(), foundLocation.address());
        assertEquals(locationDomain.capacity(), foundLocation.capacity());
        assertEquals(locationDomain.description(), foundLocation.description());
        verify(locationRepository).findById(1L);
        verify(mapper).toDomain(locationEntity);
    }

    @Test
    void shouldSuccessfullyCreateLocation() {
        EventLocation locationToCreate = EventLocation.builder()
                .name("new-location")
                .address("new-address")
                .capacity(150)
                .description("new-description")
                .build();

        EventLocationEntity newLocationEntity = EventLocationEntity.builder()
                .name("new-location")
                .address("new-address")
                .capacity(150)
                .description("new-description")
                .build();

        EventLocationEntity savedLocationEntity = EventLocationEntity.builder()
                .id(2L)
                .name("new-location")
                .address("new-address")
                .capacity(150)
                .description("new-description")
                .events(new ArrayList<>())
                .build();

        EventLocation savedLocationDomain = EventLocation.builder()
                .id(2L)
                .name("new-location")
                .address("new-address")
                .capacity(150)
                .description("new-description")
                .build();

        when(mapper.toEntity(locationToCreate)).thenReturn(newLocationEntity);
        when(locationRepository.save(newLocationEntity)).thenReturn(savedLocationEntity);
        when(mapper.toDomain(savedLocationEntity)).thenReturn(savedLocationDomain);

        EventLocation createdLocation = eventLocationService.createLocation(locationToCreate);

        assertNotNull(createdLocation.id());
        assertEquals("new-location", createdLocation.name());
        assertEquals("new-address", createdLocation.address());
        assertEquals(150, createdLocation.capacity());
        assertEquals("new-description", createdLocation.description());
        assertNotNull(newLocationEntity.getEvents());
        assertTrue(newLocationEntity.getEvents().isEmpty());
        verify(locationRepository).save(newLocationEntity);
    }

    @Test
    void shouldSuccessfullyUpdateLocation() {
        EventLocation locationToUpdate = EventLocation.builder()
                .name("updated-location")
                .address("updated-address")
                .capacity(120)
                .description("updated-description")
                .build();

        EventLocation updatedLocationDomain = EventLocation.builder()
                .id(1L)
                .name("updated-location")
                .address("updated-address")
                .capacity(120)
                .description("updated-description")
                .build();

        when(locationRepository.findById(1L)).thenReturn(Optional.of(locationEntity));
        when(mapper.toDomain(locationEntity)).thenReturn(updatedLocationDomain);

        EventLocation updatedLocation = eventLocationService.updateLocation(1L, locationToUpdate);

        assertEquals(1L, updatedLocation.id());
        assertEquals("updated-location", updatedLocation.name());
        assertEquals("updated-address", updatedLocation.address());
        assertEquals(120, updatedLocation.capacity());
        assertEquals("updated-description", updatedLocation.description());

        assertEquals("updated-location", locationEntity.getName());
        assertEquals("updated-address", locationEntity.getAddress());
        assertEquals(120, locationEntity.getCapacity());
        assertEquals("updated-description", locationEntity.getDescription());
    }

    @Test
    void shouldSuccessfullyDeleteLocationById() {
        when(locationRepository.existsById(1L)).thenReturn(true);

        eventLocationService.deleteLocationById(1L);

        verify(locationRepository).deleteById(1L);
    }

    @Test
    void shouldSuccessfullyReturnAllLocationsUsingDefaultPaging() {
        EventLocationEntity secondLocationEntity = EventLocationEntity.builder()
                .id(2L)
                .name("second-location")
                .address("second-address")
                .capacity(120)
                .description("second-description")
                .events(new ArrayList<>())
                .build();

        EventLocation secondLocationDomain = EventLocation.builder()
                .id(2L)
                .name("second-location")
                .address("second-address")
                .capacity(120)
                .description("second-description")
                .build();

        EventLocationSearchFilter filter = EventLocationSearchFilter.builder()
                .name(null)
                .address(null)
                .pageNumber(null)
                .pageSize(null)
                .build();

        when(locationRepository.searchLocations(eq(null), eq(null), any(Pageable.class)))
                .thenReturn(List.of(locationEntity, secondLocationEntity));
        when(mapper.toDomain(locationEntity)).thenReturn(locationDomain);
        when(mapper.toDomain(secondLocationEntity)).thenReturn(secondLocationDomain);

        List<EventLocation> foundLocations = eventLocationService.getAllLocations(filter);

        assertEquals(2, foundLocations.size());
        assertEquals(1L, foundLocations.get(0).id());
        assertEquals(2L, foundLocations.get(1).id());

        verify(locationRepository).searchLocations(eq(null), eq(null), argThat(pageable ->
                pageable.getPageNumber() == 0 && pageable.getPageSize() == 20
        ));
    }

    @Test
    void shouldSuccessfullyReturnFilteredLocations() {
        EventLocationSearchFilter filter = EventLocationSearchFilter.builder()
                .name("test-location")
                .address("test-address")
                .pageNumber(1)
                .pageSize(5)
                .build();

        when(locationRepository.searchLocations(eq("test-location"), eq("test-address"), any(Pageable.class)))
                .thenReturn(List.of(locationEntity));
        when(mapper.toDomain(locationEntity)).thenReturn(locationDomain);

        List<EventLocation> foundLocations = eventLocationService.getAllLocations(filter);

        assertEquals(1, foundLocations.size());
        assertEquals("test-location", foundLocations.get(0).name());
        verify(locationRepository).searchLocations(eq("test-location"), eq("test-address"), argThat(pageable ->
                pageable.getPageNumber() == 1 && pageable.getPageSize() == 5
        ));
    }

    @Test
    void shouldThrowLocationNotFoundExceptionWhenLocationNotFoundById() {
        when(locationRepository.findById(999L)).thenReturn(Optional.empty());

        LocationNotFoundException exception = assertThrows(LocationNotFoundException.class,
                () -> eventLocationService.getLocationById(999L)
        );

        assertEquals("No found location by id = 999", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenCreateLocationTemplateIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> eventLocationService.createLocation(null)
        );

        assertEquals("Location template can't be null", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenUpdateLocationTemplateIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> eventLocationService.updateLocation(1L, null)
        );

        assertEquals("Location template can't be null", exception.getMessage());
    }

    @Test
    void shouldThrowLocationNotFoundExceptionWhenUpdateLocationNotFound() {
        EventLocation locationToUpdate = EventLocation.builder()
                .name("updated-location")
                .address("updated-address")
                .capacity(120)
                .description("updated-description")
                .build();

        when(locationRepository.findById(999L)).thenReturn(Optional.empty());

        LocationNotFoundException exception = assertThrows(LocationNotFoundException.class,
                () -> eventLocationService.updateLocation(999L, locationToUpdate)
        );

        assertEquals("No found location by id = 999", exception.getMessage());
    }

    @Test
    void shouldThrowLocationCountPlacesExceptionWhenNewCapacityIsLessThanOldCapacity() {
        EventLocation locationToUpdate = EventLocation.builder()
                .name("updated-location")
                .address("updated-address")
                .capacity(50)
                .description("updated-description")
                .build();

        when(locationRepository.findById(1L)).thenReturn(Optional.of(locationEntity));

        LocationCountPlacesException exception = assertThrows(LocationCountPlacesException.class,
                () -> eventLocationService.updateLocation(1L, locationToUpdate)
        );

        assertEquals("You can't decrease location capacity, because places might be occupied by users",
                exception.getMessage());
    }

    @Test
    void shouldThrowLocationNotFoundExceptionWhenDeletingNotExistingLocation() {
        when(locationRepository.existsById(999L)).thenReturn(false);

        LocationNotFoundException exception = assertThrows(LocationNotFoundException.class,
                () -> eventLocationService.deleteLocationById(999L)
        );

        assertEquals("No found location by id = 999", exception.getMessage());
    }
}