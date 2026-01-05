package ru.haritonenko.eventmanager.user.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.haritonenko.eventmanager.user.domain.converter.UserEntityConverter;
import ru.haritonenko.eventmanager.user.domain.User;
import ru.haritonenko.eventmanager.user.api.dto.registration.UserRegistration;
import ru.haritonenko.eventmanager.user.domain.db.entity.UserEntity;
import ru.haritonenko.eventmanager.user.domain.exception.UserAlreadyRegisteredException;
import ru.haritonenko.eventmanager.user.domain.exception.UserNotFoundException;
import ru.haritonenko.eventmanager.user.domain.db.repository.UserRepository;
import ru.haritonenko.eventmanager.user.domain.role.UserRole;

import java.util.ArrayList;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserEntityConverter converter;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public User getUserById(Integer id) {
        log.info("Getting user by id: {}", id);
        var foundUser = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Error while getting user by id");
                    return new UserNotFoundException(
                            "No found user by id = %s".formatted(id));
                });
        log.info("User was successfully found by id: {}", id);
        return converter.toDomain(foundUser);
    }

    @Transactional
    public User register(UserRegistration userFromRegistration) {
        log.info("User registration started");
        if (userRepository.existsByLogin(userFromRegistration.login())) {
            log.warn("Error while register user");
            throw new UserAlreadyRegisteredException("This user has already registered");
        }
        var hashedPass = passwordEncoder.encode(userFromRegistration.password());
        var userToSave = new UserEntity(
                null,
                userFromRegistration.login(),
                hashedPass,
                userFromRegistration.age(),
                UserRole.USER,
                new ArrayList<>(),
                new ArrayList<>()
        );
        var savedUserEntity = userRepository.save(userToSave);
        log.info("User has successfully registered");
        return converter.toDomain(savedUserEntity);
    }

    public User findByLogin(String login) {
        log.info("Searching for user by login: {}", login);
        var foundUser = userRepository.findByLogin(login)
                .orElseThrow(() -> {
                    log.warn("Error while finding user by login");
                    return new UserNotFoundException("User not found");
                });
        log.info("User was successfully found by login: {}", login);
        return converter.toDomain(foundUser);
    }
}
