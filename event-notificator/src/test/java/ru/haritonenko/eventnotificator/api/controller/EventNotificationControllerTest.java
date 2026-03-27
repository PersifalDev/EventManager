package ru.haritonenko.eventnotificator.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import ru.haritonenko.commonlibs.securirty.user.AuthUser;
import ru.haritonenko.eventnotificator.api.dto.EventNotificationDto;
import ru.haritonenko.eventnotificator.api.dto.MarkNotificationsReadRequest;
import ru.haritonenko.eventnotificator.domain.EventNotification;
import ru.haritonenko.eventnotificator.domain.mapper.EventNotificationDtoMapper;
import ru.haritonenko.eventnotificator.domain.service.EventNotificationService;
import ru.haritonenko.eventnotificator.security.configuration.SecurityConfiguration;
import ru.haritonenko.eventnotificator.security.custom.authentification.CustomAuthenticationEntryPoint;
import ru.haritonenko.eventnotificator.security.custom.handler.CustomAccessDeniedHandler;
import ru.haritonenko.eventnotificator.security.jwt.filter.JwtTokenFilter;
import ru.haritonenko.eventnotificator.security.jwt.manager.JwtTokenManager;
import ru.haritonenko.eventnotificator.security.service.AuthUserService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventNotificationController.class)
@Import({
        SecurityConfiguration.class,
        EventNotificationControllerTest.TestConfig.class
})
class EventNotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventNotificationService eventNotificationService;

    @Autowired
    private EventNotificationDtoMapper eventNotificationDtoMapper;

    @Autowired
    private AuthUserService authUserService;

    @TestConfiguration
    static class TestConfig {

        @Bean
        EventNotificationService eventNotificationService() {
            return Mockito.mock(EventNotificationService.class);
        }

        @Bean
        EventNotificationDtoMapper eventNotificationDtoMapper() {
            return Mockito.mock(EventNotificationDtoMapper.class);
        }

        @Bean
        AuthUserService authUserService() {
            return Mockito.mock(AuthUserService.class);
        }

        @Bean
        JwtTokenManager jwtTokenManager() {
            return Mockito.mock(JwtTokenManager.class);
        }

        @Bean
        JwtTokenFilter jwtTokenFilter(JwtTokenManager jwtTokenManager) {
            return new JwtTokenFilter(jwtTokenManager) {
                @Override
                protected void doFilterInternal(
                        HttpServletRequest request,
                        HttpServletResponse response,
                        FilterChain filterChain
                ) throws ServletException, IOException {
                    filterChain.doFilter(request, response);
                }
            };
        }

        @Bean
        CustomAuthenticationEntryPoint customAuthenticationEntryPoint(ObjectMapper objectMapper) {
            return new CustomAuthenticationEntryPoint(objectMapper) {
                @Override
                public void commence(
                        HttpServletRequest request,
                        HttpServletResponse response,
                        AuthenticationException authException
                ) throws IOException {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                }
            };
        }

        @Bean
        CustomAccessDeniedHandler customAccessDeniedHandler(ObjectMapper objectMapper) {
            return new CustomAccessDeniedHandler(objectMapper) {
                @Override
                public void handle(
                        HttpServletRequest request,
                        HttpServletResponse response,
                        org.springframework.security.access.AccessDeniedException accessDeniedException
                ) throws IOException {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
                }
            };
        }
    }

    @Test
    @WithMockUser(authorities = "USER")
    void shouldSuccessfullyGetUnreadNotificationsForUser() throws Exception {
        AuthUser authUser = new AuthUser(1L, "test-login", "USER");

        EventNotification firstNotification = new EventNotification(
                1L,
                1L,
                10L,
                LocalDateTime.of(2099, 12, 20, 10, 0),
                false,
                "first-message"
        );

        EventNotification secondNotification = new EventNotification(
                2L,
                1L,
                11L,
                LocalDateTime.of(2099, 12, 21, 10, 0),
                false,
                "second-message"
        );

        EventNotificationDto firstDto = new EventNotificationDto(
                1L,
                10L,
                LocalDateTime.of(2099, 12, 20, 10, 0),
                false,
                "first-message"
        );

        EventNotificationDto secondDto = new EventNotificationDto(
                2L,
                11L,
                LocalDateTime.of(2099, 12, 21, 10, 0),
                false,
                "second-message"
        );

        when(authUserService.getCurrentAuthenticatedUser()).thenReturn(authUser);
        when(eventNotificationService.findUnreadNotificationsForUser(any(), any()))
                .thenReturn(List.of(firstNotification, secondNotification));
        when(eventNotificationDtoMapper.toDto(firstNotification)).thenReturn(firstDto);
        when(eventNotificationDtoMapper.toDto(secondNotification)).thenReturn(secondDto);

        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].eventId").value(10))
                .andExpect(jsonPath("$[0].read").value(false))
                .andExpect(jsonPath("$[0].message").value("first-message"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].eventId").value(11))
                .andExpect(jsonPath("$[1].message").value("second-message"));
    }

    @Test
    @WithMockUser(authorities = "USER")
    void shouldSuccessfullyMarkNotificationsRead() throws Exception {
        AuthUser authUser = new AuthUser(1L, "test-login", "USER");

        MarkNotificationsReadRequest request = new MarkNotificationsReadRequest(List.of(1L, 2L, 3L));

        when(authUserService.getCurrentAuthenticatedUser()).thenReturn(authUser);

        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(eventNotificationService).markNotificationsAsRead(authUser, List.of(1L, 2L, 3L));
    }

    @Test
    void shouldReturnUnauthorizedWhenUserIsNotAuthenticated() throws Exception {
        mockMvc.perform(get("/notifications"))
                .andExpect(status().isUnauthorized());
    }
}