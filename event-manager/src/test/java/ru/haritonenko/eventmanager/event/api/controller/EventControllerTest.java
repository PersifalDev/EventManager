package ru.haritonenko.eventmanager.event.api.controller;

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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import ru.haritonenko.commonlibs.securirty.user.AuthUser;
import ru.haritonenko.eventmanager.event.api.dto.EventCreateRequestDto;
import ru.haritonenko.eventmanager.event.api.dto.EventDto;
import ru.haritonenko.eventmanager.event.api.dto.EventUpdateRequestDto;
import ru.haritonenko.eventmanager.event.domain.Event;
import ru.haritonenko.eventmanager.event.domain.mapper.EventDtoMapper;
import ru.haritonenko.eventmanager.event.domain.service.EventService;
import ru.haritonenko.eventmanager.event.domain.status.EventStatus;
import ru.haritonenko.eventmanager.user.security.configuration.SecurityConfiguration;
import ru.haritonenko.eventmanager.user.security.custom.authentification.CustomAuthenticationEntryPoint;
import ru.haritonenko.eventmanager.user.security.custom.handler.CustomAccessDeniedHandler;
import ru.haritonenko.eventmanager.user.security.custom.service.CustomUserDetailsService;
import ru.haritonenko.eventmanager.user.security.jwt.filter.JwtTokenFilter;
import ru.haritonenko.eventmanager.user.security.jwt.manager.JwtTokenManager;
import ru.haritonenko.eventmanager.user.security.service.AuthenticationService;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@Import({
        SecurityConfiguration.class,
        EventControllerTest.TestConfig.class
})
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventService eventService;

    @Autowired
    private EventDtoMapper eventDtoMapper;

    @Autowired
    private AuthenticationService authenticationService;

    @TestConfiguration
    static class TestConfig {

        @Bean
        EventService eventService() {
            return Mockito.mock(EventService.class);
        }

        @Bean
        EventDtoMapper eventDtoMapper() {
            return Mockito.mock(EventDtoMapper.class);
        }

        @Bean
        AuthenticationService authenticationService() {
            return Mockito.mock(AuthenticationService.class);
        }

        @Bean
        CustomUserDetailsService customUserDetailsService() {
            return Mockito.mock(CustomUserDetailsService.class);
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
                       AccessDeniedException accessDeniedException
                ) throws IOException {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
                }
            };
        }
    }

    @Test
    @WithMockUser(authorities = "USER")
    void shouldSuccessfullyGetEventById() throws Exception {
        Event event = new Event(
                1L,
                "test-event",
                "1",
                100,
                0,
                "2099-12-20T10:00:00",
                BigDecimal.valueOf(1500),
                120,
                10L,
                EventStatus.WAIT_START
        );

        EventDto eventDto = new EventDto(
                null,
                "test-event",
                "1",
                100,
                0,
                "2099-12-20T10:00:00",
                BigDecimal.valueOf(1500),
                120,
                10L,
                EventStatus.WAIT_START
        );

        when(eventService.getEventById(1L)).thenReturn(event);
        when(eventDtoMapper.toDto(event)).thenReturn(eventDto);

        mockMvc.perform(get("/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test-event"))
                .andExpect(jsonPath("$.ownerId").value("1"))
                .andExpect(jsonPath("$.locationId").value(10))
                .andExpect(jsonPath("$.status").value("WAIT_START"));
    }

    @Test
    @WithMockUser(authorities = "USER")
    void shouldSuccessfullyCreateEvent() throws Exception {
        EventCreateRequestDto requestDto = new EventCreateRequestDto(
                "created-event",
                120,
                "2099-12-25T15:00:00",
                1700,
                90,
                10L
        );

        Event createdEvent = new Event(
                100L,
                "created-event",
                "1",
                120,
                0,
                "2099-12-25T15:00:00",
                BigDecimal.valueOf(1700),
                90,
                10L,
                EventStatus.WAIT_START
        );

        EventDto createdEventDto = new EventDto(
                null,
                "created-event",
                "1",
                120,
                0,
                "2099-12-25T15:00:00",
                BigDecimal.valueOf(1700),
                90,
                10L,
                EventStatus.WAIT_START
        );

        when(authenticationService.getCurrentAuthenticatedUser())
                .thenReturn(new AuthUser(1L, "user", "USER"));
        when(eventService.createEventByUserId(1L, requestDto)).thenReturn(createdEvent);
        when(eventDtoMapper.toDto(createdEvent)).thenReturn(createdEventDto);

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("created-event"))
                .andExpect(jsonPath("$.ownerId").value("1"))
                .andExpect(jsonPath("$.locationId").value(10))
                .andExpect(jsonPath("$.status").value("WAIT_START"));
    }

    @Test
    @WithMockUser(authorities = "USER")
    void shouldSuccessfullyUpdateEvent() throws Exception {
        EventUpdateRequestDto requestDto = new EventUpdateRequestDto(
                "updated-event",
                150,
                "2099-12-30T16:00:00",
                BigDecimal.valueOf(2200),
                180,
                10L
        );

        Event updatedEvent = new Event(
                1L,
                "updated-event",
                "1",
                150,
                0,
                "2099-12-30T16:00:00",
                BigDecimal.valueOf(2200),
                180,
                10L,
                EventStatus.WAIT_START
        );

        EventDto updatedEventDto = new EventDto(
                null,
                "updated-event",
                "1",
                150,
                0,
                "2099-12-30T16:00:00",
                BigDecimal.valueOf(2200),
                180,
                10L,
                EventStatus.WAIT_START
        );

        when(authenticationService.getCurrentAuthenticatedUser())
                .thenReturn(new AuthUser(1L, "user", "USER"));
        when(eventService.updateEvent(1L, 1L, requestDto)).thenReturn(updatedEvent);
        when(eventDtoMapper.toDto(updatedEvent)).thenReturn(updatedEventDto);

        mockMvc.perform(put("/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("updated-event"))
                .andExpect(jsonPath("$.maxPlaces").value(150))
                .andExpect(jsonPath("$.duration").value(180))
                .andExpect(jsonPath("$.status").value("WAIT_START"));
    }

    @Test
    @WithMockUser(authorities = "USER")
    void shouldSuccessfullyGetCreatedEventsByUser() throws Exception {
        Event firstEvent = new Event(
                1L,
                "first-event",
                "1",
                100,
                0,
                "2099-12-20T10:00:00",
                BigDecimal.valueOf(1500),
                120,
                10L,
                EventStatus.WAIT_START
        );

        Event secondEvent = new Event(
                2L,
                "second-event",
                "1",
                80,
                0,
                "2099-12-21T10:00:00",
                BigDecimal.valueOf(1200),
                90,
                10L,
                EventStatus.WAIT_START
        );

        EventDto firstDto = new EventDto(
                null,
                "first-event",
                "1",
                100,
                0,
                "2099-12-20T10:00:00",
                BigDecimal.valueOf(1500),
                120,
                10L,
                EventStatus.WAIT_START
        );

        EventDto secondDto = new EventDto(
                null,
                "second-event",
                "1",
                80,
                0,
                "2099-12-21T10:00:00",
                BigDecimal.valueOf(1200),
                90,
                10L,
                EventStatus.WAIT_START
        );

        when(authenticationService.getCurrentAuthenticatedUser())
                .thenReturn(new AuthUser(1L, "user", "USER"));
        when(eventService.findEventsCreatedByUser(any(), any())).thenReturn(List.of(firstEvent, secondEvent));
        when(eventDtoMapper.toDto(firstEvent)).thenReturn(firstDto);
        when(eventDtoMapper.toDto(secondEvent)).thenReturn(secondDto);

        mockMvc.perform(get("/events/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("first-event"))
                .andExpect(jsonPath("$[1].name").value("second-event"));
    }

    @Test
    @WithMockUser(authorities = "USER")
    void shouldSuccessfullyDeleteEvent() throws Exception {
        when(authenticationService.getCurrentAuthenticatedUser())
                .thenReturn(new AuthUser(1L, "user", "USER"));

        mockMvc.perform(delete("/events/1"))
                .andExpect(status().isNoContent());

        verify(eventService).deleteEventById(1L, 1L);
    }

    @Test
    void shouldReturnUnauthorizedWhenUserIsNotAuthenticated() throws Exception {
        mockMvc.perform(get("/events/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void shouldReturnForbiddenWhenAdminTryingCreateEvent() throws Exception {
        EventCreateRequestDto requestDto = new EventCreateRequestDto(
                "created-event",
                120,
                "2099-12-25T15:00:00",
                1700,
                90,
                10L
        );

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void shouldReturnBadRequestWhenCreateEventBodyIsInvalid() throws Exception {
        EventCreateRequestDto invalidRequestDto = new EventCreateRequestDto(
                "",
                -1,
                "",
                -10,
                10,
                0L
        );

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequestDto)))
                .andExpect(status().isBadRequest());
    }
}