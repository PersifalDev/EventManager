package ru.haritonenko.eventmanager.event.domain.db.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Pageable;
import ru.haritonenko.eventmanager.event.domain.db.entity.EventEntity;
import ru.haritonenko.eventmanager.event.domain.status.EventStatus;
import ru.haritonenko.eventmanager.event.registration.domain.db.entity.EventRegistrationEntity;
import ru.haritonenko.eventmanager.event.registration.domain.db.repository.EventRegistrationRepository;
import ru.haritonenko.eventmanager.event.registration.domain.status.EventRegistrationStatus;
import ru.haritonenko.eventmanager.location.domain.db.AbstractJpaTest;
import ru.haritonenko.eventmanager.location.domain.db.entity.EventLocationEntity;
import ru.haritonenko.eventmanager.location.domain.db.repository.EventLocationRepository;
import ru.haritonenko.eventmanager.user.domain.db.entity.UserEntity;
import ru.haritonenko.eventmanager.user.domain.db.repository.UserRepository;
import ru.haritonenko.eventmanager.user.domain.role.UserRole;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class EventRepositoryTest extends AbstractJpaTest {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int DEFAULT_FIRST_PAGE_NUMBER = 0;
    private static final int DEFAULT_SECOND_PAGE_NUMBER = 1;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventLocationRepository locationRepository;

    @Autowired
    private EventRegistrationRepository registrationRepository;

    private UserEntity owner;
    private UserEntity member;
    private EventLocationEntity location;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(
                UserEntity.builder()
                        .login("owner-user")
                        .password("test-password")
                        .age(25)
                        .userRole(UserRole.USER)
                        .ownEvents(new ArrayList<>())
                        .registrations(new ArrayList<>())
                        .build()
        );

        member = userRepository.save(
                UserEntity.builder()
                        .login("member-user")
                        .password("test-password")
                        .age(23)
                        .userRole(UserRole.USER)
                        .ownEvents(new ArrayList<>())
                        .registrations(new ArrayList<>())
                        .build()
        );

        location = locationRepository.save(
                EventLocationEntity.builder()
                        .name("test-location")
                        .address("test-address")
                        .capacity(500)
                        .description("test-location-description")
                        .events(new ArrayList<>())
                        .build()
        );
    }

    /** happy paths **/

    @Test
    void shouldSaveEventAndGenerateId() {
        EventEntity eventToSave = buildEventEntity(
                "test-event",
                100,
                0,
                "2099-12-20T10:00:00",
                BigDecimal.valueOf(1500),
                120,
                EventStatus.WAIT_START
        );

        EventEntity savedEvent = eventRepository.save(eventToSave);

        assertNotNull(savedEvent.getId());
        assertEquals("test-event", savedEvent.getName());
        assertEquals(100, savedEvent.getMaxPlaces());
        assertEquals(0, savedEvent.getOccupiedPlaces());
        assertEquals("2099-12-20T10:00:00", savedEvent.getDate());
        assertEquals(BigDecimal.valueOf(1500), savedEvent.getCost());
        assertEquals(120, savedEvent.getDuration());
        assertEquals(EventStatus.WAIT_START, savedEvent.getStatus());
        assertEquals(owner.getId(), savedEvent.getOwner().getId());
        assertEquals(location.getId(), savedEvent.getLocation().getId());
    }

    @Test
    void shouldFindCreatedEventsByUserId() {
        EventEntity firstEvent = eventRepository.save(buildEventEntity(
                "test-event-1",
                50,
                0,
                "2099-12-20T10:00:00",
                BigDecimal.valueOf(1000),
                60,
                EventStatus.WAIT_START
        ));
        EventEntity secondEvent = eventRepository.save(buildEventEntity(
                "test-event-2",
                80,
                0,
                "2099-12-21T10:00:00",
                BigDecimal.valueOf(1200),
                90,
                EventStatus.WAIT_START
        ));

        List<EventEntity> createdEvents = eventRepository.searchCreatedEventsByUserId(
                owner.getId(),
                getPageable(DEFAULT_PAGE_SIZE, DEFAULT_FIRST_PAGE_NUMBER)
        );

        assertEquals(2, createdEvents.size());
        assertTrue(createdEvents.stream().anyMatch(event -> event.getId().equals(firstEvent.getId())));
        assertTrue(createdEvents.stream().anyMatch(event -> event.getId().equals(secondEvent.getId())));
    }

    @Test
    void shouldFindBookedEventsByUserId() {
        EventEntity event = eventRepository.save(buildEventEntity(
                "booked-event",
                50,
                1,
                "2099-12-20T10:00:00",
                BigDecimal.valueOf(1000),
                60,
                EventStatus.WAIT_START
        ));

        registrationRepository.save(
                EventRegistrationEntity.builder()
                        .user(member)
                        .event(event)
                        .status(EventRegistrationStatus.ACTIVE)
                        .build()
        );

        List<EventEntity> bookedEvents = eventRepository.searchBookedEventsByUserId(
                member.getId(),
                EventRegistrationStatus.ACTIVE,
                getPageable(DEFAULT_PAGE_SIZE, DEFAULT_FIRST_PAGE_NUMBER)
        );

        assertEquals(1, bookedEvents.size());
        assertEquals(event.getId(), bookedEvents.get(0).getId());
    }

    @Test
    void shouldReturnFilteredEventsByNameAndStatus() {
        eventRepository.save(buildEventEntity(
                "checked-event",
                70,
                0,
                "2099-12-20T10:00:00",
                BigDecimal.valueOf(1100),
                90,
                EventStatus.WAIT_START
        ));
        eventRepository.save(buildEventEntity(
                "another-event",
                70,
                0,
                "2099-12-20T10:00:00",
                BigDecimal.valueOf(1100),
                90,
                EventStatus.FINISHED
        ));

        List<EventEntity> foundEvents = eventRepository.searchEventsWithFilter(
                "checked-event",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                EventStatus.WAIT_START,
                getPageable(DEFAULT_PAGE_SIZE, DEFAULT_FIRST_PAGE_NUMBER)
        );

        assertEquals(1, foundEvents.size());
        assertEquals("checked-event", foundEvents.get(0).getName());
        assertEquals(EventStatus.WAIT_START, foundEvents.get(0).getStatus());
    }

    @Test
    void shouldIncreaseOccupiedPlacesWhenPlacesAreAvailable() {
        EventEntity event = eventRepository.save(buildEventEntity(
                "test-event",
                2,
                0,
                "2099-12-20T10:00:00",
                BigDecimal.valueOf(1300),
                90,
                EventStatus.WAIT_START
        ));

        int updated = eventRepository.incOccupiedPlaces(event.getId());

        EventEntity updatedEvent = eventRepository.findById(event.getId()).orElseThrow();

        assertEquals(1, updated);
        assertEquals(1, updatedEvent.getOccupiedPlaces());
    }

    @Test
    void shouldDecreaseOccupiedPlacesWhenOccupiedPlacesAreMoreThanZero() {
        EventEntity event = eventRepository.save(buildEventEntity(
                "test-event",
                10,
                2,
                "2099-12-20T10:00:00",
                BigDecimal.valueOf(1300),
                90,
                EventStatus.WAIT_START
        ));

        int updated = eventRepository.decOccupiedPlaces(event.getId());

        EventEntity updatedEvent = eventRepository.findById(event.getId()).orElseThrow();

        assertEquals(1, updated);
        assertEquals(1, updatedEvent.getOccupiedPlaces());
    }

    @Test
    void shouldResetOccupiedPlaces() {
        EventEntity event = eventRepository.save(buildEventEntity(
                "test-event",
                10,
                5,
                "2099-12-20T10:00:00",
                BigDecimal.valueOf(1300),
                90,
                EventStatus.WAIT_START
        ));

        int updated = eventRepository.resetOccupiedPlaces(event.getId());

        EventEntity updatedEvent = eventRepository.findById(event.getId()).orElseThrow();

        assertEquals(1, updated);
        assertEquals(0, updatedEvent.getOccupiedPlaces());
    }

    @Test
    void shouldReturnDifferentEventsForDifferentPages() {
        createUniqueEvents(15);

        List<EventEntity> firstPage = eventRepository.searchEventsWithFilter(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                getPageable(DEFAULT_PAGE_SIZE, DEFAULT_FIRST_PAGE_NUMBER)
        );

        List<EventEntity> secondPage = eventRepository.searchEventsWithFilter(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                getPageable(DEFAULT_PAGE_SIZE, DEFAULT_SECOND_PAGE_NUMBER)
        );

        List<Long> firstPageIds = firstPage.stream()
                .map(EventEntity::getId)
                .toList();

        List<Long> secondPageIds = secondPage.stream()
                .map(EventEntity::getId)
                .toList();

        assertEquals(10, firstPage.size());
        assertEquals(5, secondPage.size());
        assertNotEquals(firstPageIds, secondPageIds);
    }

    /** negative paths **/

    @Test
    void shouldNotIncreaseOccupiedPlacesWhenEventIsFull() {
        EventEntity event = eventRepository.save(buildEventEntity(
                "full-event",
                2,
                2,
                "2099-12-20T10:00:00",
                BigDecimal.valueOf(1300),
                90,
                EventStatus.WAIT_START
        ));

        int updated = eventRepository.incOccupiedPlaces(event.getId());

        EventEntity updatedEvent = eventRepository.findById(event.getId()).orElseThrow();

        assertEquals(0, updated);
        assertEquals(2, updatedEvent.getOccupiedPlaces());
    }

    @Test
    void shouldNotDecreaseOccupiedPlacesWhenOccupiedPlacesAreZero() {
        EventEntity event = eventRepository.save(buildEventEntity(
                "empty-event",
                10,
                0,
                "2099-12-20T10:00:00",
                BigDecimal.valueOf(1300),
                90,
                EventStatus.WAIT_START
        ));

        int updated = eventRepository.decOccupiedPlaces(event.getId());

        EventEntity updatedEvent = eventRepository.findById(event.getId()).orElseThrow();

        assertEquals(0, updated);
        assertEquals(0, updatedEvent.getOccupiedPlaces());
    }

    private EventEntity buildEventEntity(
            String name,
            Integer maxPlaces,
            Integer occupiedPlaces,
            String date,
            BigDecimal cost,
            Integer duration,
            EventStatus status
    ) {
        return EventEntity.builder()
                .name(name)
                .owner(owner)
                .location(location)
                .registrations(new ArrayList<>())
                .maxPlaces(maxPlaces)
                .occupiedPlaces(occupiedPlaces)
                .date(date)
                .cost(cost)
                .duration(duration)
                .status(status)
                .build();
    }

    private void createUniqueEvents(int count) {
        List<EventEntity> events = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            events.add(
                    EventEntity.builder()
                            .name("test-event-" + i)
                            .owner(owner)
                            .location(location)
                            .registrations(new ArrayList<>())
                            .maxPlaces(100 + i)
                            .occupiedPlaces(0)
                            .date("2099-12-20T10:%02d:00".formatted(i % 60))
                            .cost(BigDecimal.valueOf(1000 + i))
                            .duration(60 + i)
                            .status(EventStatus.WAIT_START)
                            .build()
            );
        }

        eventRepository.saveAll(events);
    }

    private Pageable getPageable(int pageSize, int pageNumber) {
        return Pageable.ofSize(pageSize).withPage(pageNumber);
    }
}