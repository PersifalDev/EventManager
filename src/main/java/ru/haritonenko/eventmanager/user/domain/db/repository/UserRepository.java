package ru.haritonenko.eventmanager.user.domain.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.haritonenko.eventmanager.user.domain.db.entity.UserEntity;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Integer> {

    @Transactional(readOnly = true)
    boolean existsByLogin(String Login);

    @Transactional(readOnly = true)
    Optional<UserEntity> findByLogin(String login);
}
