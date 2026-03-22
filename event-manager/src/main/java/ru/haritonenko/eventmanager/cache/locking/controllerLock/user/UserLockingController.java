package ru.haritonenko.eventmanager.cache.locking.controllerLock.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.haritonenko.eventmanager.cache.locking.manager.RedisLockManager;
import ru.haritonenko.eventmanager.user.api.dto.UserDto;
import ru.haritonenko.eventmanager.user.api.dto.authorization.UserCredentials;
import ru.haritonenko.eventmanager.user.api.dto.registration.UserRegistration;
import ru.haritonenko.eventmanager.user.domain.mapper.UserDtoMapper;
import ru.haritonenko.eventmanager.user.domain.service.UserService;
import ru.haritonenko.eventmanager.user.security.jwt.response.JwtResponse;
import ru.haritonenko.eventmanager.user.security.service.AuthenticationService;

import java.time.Duration;

import static java.util.Objects.isNull;

@Profile("dev")
@Slf4j
@RestController
@RequestMapping("/users/lock")
@RequiredArgsConstructor
public class UserLockingController {

    private final UserService userService;
    private final UserDtoMapper mapper;
    private final AuthenticationService jwtAuthenticationService;
    private final RedisLockManager redisLockManager;

    @PostMapping
    public ResponseEntity<UserDto> registerUser(
            @Valid @RequestBody UserRegistration userFromSignUpRequest,
            @RequestParam(defaultValue = "0") long workMs
    ) {
        log.info("Post request for sign-up with locking, login: {}", userFromSignUpRequest.login());

        String lockKey = "users:register:login:" + userFromSignUpRequest.login();
        String lockId = lockOrThrow(lockKey, Duration.ofSeconds(30));

        try {
            sleep(workMs);
            var registeredUser = userService.register(userFromSignUpRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDto(registeredUser));
        } finally {
            redisLockManager.unlockLock(lockKey, lockId);
        }
    }

    @PostMapping("/auth")
    public ResponseEntity<JwtResponse> authenticateUser(
            @Valid @RequestBody UserCredentials userFromSignInRequest,
            @RequestParam(defaultValue = "0") long workMs
    ) {
        log.info("Post request for authenticating with locking, login: {}", userFromSignInRequest.login());

        String lockKey = "users:auth:login:" + userFromSignInRequest.login();
        String lockId = lockOrThrow(lockKey, Duration.ofSeconds(10));

        try {
            sleep(workMs);
            var token = jwtAuthenticationService.authenticate(userFromSignInRequest);
            return ResponseEntity.status(HttpStatus.OK).body(new JwtResponse(token));
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
}
