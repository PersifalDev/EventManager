package ru.haritonenko.eventmanager.user.domain.db.repository;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ru.haritonenko.eventmanager.location.domain.db.AbstractJpaTest;
import ru.haritonenko.eventmanager.user.domain.db.entity.UserEntity;
import ru.haritonenko.eventmanager.user.domain.role.UserRole;

import java.util.ArrayList;
import java.util.Optional;

@DataJpaTest
class UserRepositoryTest extends AbstractJpaTest {

    @Autowired
    private UserRepository userRepository;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        testUser = UserEntity.builder()
                .login("test-login")
                .password("test-password")
                .age(20)
                .userRole(UserRole.USER)
                .ownEvents(new ArrayList<>())
                .registrations(new ArrayList<>())
                .build();
    }

    /** happy paths **/

    @Test
    void shouldSaveUserAndGenerateId() {

        UserEntity savedUser = userRepository.save(testUser);

        assertNotNull(savedUser.getId());
        assertEquals("test-login", savedUser.getLogin());
        assertEquals("test-password", savedUser.getPassword());
        assertEquals(20, savedUser.getAge());
        assertEquals(UserRole.USER, savedUser.getUserRole());
    }

    @Test
    void shouldFindSavedUserById() {

        UserEntity savedUser = userRepository.save(testUser);

        Optional<UserEntity> foundUserOpt = userRepository.findById(savedUser.getId());

        assertTrue(foundUserOpt.isPresent());

        UserEntity foundUser = foundUserOpt.get();

        assertEquals(savedUser.getId(), foundUser.getId());
        assertEquals("test-login", foundUser.getLogin());
        assertEquals("test-password", foundUser.getPassword());
        assertEquals(20, foundUser.getAge());
        assertEquals(UserRole.USER, foundUser.getUserRole());
    }

    @Test
    void shouldFindSavedUserByLogin() {

        UserEntity savedUser = userRepository.save(testUser);

        Optional<UserEntity> foundUserOpt = userRepository.findByLogin(savedUser.getLogin());

        assertTrue(foundUserOpt.isPresent());

        UserEntity foundUser = foundUserOpt.get();

        assertEquals(savedUser.getId(), foundUser.getId());
        assertEquals("test-login", foundUser.getLogin());
        assertEquals("test-password", foundUser.getPassword());
        assertEquals(20, foundUser.getAge());
        assertEquals(UserRole.USER, foundUser.getUserRole());
    }

    @Test
    void shouldReturnTrueWhenUserExistsByLogin() {

        userRepository.save(testUser);

        boolean exists = userRepository.existsByLogin("test-login");

        assertTrue(exists);
    }

    @Test
    void shouldSuccessfullyUpdateUser() {

        UserEntity savedUser = userRepository.save(testUser);

        UserEntity foundUser = userRepository.findById(savedUser.getId()).orElseThrow();

        foundUser.setLogin("updated-login");
        foundUser.setPassword("updated-password");
        foundUser.setAge(25);
        foundUser.setUserRole(UserRole.ADMIN);

        userRepository.save(foundUser);

        UserEntity updatedUser = userRepository.findById(savedUser.getId()).orElseThrow();

        assertEquals("updated-login", updatedUser.getLogin());
        assertEquals("updated-password", updatedUser.getPassword());
        assertEquals(25, updatedUser.getAge());
        assertEquals(UserRole.ADMIN, updatedUser.getUserRole());
    }

    @Test
    void shouldSuccessfullyDeleteUser() {

        UserEntity savedUser = userRepository.save(testUser);

        Optional<UserEntity> foundUserBeforeDelete = userRepository.findById(savedUser.getId());

        assertTrue(foundUserBeforeDelete.isPresent());

        userRepository.deleteById(savedUser.getId());

        Optional<UserEntity> foundUserAfterDelete = userRepository.findById(savedUser.getId());

        assertTrue(foundUserAfterDelete.isEmpty());
    }

    /** negative paths **/

    @Test
    void shouldReturnEmptyOptionalWhenUserNotFoundById() {

        Optional<UserEntity> foundUserOpt = userRepository.findById(999999L);

        assertTrue(foundUserOpt.isEmpty());
    }

    @Test
    void shouldReturnEmptyOptionalWhenUserNotFoundByLogin() {

        Optional<UserEntity> foundUserOpt = userRepository.findByLogin("wrong-login");

        assertTrue(foundUserOpt.isEmpty());
    }

    @Test
    void shouldReturnFalseWhenUserNotExistsByLogin() {

        boolean exists = userRepository.existsByLogin("wrong-login");

        assertFalse(exists);
    }
}