package ru.haritonenko.eventmanager.event.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.haritonenko.commonlibs.dto.notification.EventChangeKafkaMessage;
import ru.haritonenko.commonlibs.securirty.user.AuthUser;
import ru.haritonenko.eventmanager.event.api.dto.EventCreateRequestDto;
import ru.haritonenko.eventmanager.event.api.dto.EventUpdateRequestDto;
import ru.haritonenko.eventmanager.event.api.dto.filter.EventPageFilter;
import ru.haritonenko.eventmanager.event.api.dto.filter.EventSearchRequestDto;
import ru.haritonenko.eventmanager.event.domain.Event;
import ru.haritonenko.eventmanager.event.domain.db.entity.EventEntity;
import ru.haritonenko.eventmanager.event.domain.db.repository.EventRepository;
import ru.haritonenko.eventmanager.event.domain.mapper.EventCreateMapper;
import ru.haritonenko.eventmanager.event.domain.mapper.EventEntityMapper;
import ru.haritonenko.eventmanager.event.domain.mapper.EventUpdateMapper;
import ru.haritonenko.eventmanager.event.domain.status.EventStatus;
import ru.haritonenko.eventmanager.event.exception.EventCountPlacesUpdateException;
import ru.haritonenko.eventmanager.event.exception.EventNotFoundException;
import ru.haritonenko.eventmanager.event.exception.NotValidEventStatusException;
import ru.haritonenko.eventmanager.event.registration.domain.db.entity.EventRegistrationEntity;
import ru.haritonenko.eventmanager.event.registration.domain.db.repository.EventRegistrationRepository;
import ru.haritonenko.eventmanager.event.registration.domain.exception.EventRegistrationNotFoundException;
import ru.haritonenko.eventmanager.event.registration.domain.status.EventRegistrationStatus;
import ru.haritonenko.eventmanager.kafka.producer.sender.KafkaEventSender;
import ru.haritonenko.eventmanager.location.domain.db.entity.EventLocationEntity;
import ru.haritonenko.eventmanager.location.domain.db.repository.EventLocationRepository;
import ru.haritonenko.eventmanager.location.domain.exception.LocationNotFoundException;
import ru.haritonenko.eventmanager.user.domain.db.entity.UserEntity;
import ru.haritonenko.eventmanager.user.domain.db.repository.UserRepository;
import ru.haritonenko.eventmanager.user.domain.exception.UserAlreadyRegisteredOnEventException;
import ru.haritonenko.eventmanager.user.domain.exception.UserNotFoundException;
import ru.haritonenko.eventmanager.user.domain.role.UserRole;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceUnitTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventLocationRepository eventLocationRepository;

    @Mock
    private EventRegistrationRepository eventRegistrationRepository;

    @Mock
    private EventEntityMapper eventEntityMapper;

    @Mock
    private EventCreateMapper eventCreateMapper;

    @Mock
    private EventUpdateMapper eventUpdateMapper;

    @Mock
    private KafkaEventSender kafkaEventSender;

    @InjectMocks
    private EventService eventService;

    private UserEntity owner;
    private UserEntity member;
    private EventLocationEntity location;
    private EventEntity eventEntity;
    private Event eventDomain;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(eventService, "defaultPageSize", 20);
        ReflectionTestUtils.setField(eventService, "defaultPageNumber", 0);

        owner = UserEntity.builder()
                .id(1L)
                .login("owner")
                .password("password")
                .age(25)
                .userRole(UserRole.USER)
                .ownEvents(new ArrayList<>())
                .registrations(new ArrayList<>())
                .build();

        member = UserEntity.builder()
                .id(2L)
                .login("member")
                .password("password")
                .age(23)
                .userRole(UserRole.USER)
                .ownEvents(new ArrayList<>())
                .registrations(new ArrayList<>())
                .build();

        location = EventLocationEntity.builder()
                .id(10L)
                .name("test-location")
                .address("test-address")
                .capacity(300)
                .description("test-location-description")
                .events(new ArrayList<>())
                .build();

        eventEntity = EventEntity.builder()
                .id(100L)
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
                .build();

        eventDomain = new Event(
                100L,
                "test-event",
                "1",
                100,
                0,
                "2099-12-20T10:00:00",
                BigDecimal.valueOf(1500),
                120,
                10L,
                EventStatus.WAIT_START
        );
    }

    /** happy paths **/

    @Test
    void shouldSuccessfullyGetEventById() {
        when(eventRepository.findById(100L)).thenReturn(Optional.of(eventEntity));
        when(eventEntityMapper.toDomain(eventEntity)).thenReturn(eventDomain);

        Event foundEvent = eventService.getEventById(100L);

        assertEquals(eventDomain.id(), foundEvent.id());
        assertEquals(eventDomain.name(), foundEvent.name());
        assertEquals(eventDomain.ownerId(), foundEvent.ownerId());
        verify(eventRepository).findById(100L);
        verify(eventEntityMapper).toDomain(eventEntity);
    }

    @Test
    void shouldSuccessfullyCreateEvent() {
        EventCreateRequestDto eventToCreate = new EventCreateRequestDto(
                "new-event",
                120,
                "2099-12-25T15:00:00",
                1700,
                90,
                10L
        );

        EventEntity newEventEntity = EventEntity.builder()
                .id(null)
                .name("new-event")
                .owner(owner)
                .location(location)
                .registrations(new ArrayList<>())
                .maxPlaces(120)
                .occupiedPlaces(0)
                .date("2099-12-25T15:00:00")
                .cost(BigDecimal.valueOf(1700))
                .duration(90)
                .status(EventStatus.WAIT_START)
                .build();

        EventEntity savedEventEntity = EventEntity.builder()
                .id(101L)
                .name("new-event")
                .owner(owner)
                .location(location)
                .registrations(new ArrayList<>())
                .maxPlaces(120)
                .occupiedPlaces(0)
                .date("2099-12-25T15:00:00")
                .cost(BigDecimal.valueOf(1700))
                .duration(90)
                .status(EventStatus.WAIT_START)
                .build();

        Event savedEventDomain = new Event(
                101L,
                "new-event",
                "1",
                120,
                0,
                "2099-12-25T15:00:00",
                BigDecimal.valueOf(1700),
                90,
                10L,
                EventStatus.WAIT_START
        );

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(eventLocationRepository.findById(location.getId())).thenReturn(Optional.of(location));
        when(eventCreateMapper.toEntity(eventToCreate, owner, location)).thenReturn(newEventEntity);
        when(eventRepository.save(newEventEntity)).thenReturn(savedEventEntity);
        when(eventEntityMapper.toDomain(savedEventEntity)).thenReturn(savedEventDomain);

        Event createdEvent = eventService.createEventByUserId(owner.getId(), eventToCreate);

        assertNotNull(createdEvent.id());
        assertEquals("new-event", createdEvent.name());
        assertEquals("1", createdEvent.ownerId());
        verify(eventRepository).save(newEventEntity);
    }

    @Test
    void shouldSuccessfullyReturnEventsCreatedByUser() {
        AuthUser authUser = new AuthUser(1L, "owner", "USER");
        EventPageFilter pageFilter = new EventPageFilter(null, null);

        EventEntity secondEventEntity = EventEntity.builder()
                .id(101L)
                .name("second-event")
                .owner(owner)
                .location(location)
                .registrations(new ArrayList<>())
                .maxPlaces(80)
                .occupiedPlaces(0)
                .date("2099-12-25T15:00:00")
                .cost(BigDecimal.valueOf(1200))
                .duration(90)
                .status(EventStatus.WAIT_START)
                .build();

        Event secondEventDomain = new Event(
                101L,
                "second-event",
                "1",
                80,
                0,
                "2099-12-25T15:00:00",
                BigDecimal.valueOf(1200),
                90,
                10L,
                EventStatus.WAIT_START
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(eventRepository.searchCreatedEventsByUserId(eq(1L), any())).thenReturn(List.of(secondEventEntity, eventEntity));
        when(eventEntityMapper.toDomain(eventEntity)).thenReturn(eventDomain);
        when(eventEntityMapper.toDomain(secondEventEntity)).thenReturn(secondEventDomain);

        List<Event> foundEvents = eventService.findEventsCreatedByUser(authUser, pageFilter);

        assertEquals(2, foundEvents.size());
        assertEquals(100L, foundEvents.get(0).id());
        assertEquals(101L, foundEvents.get(1).id());
    }

    @Test
    void shouldSuccessfullyRegisterUserOnEvent() {
        EventRegistrationEntity registration = EventRegistrationEntity.builder()
                .id(1L)
                .user(member)
                .event(eventEntity)
                .status(EventRegistrationStatus.CANCELLED)
                .build();

        Event updatedEventDomain = new Event(
                100L,
                "test-event",
                "1",
                100,
                1,
                "2099-12-20T10:00:00",
                BigDecimal.valueOf(1500),
                120,
                10L,
                EventStatus.WAIT_START
        );

        when(userRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(eventRepository.findById(eventEntity.getId())).thenReturn(Optional.of(eventEntity));
        when(eventRegistrationRepository.findByUserIdAndEventId(member.getId(), eventEntity.getId()))
                .thenReturn(Optional.of(registration));
        when(eventRepository.incOccupiedPlaces(eventEntity.getId())).thenReturn(1);
        when(eventEntityMapper.toDomain(eventEntity)).thenReturn(updatedEventDomain);

        Event registeredEvent = eventService.registerOnEvent(member.getId(), eventEntity.getId());

        assertEquals(100L, registeredEvent.id());
        verify(eventRegistrationRepository).updateStatus(
                member.getId(),
                eventEntity.getId(),
                EventRegistrationStatus.ACTIVE
        );
    }

    @Test
    void shouldSuccessfullyCancelEventRegistrationRequest() {
        EventRegistrationEntity registration = EventRegistrationEntity.builder()
                .id(1L)
                .user(member)
                .event(eventEntity)
                .status(EventRegistrationStatus.ACTIVE)
                .build();

        when(eventRepository.findById(eventEntity.getId())).thenReturn(Optional.of(eventEntity));
        when(eventRegistrationRepository.findByUserIdAndEventId(member.getId(), eventEntity.getId()))
                .thenReturn(Optional.of(registration));
        when(eventRepository.decOccupiedPlaces(eventEntity.getId())).thenReturn(1);

        assertDoesNotThrow(() -> eventService.cancelEventRegistrationRequestById(member.getId(), eventEntity.getId()));

        verify(eventRegistrationRepository).updateStatus(
                member.getId(),
                eventEntity.getId(),
                EventRegistrationStatus.CANCELLED
        );
    }

    @Test
    void shouldSuccessfullyUpdateEventAndSendKafkaNotification() {
        EventUpdateRequestDto eventToUpdate = new EventUpdateRequestDto(
                "updated-event",
                150,
                "2099-12-30T16:00:00",
                BigDecimal.valueOf(2000),
                180,
                10L
        );

        EventRegistrationEntity registration = EventRegistrationEntity.builder()
                .id(1L)
                .user(member)
                .event(eventEntity)
                .status(EventRegistrationStatus.ACTIVE)
                .build();
        eventEntity.setRegistrations(new ArrayList<>(List.of(registration)));

        Event updatedEventDomain = new Event(
                100L,
                "updated-event",
                "1",
                150,
                0,
                "2099-12-30T16:00:00",
                BigDecimal.valueOf(2000),
                180,
                10L,
                EventStatus.WAIT_START
        );

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(eventRepository.findById(eventEntity.getId())).thenReturn(Optional.of(eventEntity));
        when(eventLocationRepository.findById(location.getId())).thenReturn(Optional.of(location));
        doAnswer(invocation -> {
            EventEntity entity = invocation.getArgument(0);
            entity.setName("updated-event");
            entity.setMaxPlaces(150);
            entity.setDate("2099-12-30T16:00:00");
            entity.setCost(BigDecimal.valueOf(2000));
            entity.setDuration(180);
            return null;
        }).when(eventUpdateMapper).updateEntity(eventEntity, eventToUpdate);
        when(eventEntityMapper.toDomain(eventEntity)).thenReturn(updatedEventDomain);

        Event updatedEvent = eventService.updateEvent(owner.getId(), eventEntity.getId(), eventToUpdate);

        assertEquals("updated-event", updatedEvent.name());
        verify(kafkaEventSender).sendKafkaEvent(any(EventChangeKafkaMessage.class));
    }

    /** negative paths **/

    @Test
    void shouldThrowEventNotFoundExceptionWhenEventNotFoundById() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        EventNotFoundException exception = assertThrows(
                EventNotFoundException.class,
                () -> eventService.getEventById(999L)
        );

        assertEquals("No found event by id = 999", exception.getMessage());
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenOwnerNotFoundWhileCreatingEvent() {
        EventCreateRequestDto eventToCreate = new EventCreateRequestDto(
                "new-event",
                120,
                "2099-12-25T15:00:00",
                1700,
                90,
                10L
        );

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> eventService.createEventByUserId(1L, eventToCreate)
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void shouldThrowLocationNotFoundExceptionWhenLocationNotFoundWhileCreatingEvent() {
        EventCreateRequestDto eventToCreate = new EventCreateRequestDto(
                "new-event",
                120,
                "2099-12-25T15:00:00",
                1700,
                90,
                10L
        );

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(eventLocationRepository.findById(10L)).thenReturn(Optional.empty());

        LocationNotFoundException exception = assertThrows(
                LocationNotFoundException.class,
                () -> eventService.createEventByUserId(owner.getId(), eventToCreate)
        );

        assertEquals("No found event location by id = 10", exception.getMessage());
    }

    @Test
    void shouldThrowEventCountPlacesUpdateExceptionWhenLocationCapacityIsLessThanEventMaxPlaces() {
        EventCreateRequestDto eventToCreate = new EventCreateRequestDto(
                "new-event",
                500,
                "2099-12-25T15:00:00",
                1700,
                90,
                10L
        );

        location.setCapacity(100);

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(eventLocationRepository.findById(location.getId())).thenReturn(Optional.of(location));

        EventCountPlacesUpdateException exception = assertThrows(
                EventCountPlacesUpdateException.class,
                () -> eventService.createEventByUserId(owner.getId(), eventToCreate)
        );

        assertEquals(
                "Location capacity is less than event maxPlaces. Chose new location or decrease quantity of event places.",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowUserAlreadyRegisteredOnEventExceptionWhenOwnerTryingRegisterOnOwnEvent() {
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(eventRepository.findById(eventEntity.getId())).thenReturn(Optional.of(eventEntity));

        UserAlreadyRegisteredOnEventException exception = assertThrows(
                UserAlreadyRegisteredOnEventException.class,
                () -> eventService.registerOnEvent(owner.getId(), eventEntity.getId())
        );

        assertEquals("Event creator is member by default", exception.getMessage());
    }

    @Test
    void shouldThrowUserAlreadyRegisteredOnEventExceptionWhenRegistrationAlreadyActive() {
        EventRegistrationEntity registration = EventRegistrationEntity.builder()
                .id(1L)
                .user(member)
                .event(eventEntity)
                .status(EventRegistrationStatus.ACTIVE)
                .build();

        when(userRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(eventRepository.findById(eventEntity.getId())).thenReturn(Optional.of(eventEntity));
        when(eventRegistrationRepository.findByUserIdAndEventId(member.getId(), eventEntity.getId()))
                .thenReturn(Optional.of(registration));

        UserAlreadyRegisteredOnEventException exception = assertThrows(
                UserAlreadyRegisteredOnEventException.class,
                () -> eventService.registerOnEvent(member.getId(), eventEntity.getId())
        );

        assertEquals("You have already registered on this event", exception.getMessage());
    }

    @Test
    void shouldThrowNotValidEventStatusExceptionWhenCancelRegistrationForNotWaitStartEvent() {
        eventEntity.setStatus(EventStatus.CANCELLED);

        when(eventRepository.findById(eventEntity.getId())).thenReturn(Optional.of(eventEntity));

        NotValidEventStatusException exception = assertThrows(
                NotValidEventStatusException.class,
                () -> eventService.cancelEventRegistrationRequestById(member.getId(), eventEntity.getId())
        );

        assertEquals("Event status is not WAIT_START for that action", exception.getMessage());
    }

    @Test
    void shouldThrowEventRegistrationNotFoundExceptionWhenRegistrationNotFound() {
        when(eventRepository.findById(eventEntity.getId())).thenReturn(Optional.of(eventEntity));
        when(eventRegistrationRepository.findByUserIdAndEventId(member.getId(), eventEntity.getId()))
                .thenReturn(Optional.empty());

        EventRegistrationNotFoundException exception = assertThrows(
                EventRegistrationNotFoundException.class,
                () -> eventService.cancelEventRegistrationRequestById(member.getId(), eventEntity.getId())
        );

        assertEquals(
                "Registration not found by userId = 2 and eventId = 100",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenSearchFilterIsInvalid() {
        EventSearchRequestDto invalidFilter = new EventSearchRequestDto(
                null,
                10,
                5,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> eventService.searchEventWithFilter(invalidFilter, new EventPageFilter(null, null))
        );

        assertEquals("placesMin can not be more than placesMax", exception.getMessage());
    }
}