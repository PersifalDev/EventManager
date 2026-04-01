package ru.haritonenko.eventmanager.location.api.controller;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.haritonenko.eventmanager.location.api.dto.EventLocationCreateRequestDto;
import ru.haritonenko.eventmanager.location.api.dto.EventLocationDto;
import ru.haritonenko.eventmanager.location.api.dto.EventLocationUpdateRequestDto;
import ru.haritonenko.eventmanager.location.api.dto.filter.EventLocationSearchFilter;
import ru.haritonenko.eventmanager.location.domain.EventLocation;
import ru.haritonenko.eventmanager.location.domain.exception.LocationCountPlacesException;
import ru.haritonenko.eventmanager.location.domain.exception.LocationNotFoundException;
import ru.haritonenko.eventmanager.location.domain.mapper.EventLocationDtoMapper;
import ru.haritonenko.eventmanager.location.domain.service.EventLocationService;
import ru.haritonenko.eventmanager.user.security.configuration.SecurityConfiguration;
import ru.haritonenko.eventmanager.user.security.custom.authentification.CustomAuthenticationEntryPoint;
import ru.haritonenko.eventmanager.user.security.custom.handler.CustomAccessDeniedHandler;
import ru.haritonenko.eventmanager.user.security.custom.service.CustomUserDetailsService;
import ru.haritonenko.eventmanager.user.security.jwt.filter.JwtTokenFilter;
import ru.haritonenko.eventmanager.user.security.jwt.manager.JwtTokenManager;

import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventLocationController.class)
@Import({
        SecurityConfiguration.class,
        EventLocationControllerTest.TestConfig.class
})
class EventLocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EventLocationService locationService;

    @MockitoBean
    private EventLocationDtoMapper mapper;

    @TestConfiguration
    static class TestConfig {

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
    void shouldSuccessfullyGetLocationById() throws Exception {
        EventLocation location = EventLocation.builder()
                .id(1L)
                .name("test-location")
                .address("test-address")
                .capacity(100)
                .description("test-description")
                .build();

        EventLocationDto locationDto = new EventLocationDto(
                1,
                "test-location",
                "test-address",
                100,
                "test-description"
        );

        when(locationService.getLocationById(1L)).thenReturn(location);
        when(mapper.toDto(location)).thenReturn(locationDto);

        mockMvc.perform(get("/locations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("test-location"))
                .andExpect(jsonPath("$.address").value("test-address"))
                .andExpect(jsonPath("$.capacity").value(100))
                .andExpect(jsonPath("$.description").value("test-description"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void shouldSuccessfullyCreateLocation() throws Exception {
        EventLocationCreateRequestDto requestDto = new EventLocationCreateRequestDto(
                null,
                "new-location",
                "new-address",
                120,
                "new-description"
        );

        EventLocation locationToCreate = EventLocation.builder()
                .name("new-location")
                .address("new-address")
                .capacity(120)
                .description("new-description")
                .build();

        EventLocation createdLocation = EventLocation.builder()
                .id(1L)
                .name("new-location")
                .address("new-address")
                .capacity(120)
                .description("new-description")
                .build();

        EventLocationDto createdLocationDto = new EventLocationDto(
                1,
                "new-location",
                "new-address",
                120,
                "new-description"
        );

        when(mapper.fromCreateDto(requestDto)).thenReturn(locationToCreate);
        when(locationService.createLocation(locationToCreate)).thenReturn(createdLocation);
        when(mapper.toDto(createdLocation)).thenReturn(createdLocationDto);

        mockMvc.perform(post("/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("new-location"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void shouldSuccessfullyUpdateLocation() throws Exception {
        EventLocationUpdateRequestDto requestDto = new EventLocationUpdateRequestDto(
                null,
                "updated-location",
                "updated-address",
                150,
                "updated-description"
        );

        EventLocation locationToUpdate = EventLocation.builder()
                .name("updated-location")
                .address("updated-address")
                .capacity(150)
                .description("updated-description")
                .build();

        EventLocation updatedLocation = EventLocation.builder()
                .id(1L)
                .name("updated-location")
                .address("updated-address")
                .capacity(150)
                .description("updated-description")
                .build();

        EventLocationDto updatedLocationDto = new EventLocationDto(
                1,
                "updated-location",
                "updated-address",
                150,
                "updated-description"
        );

        when(mapper.fromUpdateDto(requestDto)).thenReturn(locationToUpdate);
        when(locationService.updateLocation(1L, locationToUpdate)).thenReturn(updatedLocation);
        when(mapper.toDto(updatedLocation)).thenReturn(updatedLocationDto);

        mockMvc.perform(put("/locations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("updated-location"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void shouldSuccessfullyDeleteLocationById() throws Exception {
        doNothing().when(locationService).deleteLocationById(1L);

        mockMvc.perform(delete("/locations/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void shouldSuccessfullySearchLocations() throws Exception {
        EventLocation firstLocation = EventLocation.builder()
                .id(1L)
                .name("location-1")
                .address("address-1")
                .capacity(100)
                .description("description-1")
                .build();

        EventLocation secondLocation = EventLocation.builder()
                .id(2L)
                .name("location-2")
                .address("address-2")
                .capacity(200)
                .description("description-2")
                .build();

        EventLocationDto firstLocationDto = new EventLocationDto(1, "location-1", "address-1", 100, "description-1");
        EventLocationDto secondLocationDto = new EventLocationDto(2, "location-2", "address-2", 200, "description-2");

        when(locationService.getAllLocations(any(EventLocationSearchFilter.class)))
                .thenReturn(List.of(firstLocation, secondLocation));
        when(mapper.toDto(firstLocation)).thenReturn(firstLocationDto);
        when(mapper.toDto(secondLocation)).thenReturn(secondLocationDto);

        mockMvc.perform(get("/locations")
                        .param("name", "checked-location")
                        .param("address", "address-1")
                        .param("pageNumber", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    void shouldReturnUnauthorizedWhenUserIsNotAuthenticated() throws Exception {
        mockMvc.perform(get("/locations/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void shouldReturnForbiddenWhenUserTryingCreateLocation() throws Exception {
        EventLocationCreateRequestDto requestDto = new EventLocationCreateRequestDto(
                null,
                "new-location",
                "new-address",
                120,
                "new-description"
        );

        mockMvc.perform(post("/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void shouldReturnBadRequestWhenSearchFilterIsInvalid() throws Exception {
        mockMvc.perform(get("/locations")
                        .param("address", "bad")
                        .param("pageNumber", "-1")
                        .param("pageSize", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void shouldReturnBadRequestWhenCreateBodyIsInvalid() throws Exception {
        EventLocationCreateRequestDto requestDto = new EventLocationCreateRequestDto(
                1,
                "",
                "bad",
                1,
                "short"
        );

        mockMvc.perform(post("/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void shouldReturnBadRequestWhenUpdateBodyIsInvalid() throws Exception {
        EventLocationUpdateRequestDto requestDto = new EventLocationUpdateRequestDto(
                1,
                "",
                "bad",
                1,
                "short"
        );

        mockMvc.perform(put("/locations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void shouldReturnNotFoundWhenLocationNotFound() throws Exception {
        when(locationService.getLocationById(999L))
                .thenThrow(new LocationNotFoundException("No found location by id = 999"));

        mockMvc.perform(get("/locations/999"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Location search error"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void shouldReturnBadRequestWhenLocationCapacityDecreaseIsForbidden() throws Exception {
        EventLocationUpdateRequestDto requestDto = new EventLocationUpdateRequestDto(
                null,
                "updated-location",
                "updated-address",
                50,
                "updated-description"
        );

        EventLocation locationToUpdate = EventLocation.builder()
                .name("updated-location")
                .address("updated-address")
                .capacity(50)
                .description("updated-description")
                .build();

        when(mapper.fromUpdateDto(requestDto)).thenReturn(locationToUpdate);
        when(locationService.updateLocation(eq(1L), eq(locationToUpdate)))
                .thenThrow(new LocationCountPlacesException(
                        "You can't decrease location capacity, because places might be occupied by users"
                ));

        mockMvc.perform(put("/locations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error while updating location"));
    }
}