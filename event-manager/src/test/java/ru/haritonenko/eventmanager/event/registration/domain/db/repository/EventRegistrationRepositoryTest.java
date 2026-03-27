package ru.haritonenko.eventmanager.event.registration.domain.db.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ru.haritonenko.eventmanager.event.domain.db.entity.EventEntity;
import ru.haritonenko.eventmanager.event.domain.db.repository.EventRepository;
import ru.haritonenko.eventmanager.event.domain.status.EventStatus;
import ru.haritonenko.eventmanager.event.registration.domain.db.entity.EventRegistrationEntity;
import ru.haritonenko.eventmanager.event.registration.domain.status.EventRegistrationStatus;
import ru.haritonenko.eventmanager.location.domain.db.AbstractJpaTest;
import ru.haritonenko.eventmanager.location.domain.db.entity.EventLocationEntity;
import ru.haritonenko.eventmanager.location.domain.db.repository.EventLocationRepository;
import ru.haritonenko.eventmanager.user.domain.db.entity.UserEntity;
import ru.haritonenko.eventmanager.user.domain.db.repository.UserRepository;
import ru.haritonenko.eventmanager.user.domain.role.UserRole;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class EventRegistrationRepositoryTest extends AbstractJpaTest {

    @Autowired
    private EventRegistrationRepository eventRegistrationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventLocationRepository locationRepository;

    private UserEntity member;
    private EventEntity eventEntity;

    @BeforeEach
    void setUp() {
        UserEntity owner = userRepository.save(
                UserEntity.builder()
                        .login("owner-login")
                        .password("owner-password")
                        .age(25)
                        .userRole(UserRole.USER)
                        .ownEvents(new ArrayList<>())
                        .registrations(new ArrayList<>())
                        .build()
        );

        member = userRepository.save(
                UserEntity.builder()
                        .login("member-login")
                        .password("member-password")
                        .age(20)
                        .userRole(UserRole.USER)
                        .ownEvents(new ArrayList<>())
                        .registrations(new ArrayList<>())
                        .build()
        );

        EventLocationEntity location = locationRepository.save(
                EventLocationEntity.builder()
                        .name("test-location")
                        .address("test-address")
                        .capacity(200)
                        .description("test-description")
                        .events(new ArrayList<>())
                        .build()
        );

        eventEntity = eventRepository.save(
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

    @Test
    void shouldSaveRegistrationAndGenerateId() {
        EventRegistrationEntity registration = EventRegistrationEntity.builder()
                .user(member)
                .event(eventEntity)
                .status(EventRegistrationStatus.ACTIVE)
                .build();

        EventRegistrationEntity savedRegistration = eventRegistrationRepository.save(registration);

        assertNotNull(savedRegistration.getId());
        assertEquals(member.getId(), savedRegistration.getUser().getId());
        assertEquals(eventEntity.getId(), savedRegistration.getEvent().getId());
        assertEquals(EventRegistrationStatus.ACTIVE, savedRegistration.getStatus());
    }

    @Test
    void shouldFindRegistrationByUserIdAndEventId() {
        EventRegistrationEntity savedRegistration = eventRegistrationRepository.save(
                EventRegistrationEntity.builder()
                        .user(member)
                        .event(eventEntity)
                        .status(EventRegistrationStatus.ACTIVE)
                        .build()
        );

        Optional<EventRegistrationEntity> foundRegistrationOpt =
                eventRegistrationRepository.findByUserIdAndEventId(member.getId(), eventEntity.getId());

        assertTrue(foundRegistrationOpt.isPresent());

        EventRegistrationEntity foundRegistration = foundRegistrationOpt.get();

        assertEquals(savedRegistration.getId(), foundRegistration.getId());
        assertEquals(member.getId(), foundRegistration.getUser().getId());
        assertEquals(eventEntity.getId(), foundRegistration.getEvent().getId());
        assertEquals(EventRegistrationStatus.ACTIVE, foundRegistration.getStatus());
    }

    @Test
    void shouldSuccessfullyUpdateRegistrationStatus() {
        eventRegistrationRepository.save(
                EventRegistrationEntity.builder()
                        .user(member)
                        .event(eventEntity)
                        .status(EventRegistrationStatus.CANCELLED)
                        .build()
        );

        eventRegistrationRepository.updateStatus(
                member.getId(),
                eventEntity.getId(),
                EventRegistrationStatus.ACTIVE
        );

        EventRegistrationEntity updatedRegistration =
                eventRegistrationRepository.findByUserIdAndEventId(member.getId(), eventEntity.getId()).orElseThrow();

        assertEquals(EventRegistrationStatus.ACTIVE, updatedRegistration.getStatus());
    }

    @Test
    void shouldSuccessfullyUpdateStatusByEventId() {
        EventRegistrationEntity firstRegistration = eventRegistrationRepository.save(
                EventRegistrationEntity.builder()
                        .user(member)
                        .event(eventEntity)
                        .status(EventRegistrationStatus.ACTIVE)
                        .build()
        );

        UserEntity secondMember = userRepository.save(
                UserEntity.builder()
                        .login("second-member-login")
                        .password("password")
                        .age(19)
                        .userRole(UserRole.USER)
                        .ownEvents(new ArrayList<>())
                        .registrations(new ArrayList<>())
                        .build()
        );

        EventRegistrationEntity secondRegistration = eventRegistrationRepository.save(
                EventRegistrationEntity.builder()
                        .user(secondMember)
                        .event(eventEntity)
                        .status(EventRegistrationStatus.ACTIVE)
                        .build()
        );

        int updatedRows = eventRegistrationRepository.updateStatusByEventId(
                eventEntity.getId(),
                EventRegistrationStatus.CANCELLED,
                EventRegistrationStatus.ACTIVE
        );

        EventRegistrationEntity updatedFirstRegistration =
                eventRegistrationRepository.findById(firstRegistration.getId()).orElseThrow();
        EventRegistrationEntity updatedSecondRegistration =
                eventRegistrationRepository.findById(secondRegistration.getId()).orElseThrow();

        assertEquals(2, updatedRows);
        assertEquals(EventRegistrationStatus.CANCELLED, updatedFirstRegistration.getStatus());
        assertEquals(EventRegistrationStatus.CANCELLED, updatedSecondRegistration.getStatus());
    }

    @Test
    void shouldReturnEmptyOptionalWhenRegistrationNotFound() {
        Optional<EventRegistrationEntity> foundRegistrationOpt =
                eventRegistrationRepository.findByUserIdAndEventId(999L, 999L);

        assertTrue(foundRegistrationOpt.isEmpty());
    }

    @Test
    void shouldNotUpdateStatusByEventIdWhenNothingMatched() {
        int updatedRows = eventRegistrationRepository.updateStatusByEventId(
                999L,
                EventRegistrationStatus.CANCELLED,
                EventRegistrationStatus.ACTIVE
        );

        assertEquals(0, updatedRows);
    }
}