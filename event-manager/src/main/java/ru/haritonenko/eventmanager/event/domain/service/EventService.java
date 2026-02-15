package ru.haritonenko.eventmanager.event.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.haritonenko.commonlibs.dto.changes.EventFieldChange;
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
import ru.haritonenko.eventmanager.event.registration.domain.exception.InvalidEventRegistrationStatusException;
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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventLocationRepository eventLocationRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final EventEntityMapper eventEntityMapper;
    private final EventCreateMapper eventCreateMapper;
    private final EventUpdateMapper eventUpdateMapper;
    private final KafkaEventSender kafkaEventSender;

    @Value("${app.location.default-page-size}")
    private int defaultPageSize;

    @Value("${app.location.default-page-number}")
    private int defaultPageNumber;

    @Cacheable(value = "events", key = "#id")
    @Transactional(readOnly = true)
    public Event getEventById(Long id) {
        log.info("Getting event by id: {}", id);
        var foundEvent = getEventByIdOrThrow(id);
        log.info("Event was successfully found by id: {}", id);
        return eventEntityMapper.toDomain(foundEvent);
    }

    @CacheEvict(value = "user-created-events", allEntries = true)
    @Transactional
    public Event createEventByUserId(
            Long ownerId,
            EventCreateRequestDto eventToCreate
    ) {
        log.info("Creating an event");
        var owner = getUserByIdOrThrow(ownerId);
        var location = getEventLocationByIdOrThrow(eventToCreate.locationId());

        checkLocationCapacityIsMoreOrEqualsEventPlacesOrThrow(location, eventToCreate.maxPlaces());

        var newEvent = eventCreateMapper.toEntity(eventToCreate, owner, location);

        location.addEvent(newEvent);
        owner.addOwnEvent(newEvent);

        var savedEventEntity = eventRepository.save(newEvent);
        log.info("Event was successfully created");
        return eventEntityMapper.toDomain(savedEventEntity);
    }

    @Caching(evict = {
            @CacheEvict(value = "events", key = "#eventId"),
            @CacheEvict(value = "user-created-events", allEntries = true),
            @CacheEvict(value = "user-booked-events", allEntries = true)
    })
    @Transactional
    public Event updateEvent(
            Long ownerId,
            Long eventId,
            EventUpdateRequestDto eventToUpdate
    ) {
        log.info("Updating event with id: {}", eventId);

        var user = getUserByIdOrThrow(ownerId);
        var event = getEventByIdOrThrow(eventId);

        checkRoleIsAdminAndEventOwnerIsUserToUpdateOrDeleteOrThrow(user, event);
        checkEventStatusIsWaitStartOrThrow(event);

        var newLocation = getEventLocationByIdOrThrow(eventToUpdate.locationId());
        var oldLocation = event.getLocation();

        checkLocationCapacityIsMoreOrEqualsEventPlacesOrThrow(newLocation, eventToUpdate.maxPlaces());
        checkCountOfOccupiedPlacesLessThanMaxOrThrow(eventToUpdate, event);

        var eventBeforeChangesSnapshot = getEventOldVersionSnapshot(event);

        event.setName(eventToUpdate.name());
        event.setMaxPlaces(eventToUpdate.maxPlaces());
        event.setDate(eventToUpdate.date());
        event.setCost(eventToUpdate.cost());
        event.setDuration(eventToUpdate.duration());

        if (!oldLocation.getId().equals(newLocation.getId())) {
            oldLocation.getEvents().remove(event);
            newLocation.getEvents().add(event);
            event.setLocation(newLocation);
        }

        eventUpdateMapper.updateEntity(event, eventToUpdate);
        log.info("Event with id: {} was successfully updated", eventId);

        var message = buildChangeNotification(
                eventBeforeChangesSnapshot,
                event,
                ownerId,
                getSubscribedUsersIdsToEventList(event)
        );
        if (nonNull(message)) {
            kafkaEventSender.sendKafkaEvent(message);
        }
        return eventEntityMapper.toDomain(event);
    }

    @Cacheable(
            value = "user-created-events",
            key = "#userFromRequest.id() + ':' + (#pageFilter.pageNumber()?:0) + ':' + (#pageFilter.pageSize()?:20)"
    )
    @Transactional(readOnly = true)
    public List<Event> findEventsCreatedByUser(
            AuthUser userFromRequest,
            EventPageFilter pageFilter
    ) {
        log.info("Searching all user's created events");
        var user = getUserByIdOrThrow(userFromRequest.id());
        checkUserRoleIsUserToGetListOfOwnEventsOrThrow(user);
        var createdEvents = eventRepository.searchCreatedEventsByUserId(
                user.getId(),
                getPageable(pageFilter)
        );
        return getSortedEventListByEventId(createdEvents);
    }

    @Cacheable(
            value = "user-booked-events",
            key = "#user.id() + ':' + (#pageFilter.pageNumber()?:0) + ':' + (#pageFilter.pageSize()?:20)"
    )
    @Transactional(readOnly = true)
    public List<Event> findBookedEventByUserId(
            AuthUser user,
            EventPageFilter pageFilter
    ) {
        log.info("Searching all user's booked events");
        var bookedEvents = eventRepository.searchBookedEventsByUserId(
                user.id(),
                EventRegistrationStatus.ACTIVE,
                getPageable(pageFilter)
        );
        return getSortedEventListByEventId(bookedEvents);
    }

    @Transactional(readOnly = true)
    public List<Event> searchEventWithFilter(
            EventSearchRequestDto eventFilter,
            EventPageFilter pageFilter
    ) {
        log.info("Searching events with filter");

        BigDecimal costMin = isNull(eventFilter.costMin()) ? null : BigDecimal.valueOf(eventFilter.costMin().doubleValue());
        BigDecimal costMax = isNull(eventFilter.costMax()) ? null : BigDecimal.valueOf(eventFilter.costMax().doubleValue());

        checkFilterConstraintsAreValidOrThrow(
                costMin,
                costMax,
                eventFilter.placesMin(),
                eventFilter.placesMax(),
                eventFilter.dateStartAfter(),
                eventFilter.dateStartBefore(),
                eventFilter.durationMin(),
                eventFilter.durationMax()
        );

        var foundEventsWithFilter = eventRepository.searchEventsWithFilter(
                eventFilter.name(),
                eventFilter.placesMin(),
                eventFilter.placesMax(),
                eventFilter.dateStartAfter(),
                eventFilter.dateStartBefore(),
                costMin,
                costMax,
                eventFilter.durationMin(),
                eventFilter.durationMax(),
                eventFilter.locationId(),
                eventFilter.eventStatus(),
                getPageable(pageFilter)
        );
        return getSortedEventListByEventId(foundEventsWithFilter);
    }

    @CacheEvict(value = "events", key = "#eventId")
    @Transactional
    public Event registerOnEvent(
            Long userId,
            Long eventId
    ) {
        log.info("Registration user on event");
        var user = getUserByIdOrThrow(userId);
        var event = getEventByIdOrThrow(eventId);

        log.info("Checking registry conditions");
        checkEventCreatorIsNotMemberOrThrow(event, userId);
        checkEventStatusIsWaitStartOrThrow(event);

        var optionalUserRegistration = eventRegistrationRepository.findByUserIdAndEventId(userId, eventId);

        if (optionalUserRegistration.isPresent()) {
            var registration = optionalUserRegistration.get();
            checkRegistrationStatusIsNotActiveOrThrow(registration);

            int updated = eventRepository.incOccupiedPlaces(eventId);
            checkCorrectUpdateOrThrow(updated, "Places are overflowed");
            eventRegistrationRepository.updateStatus(userId, eventId, EventRegistrationStatus.ACTIVE);
        } else {
            var registration = EventRegistrationEntity.builder()
                    .id(null)
                    .user(user)
                    .event(event)
                    .status(EventRegistrationStatus.ACTIVE)
                    .build();

            eventRegistrationRepository.save(registration);

            int updated = eventRepository.incOccupiedPlaces(eventId);
            checkCorrectUpdateOrThrow(updated, "Places are overflowed");
        }

        var updatedEvent = getEventByIdOrThrow(eventId);
        return eventEntityMapper.toDomain(updatedEvent);
    }

    @Caching(evict = {
            @CacheEvict(value = "events", key = "#eventId"),
            @CacheEvict(value = "user-created-events", allEntries = true),
            @CacheEvict(value = "user-booked-events", allEntries = true)
    })
    @Transactional
    public void deleteEventById(Long ownerId, Long eventId) {
        log.info("Deleting event by id: {}", eventId);

        var event = getEventByIdOrThrow(eventId);
        var user = getUserByIdOrThrow(ownerId);

        log.info("Checking conditions before deleting event");
        checkRoleIsAdminAndEventOwnerIsUserToUpdateOrDeleteOrThrow(user, event);
        checkEventStatusIsWaitStartOrThrow(event);

        var eventBeforeChangesSnapshot = getEventOldVersionSnapshot(event);

        event.setStatus(EventStatus.CANCELLED);

        int updatedStatus = eventRegistrationRepository.updateStatusByEventId(
                eventId,
                EventRegistrationStatus.CANCELLED,
                EventRegistrationStatus.ACTIVE
        );

        var message = buildChangeNotification(
                eventBeforeChangesSnapshot,
                event,
                ownerId,
                getSubscribedUsersIdsToEventList(event)
        );

        if (nonNull(message)) {
            kafkaEventSender.sendKafkaEvent(message);
        }

        int updatedPlaces = eventRepository.resetOccupiedPlaces(eventId);
        checkCorrectUpdateOrThrow(updatedStatus, "Error while updating event status");
        checkCorrectUpdateOrThrow(updatedPlaces, "Error while updating event occupied places");
    }

    @CacheEvict(value = "events", key = "#eventId")
    @Transactional
    public void cancelEventRegistrationRequestById(Long userId, Long eventId) {
        log.info("Cancelling event registration by id: {}", eventId);

        var event = getEventByIdOrThrow(eventId);
        checkEventStatusIsWaitStartOrThrow(event);

        var registration = getEventRegistrationByUserIdAndEventIdOrThrow(userId, eventId);

        checkRegistrationStatusIsActiveOrThrow(registration);
        int updated = eventRepository.decOccupiedPlaces(eventId);
        checkCorrectUpdateOrThrow(updated, "Places can not be less than zero");

        eventRegistrationRepository.updateStatus(userId, eventId, EventRegistrationStatus.CANCELLED);
    }

    private void checkFilterConstraintsAreValidOrThrow(
            BigDecimal costMin,
            BigDecimal costMax,
            Integer placesMin,
            Integer placesMax,
            String dateStartAfter,
            String dateStartBefore,
            Integer durationMin,
            Integer durationMax
    ) {
        if (nonNull(placesMin) && nonNull(placesMax) && placesMin > placesMax) {
            log.warn("Error while checking event places");
            throw new IllegalArgumentException("placesMin can not be more than placesMax");
        }
        if (nonNull(durationMin) && nonNull(durationMax) && durationMin > durationMax) {
            log.warn("Error while checking event duration");
            throw new IllegalArgumentException("durationMin can not be more than durationMax");
        }
        if (nonNull(costMin) && nonNull(costMax) && costMin.compareTo(costMax) > 0) {
            log.warn("Error while checking event cost");
            throw new IllegalArgumentException("costMin can not be  more than costMax");
        }
        if (nonNull(dateStartAfter) && nonNull(dateStartBefore) && dateStartAfter.compareTo(dateStartBefore) > 0) {
            log.warn("Error while checking event date");
            throw new IllegalArgumentException("dateStartAfter can not be later than dateStartBefore");
        }
    }

    private UserEntity getUserByIdOrThrow(Long ownerId) {
        return userRepository.findById(ownerId)
                .orElseThrow(() -> {
                    log.warn("Error while finding user by id: {}", ownerId);
                    return new UserNotFoundException("User not found");
                });
    }

    private EventEntity getEventByIdOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.warn("Error while searching for event by id: {}", eventId);
                    return new EventNotFoundException("No found event by id = %s".formatted(eventId));
                });
    }

    private EventLocationEntity getEventLocationByIdOrThrow(Long locationId) {
        return eventLocationRepository.findById(locationId)
                .orElseThrow(() -> {
                    log.warn("Error while searching for event location by id: {}", locationId);
                    return new LocationNotFoundException("No found event location by id = %s".formatted(locationId));
                });
    }

    private EventRegistrationEntity getEventRegistrationByUserIdAndEventIdOrThrow(Long userId, Long eventId) {
        return eventRegistrationRepository.findByUserIdAndEventId(userId, eventId)
                .orElseThrow(() -> {
                    log.warn("Error while searching for registration  by userId: {} and eventId: {} ", userId, eventId);
                    return new EventRegistrationNotFoundException(
                            "Registration not found by userId = %s and eventId = %s".formatted(userId, eventId)
                    );
                });
    }

    private Pageable getPageable(EventPageFilter pageFilter) {
        int pageSize = Objects.nonNull(pageFilter.pageSize()) ? pageFilter.pageSize() : defaultPageSize;
        int pageNumber = Objects.nonNull(pageFilter.pageNumber()) ? pageFilter.pageNumber() : defaultPageNumber;
        return Pageable.ofSize(pageSize).withPage(pageNumber);
    }

    private List<Event> getSortedEventListByEventId(List<EventEntity> events) {
        return events.stream()
                .map(eventEntityMapper::toDomain)
                .sorted(Comparator.comparing(Event::id))
                .collect(Collectors.toList());
    }

    private void checkEventStatusIsWaitStartOrThrow(EventEntity event) {
        if (event.getStatus() != EventStatus.WAIT_START) {
            log.warn("Error while checking event status to delete event or cancel registration");
            throw new NotValidEventStatusException("Event status is not WAIT_START for that action");
        }
    }

    private void checkEventCreatorIsNotMemberOrThrow(EventEntity eventToBeBookedByUser, Long userId) {
        if (Objects.equals(eventToBeBookedByUser.getOwner().getId(), userId)) {
            log.warn("Error while checking event creator");
            throw new UserAlreadyRegisteredOnEventException("Event creator is member by default");
        }
    }

    private void checkLocationCapacityIsMoreOrEqualsEventPlacesOrThrow(EventLocationEntity location, Integer eventPlaces) {
        if (location.getCapacity() < eventPlaces) {
            log.warn("Error while matching location and event places count");
            throw new EventCountPlacesUpdateException(
                    "Location capacity is less than event maxPlaces. Chose new location or decrease quantity of event places."
            );
        }
    }

    private void checkRoleIsAdminAndEventOwnerIsUserToUpdateOrDeleteOrThrow(UserEntity user, EventEntity event) {
        if (user.getUserRole() != UserRole.ADMIN && !event.getOwner().getId().equals(user.getId())) {
            log.warn("Error while checking user and admin role");
            throw new AccessDeniedException("You are not owner of this event");
        }
    }

    private void checkUserRoleIsUserToGetListOfOwnEventsOrThrow(UserEntity user) {
        if (user.getUserRole() != UserRole.USER) {
            log.warn("Error while checking user role");
            throw new AccessDeniedException("You are not owner of this event");
        }
    }

    private void checkCountOfOccupiedPlacesLessThanMaxOrThrow(EventUpdateRequestDto eventToUpdate, EventEntity event) {
        if (eventToUpdate.maxPlaces() < event.getOccupiedPlaces()) {
            log.warn("Error while checking count of places");
            throw new EventCountPlacesUpdateException("Occupied places can't be more than event maxPlaces ");
        }
    }

    private void checkRegistrationStatusIsNotActiveOrThrow(EventRegistrationEntity registration) {
        if (registration.getStatus() == EventRegistrationStatus.ACTIVE) {
            log.warn("Error while checking registration not active status");
            throw new UserAlreadyRegisteredOnEventException("You have already registered on this event");
        }
    }

    private void checkRegistrationStatusIsActiveOrThrow(EventRegistrationEntity registration) {
        if (registration.getStatus() != EventRegistrationStatus.ACTIVE) {
            log.warn("Error while checking registration active status");
            throw new InvalidEventRegistrationStatusException("This registration already not active");
        }
    }

    private void checkCorrectUpdateOrThrow(int updated, String message) {
        if (updated == 0) {
            log.warn("Error while updating event place");
            throw new IllegalStateException(message);
        }
    }

    private EventChangeKafkaMessage buildChangeNotification(
            EventEntity beforeChanges,
            EventEntity afterChanges,
            Long changedById,
            List<Long> users
    ) {
        var name = !Objects.equals(beforeChanges.getName(), afterChanges.getName())
                ? new EventFieldChange<>(beforeChanges.getName(), afterChanges.getName())
                : null;

        var maxPlaces = !Objects.equals(beforeChanges.getMaxPlaces(), afterChanges.getMaxPlaces())
                ? new EventFieldChange<>(beforeChanges.getMaxPlaces(), afterChanges.getMaxPlaces())
                : null;

        var time = !Objects.equals(beforeChanges.getDate(), afterChanges.getDate())
                ? new EventFieldChange<>(beforeChanges.getDate(), afterChanges.getDate())
                : null;

        var cost = !Objects.equals(beforeChanges.getCost(), afterChanges.getCost())
                ? new EventFieldChange<Number>(beforeChanges.getCost(), afterChanges.getCost())
                : null;

        var duration = !Objects.equals(beforeChanges.getDuration(), afterChanges.getDuration())
                ? new EventFieldChange<>(beforeChanges.getDuration(), afterChanges.getDuration())
                : null;

        var status = !Objects.equals(beforeChanges.getStatus(), afterChanges.getStatus())
                ? new EventFieldChange<>(beforeChanges.getStatus().toString(), afterChanges.getStatus().toString())
                : null;

        if (isNull(beforeChanges.getLocation()) || isNull(afterChanges.getLocation())) {
            throw new LocationNotFoundException("Event location must not be null");
        }

        Long beforeLocationId = beforeChanges.getLocation().getId();
        Long afterLocationId = afterChanges.getLocation().getId();

        var locationId = !Objects.equals(beforeLocationId, afterLocationId)
                ? new EventFieldChange<>(beforeLocationId, afterLocationId)
                : null;

        boolean hasAnyChange = nonNull(name) || nonNull(maxPlaces) ||
                nonNull(time) || nonNull(cost) || nonNull(duration) ||
                nonNull(locationId) || nonNull(status);

        if (!hasAnyChange) {
            return null;
        }

        return EventChangeKafkaMessage.builder()
                .users(users)
                .ownerId(afterChanges.getOwner().getId())
                .changedById(changedById)
                .eventId(afterChanges.getId())
                .name(name)
                .maxPlaces(maxPlaces)
                .time(time)
                .cost(cost)
                .duration(duration)
                .status(status)
                .locationId(locationId)
                .build();
    }

    private List<Long> getSubscribedUsersIdsToEventList(EventEntity event) {
        return event.getRegistrations().stream()
                .filter(Objects::nonNull)
                .filter(registration -> registration.getStatus() == EventRegistrationStatus.ACTIVE)
                .map(EventRegistrationEntity::getUser)
                .filter(Objects::nonNull)
                .map(UserEntity::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private EventEntity getEventOldVersionSnapshot(EventEntity event) {
        return EventEntity.builder()
                .id(event.getId())
                .name(event.getName())
                .owner(event.getOwner())
                .location(event.getLocation())
                .registrations(event.getRegistrations())
                .maxPlaces(event.getMaxPlaces())
                .occupiedPlaces(event.getOccupiedPlaces())
                .date(event.getDate())
                .cost(event.getCost())
                .duration(event.getDuration())
                .status(event.getStatus())
                .build();
    }
}
