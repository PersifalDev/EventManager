package ru.haritonenko.eventmanager.user.domain.mapper;

import org.mapstruct.Mapper;
import ru.haritonenko.eventmanager.user.api.dto.UserDto;
import ru.haritonenko.eventmanager.user.domain.User;

@Mapper(componentModel = "spring")
public interface UserDtoMapper {

    UserDto toDto(User user);
}