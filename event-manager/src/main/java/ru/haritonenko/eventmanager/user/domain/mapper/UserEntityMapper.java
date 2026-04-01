package ru.haritonenko.eventmanager.user.domain.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.haritonenko.eventmanager.user.api.dto.registration.UserRegistration;
import ru.haritonenko.eventmanager.user.domain.User;
import ru.haritonenko.eventmanager.user.domain.db.entity.UserEntity;
import ru.haritonenko.eventmanager.user.domain.role.UserRole;

import java.util.ArrayList;

@Mapper(componentModel = "spring", imports = {UserRole.class, ArrayList.class})
public interface UserEntityMapper {

    @Mapping(target = "role", source = "userRole")
    User toDomain(UserEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "password", source = "hashedPassword")
    @Mapping(target = "userRole", expression = "java(UserRole.USER)")
    @Mapping(target = "ownEvents", expression = "java(new ArrayList<>())")
    @Mapping(target = "registrations", expression = "java(new ArrayList<>())")
    UserEntity toEntity(UserRegistration dto, String hashedPassword);
}
