package ru.haritonenko.eventnotificator.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.haritonenko.commonlibs.dto.changes.EventFieldChange;
import ru.haritonenko.commonlibs.dto.notification.EventChangeKafkaMessage;
import ru.haritonenko.commonlibs.securirty.user.AuthUser;
import ru.haritonenko.eventnotificator.api.dto.filter.EventNotificationPageFilter;
import ru.haritonenko.eventnotificator.domain.EventNotification;
import ru.haritonenko.eventnotificator.domain.db.entity.EventNotificationEntity;
import ru.haritonenko.eventnotificator.domain.db.repository.EventNotificationRepository;
import ru.haritonenko.eventnotificator.domain.mapper.EventNotificationEntityMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventNotificationService {

    private final EventNotificationRepository notificationRepository;
    private final EventNotificationEntityMapper mapper;

    @Value("${app.location.default-page-size}")
    private int defaultPageSize;

    @Value("${app.location.default-page-number}")
    private int defaultPageNumber;

    @Cacheable(
            cacheNames = "unread-notifications",
            key = "#user.id() + ':' + (#pageFilter?.pageNumber() ?: 0) + ':' + (#pageFilter?.pageSize() ?: 5)"
    )
    @Transactional(readOnly = true)
    public List<EventNotification> findUnreadNotificationsForUser(
            AuthUser user,
            EventNotificationPageFilter pageFilter
    ) {
        log.info("Searching for unread user notification");
        if (isNull(user)) {
            throw new IllegalStateException("Authentication not present");
        }
        if (isNull(pageFilter)) {
            pageFilter = new EventNotificationPageFilter(null, null);
        }

        return notificationRepository.findAllUnredNotificationsByUserId(user.id(), getPageable(pageFilter))
                .stream()
                .map(mapper::toDomain)
                .sorted(Comparator.comparing(EventNotification::id))
                .collect(Collectors.toList());
    }

    @CacheEvict(cacheNames = "unread-notifications", allEntries = true)
    @Transactional
    public void markNotificationsAsRead(AuthUser user, List<Integer> notificationIds) {
        if (isNull(user)) {
            throw new IllegalStateException("Authentication not present");
        }
        if (isNull(notificationIds) || notificationIds.isEmpty()) {
            return;
        }
        notificationRepository.markUserNotificationsAsRead(user.id(), notificationIds);
    }

    @CacheEvict(cacheNames = "unread-notifications", allEntries = true)
    @Transactional
    public void saveNotificationsFromKafka(EventChangeKafkaMessage message) {
        if (isNull(message) || isNull(message.users()) || message.users().isEmpty()) {
            return;
        }

        String text = buildMessageText(message);

        List<EventNotificationEntity> entities = message.users().stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(userId -> EventNotificationEntity.builder()
                        .id(null)
                        .userId(userId)
                        .eventId(message.eventId())
                        .createdAt(LocalDateTime.now())
                        .read(false)
                        .message(text)
                        .build())
                .toList();

        notificationRepository.saveAll(entities);
    }

    private String buildMessageText(EventChangeKafkaMessage message) {
        List<String> parts = new ArrayList<>();

        addChange(parts, "name", message.name());
        addChange(parts, "maxPlaces", message.maxPlaces());
        addChange(parts, "time", message.time());
        addChange(parts, "cost", message.cost());
        addChange(parts, "duration", message.duration());
        addChange(parts, "status", message.status());
        addChange(parts, "locationId", message.locationId());

        String title = "Event " + message.eventId() + " changed";
        if (parts.isEmpty()) {
            return title;
        }
        return title + ": " + String.join(", ", parts);
    }

    private <T> void addChange(List<String> parts, String field, EventFieldChange<T> change) {
        if (isNull(change)) {
            return;
        }
        if (nonNull(change.oldField()) || nonNull(change.newField())) {
            parts.add(field + " [" + change.oldField() + " -> " + change.newField() + "]");
        }
    }

    private Pageable getPageable(EventNotificationPageFilter pageFilter) {
        int pageSize = Objects.nonNull(pageFilter.pageSize()) ? pageFilter.pageSize() : defaultPageSize;
        int pageNumber = Objects.nonNull(pageFilter.pageNumber()) ? pageFilter.pageNumber() : defaultPageNumber;
        return Pageable.ofSize(pageSize).withPage(pageNumber);
    }
}
