package ru.haritonenko.eventmanager.user.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.haritonenko.eventmanager.user.api.dto.registration.UserRegistration;
import ru.haritonenko.eventmanager.user.domain.User;
import ru.haritonenko.eventmanager.user.domain.db.entity.UserEntity;
import ru.haritonenko.eventmanager.user.domain.db.repository.UserRepository;
import ru.haritonenko.eventmanager.user.domain.exception.UserAlreadyRegisteredException;
import ru.haritonenko.eventmanager.user.domain.exception.UserNotFoundException;
import ru.haritonenko.eventmanager.user.domain.mapper.UserEntityMapper;
import ru.haritonenko.eventmanager.user.domain.role.UserRole;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserEntityMapper mapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    /** happy paths **/

    @Test
    void shouldSuccessfullyGetUserById() {

        UserEntity userEntity = UserEntity.builder()
                .id(1L)
                .login("test-login")
                .password("test-password")
                .age(20)
                .userRole(UserRole.USER)
                .ownEvents(new ArrayList<>())
                .registrations(new ArrayList<>())
                .build();

        User userDomain = new User(
                1L,
                "test-login",
                20,
                UserRole.USER
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(userEntity));
        when(mapper.toDomain(userEntity)).thenReturn(userDomain);

        User foundUser = userService.getUserById(1L);

        assertEquals(userDomain.id(), foundUser.id());
        assertEquals(userDomain.login(), foundUser.login());
        assertEquals(userDomain.age(), foundUser.age());
        assertEquals(userDomain.role(), foundUser.role());
    }

    @Test
    void shouldSuccessfullyRegisterUser() {

        UserRegistration userRegistration = new UserRegistration(
                "test-login",
                "test-password",
                20
        );

        UserEntity userToSave = UserEntity.builder()
                .login("test-login")
                .password("hashed-password")
                .age(20)
                .userRole(UserRole.USER)
                .ownEvents(new ArrayList<>())
                .registrations(new ArrayList<>())
                .build();

        UserEntity savedUserEntity = UserEntity.builder()
                .id(1L)
                .login("test-login")
                .password("hashed-password")
                .age(20)
                .userRole(UserRole.USER)
                .ownEvents(new ArrayList<>())
                .registrations(new ArrayList<>())
                .build();

        User savedUserDomain = new User(
                1L,
                "test-login",
                20,
                UserRole.USER
        );

        when(userRepository.existsByLogin("test-login")).thenReturn(false);
        when(passwordEncoder.encode("test-password")).thenReturn("hashed-password");
        when(mapper.toEntity(userRegistration, "hashed-password")).thenReturn(userToSave);
        when(userRepository.save(userToSave)).thenReturn(savedUserEntity);
        when(mapper.toDomain(savedUserEntity)).thenReturn(savedUserDomain);

        User registeredUser = userService.register(userRegistration);

        assertNotNull(registeredUser.id());
        assertEquals("test-login", registeredUser.login());
        assertEquals(20, registeredUser.age());
        assertEquals(UserRole.USER, registeredUser.role());

        verify(passwordEncoder).encode("test-password");
        verify(userRepository).save(userToSave);
    }

    @Test
    void shouldSuccessfullyFindUserByLogin() {

        UserEntity userEntity = UserEntity.builder()
                .id(1L)
                .login("test-login")
                .password("test-password")
                .age(20)
                .userRole(UserRole.USER)
                .ownEvents(new ArrayList<>())
                .registrations(new ArrayList<>())
                .build();

        User userDomain = new User(
                1L,
                "test-login",
                20,
                UserRole.USER
        );

        when(userRepository.findByLogin("test-login")).thenReturn(Optional.of(userEntity));
        when(mapper.toDomain(userEntity)).thenReturn(userDomain);

        User foundUser = userService.findByLogin("test-login");

        assertEquals(userDomain.id(), foundUser.id());
        assertEquals(userDomain.login(), foundUser.login());
        assertEquals(userDomain.age(), foundUser.age());
        assertEquals(userDomain.role(), foundUser.role());
    }

    /** negative paths **/

    @Test
    void shouldThrowUserNotFoundExceptionWhenUserNotFoundById() {

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> userService.getUserById(999L)
        );

        assertEquals("No found user by id = 999", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenRegistrationIsNull() {

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.register(null)
        );

        assertEquals("Registration can't be null", exception.getMessage());
    }

    @Test
    void shouldThrowUserAlreadyRegisteredExceptionWhenUserAlreadyExists() {

        UserRegistration userRegistration = new UserRegistration(
                "test-login",
                "test-password",
                20
        );

        when(userRepository.existsByLogin("test-login")).thenReturn(true);

        UserAlreadyRegisteredException exception = assertThrows(UserAlreadyRegisteredException.class,
                () -> userService.register(userRegistration)
        );

        assertEquals("This user has already registered", exception.getMessage());
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenUserNotFoundByLogin() {

        when(userRepository.findByLogin("wrong-login")).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> userService.findByLogin("wrong-login")
        );

        assertEquals("User not found", exception.getMessage());
    }
}