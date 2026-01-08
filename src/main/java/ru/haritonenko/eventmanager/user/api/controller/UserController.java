package ru.haritonenko.eventmanager.user.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.haritonenko.eventmanager.user.security.service.AuthenticationService;
import ru.haritonenko.eventmanager.user.security.jwt.JwtResponse;
import ru.haritonenko.eventmanager.user.domain.converter.UserDtoConverter;
import ru.haritonenko.eventmanager.user.api.dto.UserDto;
import ru.haritonenko.eventmanager.user.api.dto.authorization.UserCredentials;
import ru.haritonenko.eventmanager.user.api.dto.registration.UserRegistration;
import ru.haritonenko.eventmanager.user.domain.service.UserService;

import java.util.Map;

import static java.util.Objects.isNull;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserDtoConverter converter;
    private final AuthenticationService jwtAuthenticationService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public UserDto getUserById(
            @PathVariable Integer id
    ) {
        log.info("Get request for getting user by id: {}", id);
        var foundUser = userService.getUserById(id);
        return converter.toDto(foundUser);
    }

    @PostMapping
    public ResponseEntity<UserDto> registerUser(
            @Valid @RequestBody UserRegistration userFromSignUpRequest
    ) {
        log.info("Post request for sign-up login: {}", userFromSignUpRequest.login());
        var registeredUser = userService.register(userFromSignUpRequest);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(converter.toDto(registeredUser));
    }

    @PostMapping("/auth")
    public ResponseEntity<JwtResponse> authenticateUser(
            @Valid @RequestBody UserCredentials userFromSignInRequest
    ) {
        log.info("Post request for authenticating login: {}", userFromSignInRequest.login());
        var token = jwtAuthenticationService.authenticate(userFromSignInRequest);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new JwtResponse(token));
    }

    @GetMapping("/debug/auth")
    public Object auth(Authentication authentication) {
        return Map.of(
                "name", isNull(authentication) ? null : authentication.getName(),
                "authorities", isNull(authentication) ? null : authentication.getAuthorities()
        );
    }
}
