package ru.haritonenko.eventmanager.user.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.haritonenko.eventmanager.user.api.dto.registration.UserRegistration;
import ru.haritonenko.eventmanager.user.domain.User;
import ru.haritonenko.eventmanager.user.domain.db.repository.UserRepository;
import ru.haritonenko.eventmanager.user.domain.exception.UserAlreadyRegisteredException;
import ru.haritonenko.eventmanager.user.domain.exception.UserNotFoundException;
import ru.haritonenko.eventmanager.user.domain.mapper.UserEntityMapper;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserEntityMapper mapper;
    private final PasswordEncoder passwordEncoder;

    @Cacheable(value = "users", key = "'id:' + #id")
    @Transactional(readOnly = true)
    public User getUserById(Integer id) {
        log.info("Getting user by id: {}", id);
        var foundUser = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Error while getting user by id");
                    return new UserNotFoundException("No found user by id = %s".formatted(id));
                });
        log.info("User was successfully found by id: {}", id);
        return mapper.toDomain(foundUser);
    }

    @Caching(put = {
            @CachePut(value = "users", key = "'id:' + #result.id()"),
            @CachePut(value = "users", key = "'login:' + #result.login()")
    })
    @Transactional
    public User register(UserRegistration userFromRegistration) {
        log.info("User registration started");
        if (userRepository.existsByLogin(userFromRegistration.login())) {
            log.warn("Error while register user");
            throw new UserAlreadyRegisteredException("This user has already registered");
        }
        var hashedPass = passwordEncoder.encode(userFromRegistration.password());
        var userToSave = mapper.toEntity(userFromRegistration, hashedPass);
        var savedUserEntity = userRepository.save(userToSave);
        log.info("User has successfully registered");
        return mapper.toDomain(savedUserEntity);
    }

    @Cacheable(value = "users", key = "'login:' + #login")
    @Transactional(readOnly = true)
    public User findByLogin(String login) {
        log.info("Searching for user by login: {}", login);
        var foundUser = userRepository.findByLogin(login)
                .orElseThrow(() -> {
                    log.warn("Error while finding user by login");
                    return new UserNotFoundException("User not found");
                });
        log.info("User was successfully found by login: {}", login);
        return mapper.toDomain(foundUser);
    }
}
