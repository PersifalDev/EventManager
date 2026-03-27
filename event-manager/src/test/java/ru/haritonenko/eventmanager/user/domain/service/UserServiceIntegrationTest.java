package ru.haritonenko.eventmanager.user.domain.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ru.haritonenko.eventmanager.AbstractIntegrationTest;
import ru.haritonenko.eventmanager.user.api.dto.registration.UserRegistration;
import ru.haritonenko.eventmanager.user.domain.User;
import ru.haritonenko.eventmanager.user.domain.db.entity.UserEntity;
import ru.haritonenko.eventmanager.user.domain.db.repository.UserRepository;
import ru.haritonenko.eventmanager.user.domain.exception.UserAlreadyRegisteredException;
import ru.haritonenko.eventmanager.user.domain.exception.UserNotFoundException;
import ru.haritonenko.eventmanager.user.domain.role.UserRole;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    /** happy paths **/

    @Transactional
    @Test
    void shouldSuccessfullyGetUserById() {

        UserEntity savedUser = getSavedDummyUser("test-login");

        User foundUser = userService.getUserById(savedUser.getId());

        assertEquals(savedUser.getId(), foundUser.id());
        assertEquals(savedUser.getLogin(), foundUser.login());
        assertEquals(savedUser.getAge(), foundUser.age());
        assertEquals(savedUser.getUserRole(), foundUser.role());
    }

    @Transactional
    @Test
    void shouldSuccessfullyRegisterUser() {

        UserRegistration userRegistration = new UserRegistration(
                "registered-login",
                "registered-password",
                21
        );

        User registeredUser = userService.register(userRegistration);

        assertNotNull(registeredUser.id());
        assertEquals("registered-login", registeredUser.login());
        assertEquals(21, registeredUser.age());
        assertEquals(UserRole.USER, registeredUser.role());

        UserEntity savedUserEntity = userRepository.findById(registeredUser.id()).orElseThrow();

        assertEquals("registered-login", savedUserEntity.getLogin());
        assertEquals(21, savedUserEntity.getAge());
        assertEquals(UserRole.USER, savedUserEntity.getUserRole());
        assertNotEquals("registered-password", savedUserEntity.getPassword());
    }

    @Transactional
    @Test
    void shouldSuccessfullyFindUserByLogin() {

        UserEntity savedUser = getSavedDummyUser("test-login");

        User foundUser = userService.findByLogin("test-login");

        assertEquals(savedUser.getId(), foundUser.id());
        assertEquals(savedUser.getLogin(), foundUser.login());
        assertEquals(savedUser.getAge(), foundUser.age());
        assertEquals(savedUser.getUserRole(), foundUser.role());
    }

    /** negative paths **/

    @Transactional
    @Test
    void shouldThrowUserNotFoundExceptionWhenUserNotFoundById() {

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> userService.getUserById(999999L)
        );

        assertEquals("No found user by id = 999999", exception.getMessage());
    }

    @Transactional
    @Test
    void shouldThrowUserAlreadyRegisteredExceptionWhenUserAlreadyExists() {

        getSavedDummyUser("test-login");

        UserRegistration userRegistration = new UserRegistration(
                "test-login",
                "test-password",
                20
        );

        UserAlreadyRegisteredException exception = assertThrows(UserAlreadyRegisteredException.class,
                () -> userService.register(userRegistration)
        );

        assertEquals("This user has already registered", exception.getMessage());
    }

    @Transactional
    @Test
    void shouldThrowUserNotFoundExceptionWhenUserNotFoundByLogin() {

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> userService.findByLogin("wrong-login")
        );

        assertEquals("User not found", exception.getMessage());
    }

    private UserEntity getSavedDummyUser(String login) {
        return userRepository.save(
                UserEntity.builder()
                        .login(login)
                        .password("test-password")
                        .age(20)
                        .userRole(UserRole.USER)
                        .ownEvents(new java.util.ArrayList<>())
                        .registrations(new java.util.ArrayList<>())
                        .build()
        );
    }
}