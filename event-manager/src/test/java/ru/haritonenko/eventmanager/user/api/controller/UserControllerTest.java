package ru.haritonenko.eventmanager.user.api.controller;

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
import ru.haritonenko.eventmanager.user.api.dto.UserDto;
import ru.haritonenko.eventmanager.user.api.dto.authorization.UserCredentials;
import ru.haritonenko.eventmanager.user.api.dto.registration.UserRegistration;
import ru.haritonenko.eventmanager.user.domain.User;
import ru.haritonenko.eventmanager.user.domain.mapper.UserDtoMapper;
import ru.haritonenko.eventmanager.user.domain.role.UserRole;
import ru.haritonenko.eventmanager.user.domain.service.UserService;
import ru.haritonenko.eventmanager.user.security.configuration.SecurityConfiguration;
import ru.haritonenko.eventmanager.user.security.custom.authentification.CustomAuthenticationEntryPoint;
import ru.haritonenko.eventmanager.user.security.custom.handler.CustomAccessDeniedHandler;
import ru.haritonenko.eventmanager.user.security.custom.service.CustomUserDetailsService;
import ru.haritonenko.eventmanager.user.security.jwt.filter.JwtTokenFilter;
import ru.haritonenko.eventmanager.user.security.jwt.manager.JwtTokenManager;
import ru.haritonenko.eventmanager.user.security.service.AuthenticationService;

import java.io.IOException;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({
        SecurityConfiguration.class,
        UserControllerTest.TestConfig.class
})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private UserDtoMapper userDtoMapper;

    @Autowired
    private AuthenticationService authenticationService;

    @TestConfiguration
    static class TestConfig {

        @Bean
        UserService userService() {
            return Mockito.mock(UserService.class);
        }

        @Bean
        UserDtoMapper userDtoMapper() {
            return Mockito.mock(UserDtoMapper.class);
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

    /** happy paths **/

    @Test
    @WithMockUser(authorities = "ADMIN")
    void shouldSuccessfullyGetUserById() throws Exception {

        User user = new User(
                1L,
                "test-login",
                20,
                UserRole.USER
        );

        UserDto userDto = new UserDto(
                1,
                "test-login"
        );

        when(userService.getUserById(1L)).thenReturn(user);
        when(userDtoMapper.toDto(user)).thenReturn(userDto);

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.login").value("test-login"));
    }

    @Test
    void shouldSuccessfullyRegisterUser() throws Exception {

        UserRegistration requestDto = new UserRegistration(
                "test-login",
                "test-password",
                20
        );

        User registeredUser = new User(
                1L,
                "test-login",
                20,
                UserRole.USER
        );

        UserDto registeredUserDto = new UserDto(
                1,
                "test-login"
        );

        when(userService.register(requestDto)).thenReturn(registeredUser);
        when(userDtoMapper.toDto(registeredUser)).thenReturn(registeredUserDto);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.login").value("test-login"));
    }

    @Test
    void shouldSuccessfullyAuthenticateUser() throws Exception {

        UserCredentials requestDto = new UserCredentials(
                "test-login",
                "test-password"
        );

        when(authenticationService.authenticate(requestDto)).thenReturn("test-jwt-token");

        mockMvc.perform(post("/users/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwt").value("test-jwt-token"));
    }

    /** security **/

    @Test
    void shouldReturnUnauthorizedWhenUserIsNotAuthenticated() throws Exception {

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "USER")
    void shouldReturnForbiddenWhenUserTryingGetUserById() throws Exception {

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isForbidden());
    }

    /** validation **/

    @Test
    void shouldReturnBadRequestWhenRegisterBodyIsInvalid() throws Exception {

        UserRegistration requestDto = new UserRegistration(
                "",
                "",
                10
        );

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenAuthenticateBodyIsInvalid() throws Exception {

        UserCredentials requestDto = new UserCredentials(
                "",
                ""
        );

        mockMvc.perform(post("/users/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
    }
}