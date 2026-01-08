package ru.haritonenko.eventnotificator.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.haritonenko.commonlibs.securirty.user.AuthUser;
import ru.haritonenko.eventnotificator.api.dto.EventNotificationDto;
import ru.haritonenko.eventnotificator.api.dto.MarkNotificationsReadRequest;
import ru.haritonenko.eventnotificator.api.dto.filter.EventNotificationPageFilter;
import ru.haritonenko.eventnotificator.domain.converter.EventNotificationDtoConverter;
import ru.haritonenko.eventnotificator.domain.service.EventNotificationService;
import ru.haritonenko.eventnotificator.security.service.AuthUserService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class EventNotificationController {

    private final EventNotificationService eventNotificationService;
    private final AuthUserService authUserService;
    private final EventNotificationDtoConverter converter;

    @GetMapping
    public List<EventNotificationDto> getUnreadNotificationsForUser(
            @Valid EventNotificationPageFilter pageFilter
    ) {
        log.info("Get request for getting unread notifications for user");
        return eventNotificationService.findUnreadNotificationsForUser(getAuthenticatedUser(), pageFilter)
                .stream()
                .map(converter::toDto)
                .toList();
    }
    @PostMapping
    public void markNotificationsRead(@RequestBody MarkNotificationsReadRequest request) {
        log.info("Post request for marking unread notifications for user");
        eventNotificationService.markNotificationsAsRead(
                getAuthenticatedUser(),
                request.notificationIds()
        );
    }


    private AuthUser getAuthenticatedUser() {
        return authUserService.getCurrentAuthenticatedUser();
    }


}

