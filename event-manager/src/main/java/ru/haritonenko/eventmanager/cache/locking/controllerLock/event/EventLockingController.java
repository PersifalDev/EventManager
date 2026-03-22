package ru.haritonenko.eventmanager.cache.locking.controllerLock.event;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.haritonenko.commonlibs.securirty.user.AuthUser;
import ru.haritonenko.eventmanager.cache.locking.manager.RedisLockManager;
import ru.haritonenko.eventmanager.event.api.dto.EventCreateRequestDto;
import ru.haritonenko.eventmanager.event.api.dto.EventDto;
import ru.haritonenko.eventmanager.event.api.dto.EventUpdateRequestDto;
import ru.haritonenko.eventmanager.event.domain.mapper.EventDtoMapper;
import ru.haritonenko.eventmanager.event.domain.service.EventService;
import ru.haritonenko.eventmanager.user.security.service.AuthenticationService;

import java.time.Duration;

import static java.util.Objects.isNull;

@Profile("dev")
@Slf4j
@RestController
@RequestMapping("/events/lock")
@RequiredArgsConstructor
public class EventLockingController {

    private final EventService eventService;
    private final EventDtoMapper mapper;
    private final AuthenticationService authenticationService;
    private final RedisLockManager redisLockManager;

    @PostMapping
    public ResponseEntity<EventDto> createEvent(
            @Valid @RequestBody EventCreateRequestDto eventFromCreationRequest,
            @RequestParam(defaultValue = "0") long workMs
    ) {
        log.info("Post request for creation a new event with locking: {}", eventFromCreationRequest);

        AuthUser user = getAuthenticatedUser();
        String lockKey = "events:create:user:" + user.id();
        String lockId = lockOrThrow(lockKey, Duration.ofSeconds(30));

        try {
            sleep(workMs);
            var createdEvent = eventService.createEventByUserId(user.id(), eventFromCreationRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDto(createdEvent));
        } finally {
            redisLockManager.unlockLock(lockKey, lockId);
        }
    }

    @PutMapping("/{id}")
    public EventDto updateEvent(
            @PathVariable("id") Long eventId,
            @RequestBody @Valid EventUpdateRequestDto eventFromUpdateRequest,
            @RequestParam(defaultValue = "0") long workMs
    ) {
        log.info("Put request for updating event with locking: {}", eventFromUpdateRequest);

        AuthUser user = getAuthenticatedUser();
        String lockKey = "events:update:" + eventId;
        String lockId = lockOrThrow(lockKey, Duration.ofSeconds(30));

        try {
            sleep(workMs);
            var updatedEvent = eventService.updateEvent(user.id(), eventId, eventFromUpdateRequest);
            return mapper.toDto(updatedEvent);
        } finally {
            redisLockManager.unlockLock(lockKey, lockId);
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEventById(
            @PathVariable("id") Long eventId,
            @RequestParam(defaultValue = "0") long workMs
    ) {
        log.info("Delete request for deleting event with locking, id: {}", eventId);

        AuthUser user = getAuthenticatedUser();
        String lockKey = "events:delete:" + eventId;
        String lockId = lockOrThrow(lockKey, Duration.ofSeconds(30));

        try {
            sleep(workMs);
            eventService.deleteEventById(user.id(), eventId);
        } finally {
            redisLockManager.unlockLock(lockKey, lockId);
        }
    }

    @PostMapping("/registrations/{id}")
    public EventDto registerUserOnEvent(
            @PathVariable("id") Long eventId,
            @RequestParam(defaultValue = "0") long workMs
    ) {
        log.info("Post request for register a new user on event with locking, eventId: {}", eventId);

        AuthUser user = getAuthenticatedUser();
        String lockKey = "events:registration:create:event:" + eventId;
        String lockId = lockOrThrow(lockKey, Duration.ofSeconds(30));

        try {
            sleep(workMs);
            var eventThatUserRegisteredOn = eventService.registerOnEvent(user.id(), eventId);
            return mapper.toDto(eventThatUserRegisteredOn);
        } finally {
            redisLockManager.unlockLock(lockKey, lockId);
        }
    }

    @DeleteMapping("/registrations/cancel/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEventRegistrationRequest(
            @PathVariable("id") Long eventId,
            @RequestParam(defaultValue = "0") long workMs
    ) {
        log.info("Delete request for cancel event registration with locking, eventId: {}", eventId);

        AuthUser user = getAuthenticatedUser();
        String lockKey = "events:registration:cancel:event:" + eventId;
        String lockId = lockOrThrow(lockKey, Duration.ofSeconds(30));

        try {
            sleep(workMs);
            eventService.cancelEventRegistrationRequestById(user.id(), eventId);
        } finally {
            redisLockManager.unlockLock(lockKey, lockId);
        }
    }

    private String lockOrThrow(String lockKey, Duration ttl) {
        String lockId = redisLockManager.tryLock(lockKey, ttl);
        if (isNull(lockId)) {
            throw new ResponseStatusException(
                    HttpStatus.LOCKED,
                    "Lock has been captured for object %s. Try later".formatted(lockKey)
            );
        }
        return lockId;
    }

    private void sleep(long workMs) {
        if (workMs <= 0) {
            return;
        }
        try {
            Thread.sleep(workMs);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private AuthUser getAuthenticatedUser() {
        return authenticationService.getCurrentAuthenticatedUser();
    }
}
