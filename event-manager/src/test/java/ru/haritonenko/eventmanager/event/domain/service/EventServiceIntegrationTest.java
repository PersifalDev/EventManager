package ru.haritonenko.eventmanager.event.domain.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import ru.haritonenko.commonlibs.securirty.user.AuthUser;
import ru.haritonenko.eventmanager.AbstractIntegrationTest;
import ru.haritonenko.eventmanager.event.api.dto.EventCreateRequestDto;
import ru.haritonenko.eventmanager.event.api.dto.EventUpdateRequestDto;
import ru.haritonenko.eventmanager.event.api.dto.filter.EventPageFilter;
import ru.haritonenko.eventmanager.event.api.dto.filter.EventSearchRequestDto;
import ru.haritonenko.eventmanager.event.domain.Event;
import ru.haritonenko.eventmanager.event.domain.db.entity.EventEntity;
import ru.haritonenko.eventmanager.event.domain.db.repository.EventRepository;
import ru.haritonenko.eventmanager.event.domain.status.EventStatus;
import ru.haritonenko.eventmanager.event.exception.EventCountPlacesUpdateException;
import ru.haritonenko.eventmanager.event.exception.EventNotFoundException;
import ru.haritonenko.eventmanager.event.registration.domain.db.entity.EventRegistrationEntity;
import ru.haritonenko.eventmanager.event.registration.domain.db.repository.EventRegistrationRepository;
import ru.haritonenko.eventmanager.event.registration.domain.status.EventRegistrationStatus;
import ru.haritonenko.eventmanager.kafka.producer.sender.KafkaEventSender;
import ru.haritonenko.eventmanager.location.domain.db.entity.EventLocationEntity;
import ru.haritonenko.eventmanager.location.domain.db.repository.EventLocationRepository;
import ru.haritonenko.eventmanager.user.domain.db.entity.UserEntity;
import ru.haritonenko.eventmanager.user.domain.db.repository.UserRepository;
import ru.haritonenko.eventmanager.user.domain.exception.UserAlreadyRegisteredOnEventException;
import ru.haritonenko.eventmanager.user.domain.role.UserRole;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EventServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EventService eventService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventLocationRepository locationRepository;

    @Autowired
    private EventRegistrationRepository registrationRepository;

    @MockitoBean
    private KafkaEventSender kafkaEventSender;

    /** happy paths **/

    @Transactional
    @Test
    void shouldSuccessfullyGetEventById() {
        UserEntity owner = getSavedDummyUser("owner-user");
        EventLocationEntity location = getSavedDummyLocation();
        EventEntity savedEvent = getSavedDummyEvent(owner, location);

        Event foundEvent = eventService.getEventById(savedEvent.getId());

        assertEquals(savedEvent.getId(), foundEvent.id());
        assertEquals(savedEvent.getName(), foundEvent.name());
        assertEquals(savedEvent.getOwner().getId().toString(), foundEvent.ownerId());
        assertEquals(savedEvent.getLocation().getId(), foundEvent.locationId());
        assertEquals(savedEvent.getStatus(), foundEvent.status());
    }

    @Transactional
    @Test
    void shouldSuccessfullyCreateEvent() {
        UserEntity owner = getSavedDummyUser("owner-user");
        EventLocationEntity location = getSavedDummyLocation();

        EventCreateRequestDto eventToCreate = new EventCreateRequestDto(
                "created-event",
                120,
                "2099-12-25T15:00:00",
                1700,
                90,
                location.getId()
        );

        Event createdEvent = eventService.createEventByUserId(owner.getId(), eventToCreate);

        assertNotNull(createdEvent.id());
        assertEquals("created-event", createdEvent.name());
        assertEquals(owner.getId().toString(), createdEvent.ownerId());
        assertEquals(location.getId(), createdEvent.locationId());
        assertEquals(EventStatus.WAIT_START, createdEvent.status());

        EventEntity savedEventEntity = eventRepository.findById(createdEvent.id()).orElseThrow();

        assertEquals("created-event", savedEventEntity.getName());
        assertEquals(120, savedEventEntity.getMaxPlaces());
        assertEquals(0, savedEventEntity.getOccupiedPlaces());
        assertEquals(location.getId(), savedEventEntity.getLocation().getId());
        assertEquals(owner.getId(), savedEventEntity.getOwner().getId());
    }

    @Transactional
    @Test
    void shouldSuccessfullyUpdateEvent() {
        UserEntity owner = getSavedDummyUser("owner-user");
        EventLocationEntity oldLocation = getSavedDummyLocation();
        EventLocationEntity newLocation = locationRepository.save(
                EventLocationEntity.builder()
                        .name("new-location")
                        .address("new-address")
                        .capacity(500)
                        .description("new-location-description")
                        .events(new ArrayList<>())
                        .build()
        );
        EventEntity savedEvent = getSavedDummyEvent(owner, oldLocation);

        EventUpdateRequestDto eventToUpdate = new EventUpdateRequestDto(
                "updated-event",
                150,
                "2099-12-30T16:00:00",
                BigDecimal.valueOf(2200),
                180,
                newLocation.getId()
        );

        Event updatedEvent = eventService.updateEvent(owner.getId(), savedEvent.getId(), eventToUpdate);

        assertEquals(savedEvent.getId(), updatedEvent.id());
        assertEquals("updated-event", updatedEvent.name());
        assertEquals(150, updatedEvent.maxPlaces());
        assertEquals("2099-12-30T16:00:00", updatedEvent.date());
        assertEquals(BigDecimal.valueOf(2200), updatedEvent.cost());
        assertEquals(180, updatedEvent.duration());
        assertEquals(newLocation.getId(), updatedEvent.locationId());

        EventEntity updatedEventEntity = eventRepository.findById(savedEvent.getId()).orElseThrow();

        assertEquals("updated-event", updatedEventEntity.getName());
        assertEquals(150, updatedEventEntity.getMaxPlaces());
        assertEquals(newLocation.getId(), updatedEventEntity.getLocation().getId());
    }

    @Transactional
    @Test
    void shouldSuccessfullyRegisterUserOnEvent() {
        UserEntity owner = getSavedDummyUser("owner-user");
        UserEntity member = getSavedDummyUser("member-user");
        EventLocationEntity location = getSavedDummyLocation();
        EventEntity savedEvent = getSavedDummyEvent(owner, location);

        Event updatedEvent = eventService.registerOnEvent(member.getId(), savedEvent.getId());

        assertEquals(1, updatedEvent.occupiedPlaces());

        EventEntity foundEventEntity = eventRepository.findById(savedEvent.getId()).orElseThrow();
        EventRegistrationEntity registration = registrationRepository.findByUserIdAndEventId(member.getId(), savedEvent.getId())
                .orElseThrow();

        assertEquals(1, foundEventEntity.getOccupiedPlaces());
        assertEquals(EventRegistrationStatus.ACTIVE, registration.getStatus());
    }

    @Transactional
    @Test
    void shouldSuccessfullyCancelRegistrationRequest() {
        UserEntity owner = getSavedDummyUser("owner-user");
        UserEntity member = getSavedDummyUser("member-user");
        EventLocationEntity location = getSavedDummyLocation();
        EventEntity savedEvent = getSavedDummyEvent(owner, location);

        savedEvent.setOccupiedPlaces(1);
        eventRepository.save(savedEvent);

        registrationRepository.save(
                EventRegistrationEntity.builder()
                        .user(member)
                        .event(savedEvent)
                        .status(EventRegistrationStatus.ACTIVE)
                        .build()
        );

        assertDoesNotThrow(() ->
                eventService.cancelEventRegistrationRequestById(member.getId(), savedEvent.getId())
        );

        EventEntity foundEventEntity = eventRepository.findById(savedEvent.getId()).orElseThrow();
        EventRegistrationEntity registration = registrationRepository.findByUserIdAndEventId(member.getId(), savedEvent.getId())
                .orElseThrow();

        assertEquals(0, foundEventEntity.getOccupiedPlaces());
        assertEquals(EventRegistrationStatus.CANCELLED, registration.getStatus());
    }

    @Transactional
    @Test
    void shouldSuccessfullyFindEventsCreatedByUser() {
        UserEntity owner = getSavedDummyUser("owner-user");
        EventLocationEntity location = getSavedDummyLocation();

        EventEntity firstEvent = getSavedDummyEvent(owner, location);
        EventEntity secondEvent = eventRepository.save(
                EventEntity.builder()
                        .name("second-event")
                        .owner(owner)
                        .location(location)
                        .registrations(new ArrayList<>())
                        .maxPlaces(80)
                        .occupiedPlaces(0)
                        .date("2099-12-21T11:00:00")
                        .cost(BigDecimal.valueOf(1200))
                        .duration(90)
                        .status(EventStatus.WAIT_START)
                        .build()
        );

        List<Event> foundEvents = eventService.findEventsCreatedByUser(
                new AuthUser(owner.getId(), owner.getLogin(), owner.getUserRole().name()),
                new EventPageFilter(null, null)
        );

        assertEquals(2, foundEvents.size());
        assertEquals(firstEvent.getId(), foundEvents.get(0).id());
        assertEquals(secondEvent.getId(), foundEvents.get(1).id());
    }

    @Transactional
    @Test
    void shouldSuccessfullySearchEventsWithFilter() {
        UserEntity owner = getSavedDummyUser("owner-user");
        EventLocationEntity location = getSavedDummyLocation();

        eventRepository.save(
                EventEntity.builder()
                        .name("checked-event")
                        .owner(owner)
                        .location(location)
                        .registrations(new ArrayList<>())
                        .maxPlaces(100)
                        .occupiedPlaces(0)
                        .date("2099-12-21T11:00:00")
                        .cost(BigDecimal.valueOf(1200))
                        .duration(90)
                        .status(EventStatus.WAIT_START)
                        .build()
        );

        eventRepository.save(
                EventEntity.builder()
                        .name("another-event")
                        .owner(owner)
                        .location(location)
                        .registrations(new ArrayList<>())
                        .maxPlaces(50)
                        .occupiedPlaces(0)
                        .date("2099-12-22T11:00:00")
                        .cost(BigDecimal.valueOf(900))
                        .duration(60)
                        .status(EventStatus.CANCELLED)
                        .build()
        );

        EventSearchRequestDto filter = new EventSearchRequestDto(
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
                EventStatus.WAIT_START
        );

        List<Event> foundEvents = eventService.searchEventWithFilter(filter, new EventPageFilter(null, null));

        assertEquals(1, foundEvents.size());
        assertEquals("checked-event", foundEvents.get(0).name());
        assertEquals(EventStatus.WAIT_START, foundEvents.get(0).status());
    }

    /** negative paths **/

    @Transactional
    @Test
    void shouldThrowEventNotFoundExceptionWhenEventNotFoundById() {
        EventNotFoundException exception = assertThrows(
                EventNotFoundException.class,
                () -> eventService.getEventById(999999L)
        );

        assertEquals("No found event by id = 999999", exception.getMessage());
    }

    @Transactional
    @Test
    void shouldThrowEventCountPlacesUpdateExceptionWhenLocationCapacityIsLessThanEventPlaces() {
        UserEntity owner = getSavedDummyUser("owner-user");
        EventLocationEntity location = locationRepository.save(
                EventLocationEntity.builder()
                        .name("small-location")
                        .address("small-address")
                        .capacity(10)
                        .description("small-location-description")
                        .events(new ArrayList<>())
                        .build()
        );

        EventCreateRequestDto eventToCreate = new EventCreateRequestDto(
                "big-event",
                100,
                "2099-12-25T15:00:00",
                1700,
                90,
                location.getId()
        );

        EventCountPlacesUpdateException exception = assertThrows(
                EventCountPlacesUpdateException.class,
                () -> eventService.createEventByUserId(owner.getId(), eventToCreate)
        );

        assertEquals(
                "Location capacity is less than event maxPlaces. Chose new location or decrease quantity of event places.",
                exception.getMessage()
        );
    }

    @Transactional
    @Test
    void shouldThrowUserAlreadyRegisteredOnEventExceptionWhenOwnerRegistersOnOwnEvent() {
        UserEntity owner = getSavedDummyUser("owner-user");
        EventLocationEntity location = getSavedDummyLocation();
        EventEntity savedEvent = getSavedDummyEvent(owner, location);

        UserAlreadyRegisteredOnEventException exception = assertThrows(
                UserAlreadyRegisteredOnEventException.class,
                () -> eventService.registerOnEvent(owner.getId(), savedEvent.getId())
        );

        assertEquals("Event creator is member by default", exception.getMessage());
    }

    private UserEntity getSavedDummyUser(String login) {
        return userRepository.save(
                UserEntity.builder()
                        .login(login)
                        .password("test-password")
                        .age(25)
                        .userRole(UserRole.USER)
                        .ownEvents(new ArrayList<>())
                        .registrations(new ArrayList<>())
                        .build()
        );
    }

    private EventLocationEntity getSavedDummyLocation() {
        return locationRepository.save(
                EventLocationEntity.builder()
                        .name("test-location")
                        .address("test-address")
                        .capacity(500)
                        .description("test-location-description")
                        .events(new ArrayList<>())
                        .build()
        );
    }

    private EventEntity getSavedDummyEvent(UserEntity owner, EventLocationEntity location) {
        return eventRepository.save(
                EventEntity.builder()
                        .name("test-event")
                        .owner(owner)
                        .location(location)
                        .registrations(new ArrayList<>())
                        .maxPlaces(100)
                        .occupiedPlaces(0)
                        .date("2099-12-20T10:00:00")
                        .cost(BigDecimal.valueOf(1500))
                        .duration(120)
                        .status(EventStatus.WAIT_START)
                        .build()
        );
    }
}