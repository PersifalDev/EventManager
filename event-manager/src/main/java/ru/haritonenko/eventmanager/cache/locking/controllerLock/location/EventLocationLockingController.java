package ru.haritonenko.eventmanager.cache.locking.controllerLock.location;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.haritonenko.eventmanager.cache.locking.manager.RedisLockManager;
import ru.haritonenko.eventmanager.location.api.dto.EventLocationUpdateRequestDto;
import ru.haritonenko.eventmanager.location.api.dto.EventLocationDto;
import ru.haritonenko.eventmanager.location.domain.mapper.EventLocationDtoMapper;
import ru.haritonenko.eventmanager.location.domain.service.EventLocationService;

import java.time.Duration;

import static java.util.Objects.isNull;

@Profile("dev")
@Slf4j
@RestController
@RequestMapping("/locations/lock")
@RequiredArgsConstructor
public class EventLocationLockingController {

    private final EventLocationService locationService;
    private final EventLocationDtoMapper mapper;
    private final RedisLockManager redisLockManager;

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public EventLocationDto updateLocation(
            @PathVariable Long id,
            @RequestBody @Valid EventLocationUpdateRequestDto request,
            @RequestParam(defaultValue = "500") long workMs
    ) {
        String lockKey = "locations:update:" + id;
        String lockId = redisLockManager.tryLock(lockKey, Duration.ofSeconds(30));

        if (isNull(lockId)) {
            throw new ResponseStatusException(
                    HttpStatus.LOCKED,
                    "Lock has been captured for object %s. Try later".formatted(lockKey)
            );
        }
        try {
            if (workMs > 0) {
                try {
                    Thread.sleep(workMs);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
            var updated = locationService.updateLocation(id, mapper.fromUpdateDto(request));
            return mapper.toDto(updated);
        } finally {
            redisLockManager.unlockLock(lockKey, lockId);
        }
    }
}
