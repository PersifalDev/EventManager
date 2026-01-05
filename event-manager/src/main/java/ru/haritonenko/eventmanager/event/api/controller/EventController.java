package ru.haritonenko.eventmanager.event.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.haritonenko.eventmanager.event.domain.converter.EventDtoConverter;
import ru.haritonenko.eventmanager.event.api.dto.EventCreateRequestDto;
import ru.haritonenko.eventmanager.event.api.dto.EventDto;
import ru.haritonenko.eventmanager.event.api.dto.EventUpdateRequestDto;
import ru.haritonenko.eventmanager.event.api.dto.filter.EventPageFilter;
import ru.haritonenko.eventmanager.event.api.dto.filter.EventSearchRequestDto;
import ru.haritonenko.eventmanager.event.domain.service.EventService;
import ru.haritonenko.eventmanager.user.security.service.AuthenticationService;
import ru.haritonenko.eventmanager.user.domain.User;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final EventDtoConverter converter;
    private final AuthenticationService authenticationService;

    @GetMapping("/{id}")
    public EventDto getById(
            @PathVariable Integer id
    ) {
        log.info("Get request for getting event by id: {}", id);
        var foundEvent = eventService.getEventById(id);
        return converter.toDto(foundEvent);
    }

    @GetMapping("/my")
    public List<EventDto> getUserCreatedEvents(
            @Valid EventPageFilter pageFilter
    ) {
        log.info("Get request for getting events created by user");
        return eventService.findEventsCreatedByUser(getAuthenticatedUser(), pageFilter)
                .stream()
                .map(converter::toDto)
                .toList();
    }

    @GetMapping("/registrations/my")
    public List<EventDto> getUserBookedEvents(
            @Valid EventPageFilter pageFilter
    ) {
        log.info("Get request for getting events booked by user");
        return eventService.findBookedEventByUserId(getAuthenticatedUser(), pageFilter)
                .stream()
                .map(converter::toDto)
                .toList();
    }

    @PostMapping
    public ResponseEntity<EventDto> createEvent(
            @Valid @RequestBody EventCreateRequestDto eventFromCreationRequest
    ) {
        log.info("Post request for creation a new event: {}", eventFromCreationRequest);
        var createdEvent = eventService.createEventByUserId(
                getAuthenticatedUser().id(),
                eventFromCreationRequest
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(converter.toDto(createdEvent));
    }

    @PostMapping("/search")
    public ResponseEntity<List<EventDto>> searchEventWithFilter(
            @Valid @RequestBody EventSearchRequestDto eventFromSearchWithFilterRequest,
            @Valid EventPageFilter pageFilter
    ) {
        log.info("Post request for search a new event with filter: {}", eventFromSearchWithFilterRequest);
        var foundEvents = eventService.searchEventWithFilter(eventFromSearchWithFilterRequest, pageFilter)
                .stream()
                .map(converter::toDto)
                .toList();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(foundEvents);
    }

    @PostMapping("/registrations/{id}")
    public ResponseEntity<EventDto> registerUserOnEvent(
            @PathVariable("id") Integer eventId
    ) {
        log.info("Post request for register a new user on event with eventId: {}", eventId);
        var eventThatUserRegisteredOn = eventService.registerOnEvent(
                getAuthenticatedUser().id(),
                eventId
        );
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(converter.toDto(eventThatUserRegisteredOn));
    }

    @PutMapping("/{id}")
    public EventDto updateEvent(
            @PathVariable("id") Integer eventId,
            @RequestBody @Valid EventUpdateRequestDto eventFromUpdateRequest
    ) {
        log.info("Put request for updating event: {}", eventFromUpdateRequest);
        var updatedEvent = eventService
                .updateEvent(
                        getAuthenticatedUser().id(),
                        eventId,
                        eventFromUpdateRequest);
        return converter.toDto(updatedEvent);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEventById(
            @PathVariable("id") Integer eventId
    ) {
        log.info("Delete request for deleting event with id: {}", eventId);
        eventService.deleteEventById(
                getAuthenticatedUser().id(),
                eventId
        );
    }

    @DeleteMapping("/registrations/cancel/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEventRegistrationRequest(
            @PathVariable("id") Integer eventId
    ) {
        log.info("Delete request for cancel event registration from event with id: {}", eventId);
        eventService.cancelEventRegistrationRequestById(
                getAuthenticatedUser().id(),
                eventId
        );
    }

    private User getAuthenticatedUser() {
        return authenticationService.getCurrentAuthenticatedUser();
    }
}
