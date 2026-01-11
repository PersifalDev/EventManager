package ru.haritonenko.eventnotificator.security.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import ru.haritonenko.commonlibs.securirty.user.AuthUser;

import static java.util.Objects.isNull;

@Slf4j
@Service
public class AuthUserService {

    public AuthUser getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (isNull(auth)|| isNull(auth.getPrincipal())) {
            log.warn("Error while getting authenticated user");
            throw new IllegalStateException("Authentication not present");
        }
        return (AuthUser) auth.getPrincipal();
    }
}
