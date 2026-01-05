package ru.haritonenko.eventmanager.event.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.haritonenko.eventmanager.event.domain.Event;
import ru.haritonenko.eventmanager.event.domain.converter.EventEntityConverter;
import ru.haritonenko.eventmanager.event.api.dto.EventCreateRequestDto;
import ru.haritonenko.eventmanager.event.api.dto.EventUpdateRequestDto;
import ru.haritonenko.eventmanager.event.api.dto.filter.EventPageFilter;
import ru.haritonenko.eventmanager.event.api.dto.filter.EventSearchRequestDto;
import ru.haritonenko.eventmanager.event.domain.exception.EventCountPlacesException;
import ru.haritonenko.eventmanager.event.domain.exception.EventInvalidStatusException;
import ru.haritonenko.eventmanager.event.domain.exception.EventNotFoundException;
import ru.haritonenko.eventmanager.event.registration.db.repository.EventRegistrationRepository;
import ru.haritonenko.eventmanager.event.registration.db.entity.EventRegistrationEntity;
import ru.haritonenko.eventmanager.event.registration.exception.EventRegistrationNotFoundException;
import ru.haritonenko.eventmanager.event.registration.exception.InvalidEventRegistrationStatusException;
import ru.haritonenko.eventmanager.event.registration.status.EventRegistrationStatus;
import ru.haritonenko.eventmanager.location.domain.exception.LocationNotFoundException;
import ru.haritonenko.eventmanager.location.domain.db.entity.EventLocationEntity;
import ru.haritonenko.eventmanager.user.domain.exception.UserAlreadyRegisteredOnEventException;
import ru.haritonenko.eventmanager.event.domain.status.EventStatus;
import ru.haritonenko.eventmanager.event.domain.db.entity.EventEntity;
import ru.haritonenko.eventmanager.event.domain.db.repository.EventRepository;
import ru.haritonenko.eventmanager.location.domain.db.repository.EventLocationRepository;
import ru.haritonenko.eventmanager.user.domain.exception.UserNotFoundException;
import ru.haritonenko.eventmanager.user.domain.role.UserRole;
import ru.haritonenko.eventmanager.user.domain.db.entity.UserEntity;
import ru.haritonenko.eventmanager.user.domain.db.repository.UserRepository;
import ru.haritonenko.eventmanager.user.domain.User;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
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

    @Value("${app.location.default-page-size}")
    private int defaultPageSize;

    @Value("${app.location.default-page-number}")
    private int defaultPageNumber;

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventLocationRepository eventLocationRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final EventEntityConverter converter;

    @Transactional(readOnly = true)
    public Event getEventById(Integer id) {
        log.info("Getting event by id: {}", id);
        var foundEvent = getEventByIdOrThrow(id);
        log.info("Event was successfully found by id: {}", id);
        return converter.toDomain(foundEvent);
    }

    @Transactional
    public Event createEventByUserId(
            Integer ownerId,
            EventCreateRequestDto eventToCreate
    ) {
        log.info("Creating an event");
        var owner = getUserByIdOrThrow(ownerId);
        var locationId = eventToCreate.locationId();
        var location = getEventLocationByIdOrThrow(locationId);
        checkLocationCapacityIsMoreOrEqualsEventPlacesOrThrow(location, eventToCreate.maxPlaces());
        var newEvent = new EventEntity(
                null,
                eventToCreate.name(),
                owner,
                location,
                new ArrayList<>(),
                eventToCreate.maxPlaces(),
                0,
                eventToCreate.date(),
                BigDecimal.valueOf(eventToCreate.cost()),
                eventToCreate.duration(),
                EventStatus.WAIT_START
        );
        location.addEvent(newEvent);
        owner.addOwnEvent(newEvent);
        var savedEventEntity = eventRepository.save(newEvent);
        log.info("Event was successfully created");
        return converter.toDomain(savedEventEntity);
    }

    @Transactional
    public Event updateEvent(
            Integer ownerId,
            Integer eventId,
            EventUpdateRequestDto eventToUpdate
    ) {
        log.info("Updating event with id: {}", eventId);

        var user = getUserByIdOrThrow(ownerId);
        var event = getEventByIdOrThrow(eventId);

        checkRoleIsAdminAndEventOwnerIsUserToUpdateOrDeleteOrThrow(user, event);

        var newLocation = getEventLocationByIdOrThrow(eventToUpdate.locationId());
        var oldLocation = event.getLocation();

        checkLocationCapacityIsMoreOrEqualsEventPlacesOrThrow(newLocation, eventToUpdate.maxPlaces());
        checkCountOfOccupiedPlacesLessThanMaxOrThrow(eventToUpdate, event);

        if (!oldLocation.getId().equals(newLocation.getId())) {
            oldLocation.getEvents().remove(event);
            event.setLocation(newLocation);
            newLocation.getEvents().add(event);
        }

        event.setName(eventToUpdate.name());
        event.setMaxPlaces(eventToUpdate.maxPlaces());
        event.setDate(eventToUpdate.date());
        event.setCost(eventToUpdate.cost());
        event.setDuration(eventToUpdate.duration());

        log.info("Event with id: {} was successfully updated", eventId);
        return converter.toDomain(event);
    }

    public List<Event> findEventsCreatedByUser(
            User userFromRequest,
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

    public List<Event> findBookedEventByUserId(
            User user,
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

    @Transactional
    public Event registerOnEvent(Integer userId, Integer eventId) {
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
            var registration = new EventRegistrationEntity(
                    null,
                    user,
                    event,
                    EventRegistrationStatus.ACTIVE
            );
            eventRegistrationRepository.save(registration);
            int updated = eventRepository.incOccupiedPlaces(eventId);
            checkCorrectUpdateOrThrow(updated, "Places are overflowed");
        }
        var updatedEvent = getEventByIdOrThrow(eventId);
        return converter.toDomain(updatedEvent);
    }

    @Transactional
    public void deleteEventById(Integer ownerId, Integer eventId) {

        log.info("Deleting event by id: {}", eventId);
        var event = getEventByIdOrThrow(eventId);
        var user = getUserByIdOrThrow(ownerId);

        log.info("Checking conditions before deleting event");
        checkRoleIsAdminAndEventOwnerIsUserToUpdateOrDeleteOrThrow(user, event);
        checkEventStatusIsWaitStartOrThrow(event);
        event.setStatus(EventStatus.CANCELLED);
        int updatedStatus = eventRegistrationRepository.updateStatusByEventId(
                eventId,
                EventRegistrationStatus.CANCELLED,
                EventRegistrationStatus.ACTIVE
        );
        int updatedPlaces = eventRepository.resetOccupiedPlaces(eventId);
        checkCorrectUpdateOrThrow(updatedStatus, "Error while updating event status");
        checkCorrectUpdateOrThrow(updatedPlaces, "Error while updating event occupied places");
    }

    @Transactional
    public void cancelEventRegistrationRequestById(Integer userId, Integer eventId) {
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
        if (nonNull(placesMin) && nonNull(placesMax)
                && placesMin > placesMax) {
            log.warn("Error while checking event places");
            throw new IllegalArgumentException("placesMin can not be more than placesMax");
        }
        if (nonNull(durationMin)
                && nonNull(durationMax) && durationMin > durationMax) {
            log.warn("Error while checking event duration");
            throw new IllegalArgumentException("durationMin can not be more than durationMax");
        }
        if (nonNull(costMin) && nonNull(costMax)
                && costMin.compareTo(costMax) > 0) {
            log.warn("Error while checking event cost");
            throw new IllegalArgumentException("costMin can not be  more than costMax");
        }
        if (nonNull(dateStartAfter) && nonNull(dateStartBefore)
                && dateStartAfter.compareTo(dateStartBefore) > 0) {
            log.warn("Error while checking event date");
            throw new IllegalArgumentException("dateStartAfter can not be later than dateStartBefore");
        }
    }

    private UserEntity getUserByIdOrThrow(Integer ownerId) {
        return userRepository.findById(ownerId)
                .orElseThrow(() -> {
                    log.warn("Error while finding user by id: {}", ownerId);
                    return new UserNotFoundException("User not found");
                });
    }

    private EventEntity getEventByIdOrThrow(Integer eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.warn("Error while searching for event by id: {}", eventId);
                    return new EventNotFoundException(
                            "No found event by id = %s".formatted(eventId));
                });
    }

    private EventLocationEntity getEventLocationByIdOrThrow(
            Integer locationId
    ) {
        return eventLocationRepository.findById(locationId)
                .orElseThrow(() -> {
                    log.warn("Error while searching for event location by id: {}", locationId);
                    return new LocationNotFoundException(
                            "No found event location by id = %s".formatted(locationId));
                });
    }

    private EventRegistrationEntity getEventRegistrationByUserIdAndEventIdOrThrow(
            Integer userId,
            Integer eventId
    ) {
        return eventRegistrationRepository.findByUserIdAndEventId(userId, eventId)
                .orElseThrow(() -> {
                    log.warn("Error while searching for registration  by userId: {} and eventId: {} ", userId, eventId);
                    return new EventRegistrationNotFoundException(
                            "Registration not found by userId = %s and eventId = %s".formatted(userId, eventId));
                });
    }

    private Pageable getPageable(EventPageFilter pageFilter) {
        int pageSize = Objects.nonNull(pageFilter.pageSize())
                ? pageFilter.pageSize() : defaultPageSize;
        int pageNumber = Objects.nonNull(pageFilter.pageNumber())
                ? pageFilter.pageNumber() : defaultPageNumber;
        return Pageable
                .ofSize(pageSize)
                .withPage(pageNumber);
    }

    private List<Event> getSortedEventListByEventId(List<EventEntity> events) {
        return events.stream()
                .map(converter::toDomain)
                .sorted(Comparator.comparing(Event::id))
                .collect(Collectors.toList());
    }


    public void checkEventStatusIsWaitStartOrThrow(
            EventEntity event
    ) {
        if (event.getStatus() != EventStatus.WAIT_START) {
            log.warn("Error while checking event status to delete event or cancel registration");
            throw new EventInvalidStatusException("Event status is not WAIT_START for that action");

        }
    }

    private void checkEventCreatorIsNotMemberOrThrow(
            EventEntity eventToBeBookedByUser,
            Integer userId
    ) {
        if (Objects.equals(eventToBeBookedByUser.getOwner().getId(), userId)) {
            log.warn("Error while checking event creator");
            throw new UserAlreadyRegisteredOnEventException("Event creator is member by default");
        }
    }

    private void checkLocationCapacityIsMoreOrEqualsEventPlacesOrThrow(
            EventLocationEntity location,
            Integer eventPlaces
    ) {
        if (location.getCapacity() < eventPlaces) {
            log.warn("Error while matching location and event places count");
            throw new EventCountPlacesException("Location capacity is less than event maxPlaces." +
                    " Chose new location or decrease quantity of event places.");
        }
    }

    private void checkRoleIsAdminAndEventOwnerIsUserToUpdateOrDeleteOrThrow(
            UserEntity user,
            EventEntity event
    ) {
        if (user.getUserRole() != UserRole.ADMIN
                && !event.getOwner().getId().equals(user.getId())) {
            log.warn("Error while checking user and admin role");
            throw new AccessDeniedException("You are not owner of this event");
        }
    }

    private void checkUserRoleIsUserToGetListOfOwnEventsOrThrow(
            UserEntity user
    ) {
        if (user.getUserRole() != UserRole.USER) {
            log.warn("Error while checking user role");
            throw new AccessDeniedException("You are not owner of this event");
        }
    }

    private void checkCountOfOccupiedPlacesLessThanMaxOrThrow(
            EventUpdateRequestDto eventToUpdate,
            EventEntity event
    ) {
        if (eventToUpdate.maxPlaces() < event.getOccupiedPlaces()) {
            log.warn("Error while checking count of places");
            throw new EventCountPlacesException("Occupied places can't be more than event maxPlaces ");
        }
    }

    private void checkRegistrationStatusIsNotActiveOrThrow(
            EventRegistrationEntity registration
    ) {
        if (registration.getStatus() == EventRegistrationStatus.ACTIVE) {
            log.warn("Error while checking registration not active status");
            throw new UserAlreadyRegisteredOnEventException("You have already registered on this event");
        }
    }

    private void checkRegistrationStatusIsActiveOrThrow(
            EventRegistrationEntity registration
    ) {
        if (registration.getStatus() != EventRegistrationStatus.ACTIVE) {
            log.warn("Error while checking registration active status");
            throw new InvalidEventRegistrationStatusException("This registration already not active");
        }
    }

    private void checkCorrectUpdateOrThrow(
            Integer updated,
            String message
    ) {
        if (updated == 0) {
            log.warn("Error while updating event place");
            throw new IllegalStateException(message);
        }
    }
}

