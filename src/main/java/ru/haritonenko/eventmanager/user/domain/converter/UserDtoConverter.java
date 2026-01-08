package ru.haritonenko.eventmanager.user.domain.converter;

import org.springframework.stereotype.Component;
import ru.haritonenko.eventmanager.user.domain.User;
import ru.haritonenko.eventmanager.user.api.dto.UserDto;


@Component
public class UserDtoConverter {

    public UserDto toDto(User user) {
        return new UserDto(
                user.id(),
                user.login()
        );
    }

}
