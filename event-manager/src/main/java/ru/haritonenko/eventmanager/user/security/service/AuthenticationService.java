package ru.haritonenko.eventmanager.user.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import ru.haritonenko.eventmanager.user.api.dto.authorization.UserCredentials;
import ru.haritonenko.eventmanager.user.security.jwt.JwtTokenManager;
import ru.haritonenko.eventmanager.user.domain.User;

import static java.util.Objects.isNull;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;

    private final JwtTokenManager jwtTokenManager;

    public String authenticate(UserCredentials userFromSignInRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        userFromSignInRequest.login(),
                        userFromSignInRequest.password()
                )
        );
        log.info("Generating jwt token");
        return jwtTokenManager.generateToken(userFromSignInRequest.login());
    }

    public User getCurrentAuthenticatedUser() {
        log.info("Getting authenticated user");
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (isNull(authentication)) {
            log.warn("Error while getting authenticated user");
            throw new IllegalStateException("Authentication not present");
        }
        return (User) authentication.getPrincipal();
    }

}
