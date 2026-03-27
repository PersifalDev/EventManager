package ru.haritonenko.eventmanager.user.security.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.haritonenko.commonlibs.securirty.user.AuthUser;
import ru.haritonenko.eventmanager.user.api.dto.authorization.UserCredentials;
import ru.haritonenko.eventmanager.user.domain.User;
import ru.haritonenko.eventmanager.user.domain.role.UserRole;
import ru.haritonenko.eventmanager.user.security.jwt.manager.JwtTokenManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import ru.haritonenko.eventmanager.user.domain.service.UserService;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceUnitTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenManager jwtTokenManager;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void shouldSuccessfullyAuthenticateUser() {

        UserCredentials credentials = new UserCredentials(
                "test-login",
                "test-password"
        );

        User user = new User(
                1L,
                "test-login",
                20,
                UserRole.USER
        );

        when(userService.findByLogin("test-login")).thenReturn(user);
        when(jwtTokenManager.generateToken(1L, "test-login", "USER")).thenReturn("test-jwt-token");

        String jwt = authenticationService.authenticate(credentials);

        assertEquals("test-jwt-token", jwt);
        verify(authenticationManager).authenticate(any());
        verify(userService).findByLogin("test-login");
        verify(jwtTokenManager).generateToken(1L, "test-login", "USER");
    }

    @Test
    void shouldSuccessfullyGetCurrentAuthenticatedUser() {

        AuthUser authUser = new AuthUser(
                1L,
                "test-login",
                "USER"
        );

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        authUser,
                        null,
                        List.of(new SimpleGrantedAuthority("USER"))
                );
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        AuthUser currentUser = authenticationService.getCurrentAuthenticatedUser();

        assertEquals(1L, currentUser.id());
        assertEquals("test-login", currentUser.login());
        assertEquals("USER", currentUser.role());

        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenAuthenticationNotPresent() {

        SecurityContextHolder.clearContext();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> authenticationService.getCurrentAuthenticatedUser()
        );

        assertEquals("Authentication not present", exception.getMessage());
    }
}