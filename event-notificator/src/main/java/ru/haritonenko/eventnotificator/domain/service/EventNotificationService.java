package ru.haritonenko.eventnotificator.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
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

import java.time.Duration;
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

    private static final String CACHE_KEY_PREFIX = "unread-notifications:";

    @Value("${app.cache.unread-notifications-ttl:30s}")
    private Duration cacheTtl;

    private final EventNotificationRepository notificationRepository;
    private final EventNotificationEntityMapper mapper;
    private final RedisTemplate<String, EventNotificationEntity> redisTemplate;

    @Value("${app.location.default-page-size}")
    private int defaultPageSize;

    @Value("${app.location.default-page-number}")
    private int defaultPageNumber;

    @Transactional(readOnly = true)
    public List<EventNotification> findUnreadNotificationsForUser(
            AuthUser user,
            EventNotificationPageFilter pageFilter
    ) {
        log.info("Searching for unread user notification");

        if (isNull(user)) {
            log.warn("Error while authentication");
            throw new IllegalStateException("Authentication not present");
        }
        if (isNull(pageFilter)) {
            pageFilter = new EventNotificationPageFilter(null, null);
        }

        Pageable pageable = getPageable(pageFilter);
        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();

        String cacheKey = CACHE_KEY_PREFIX + user.id();

        long start = (long) pageNumber * pageSize;
        long end = start + pageSize - 1;

        List<EventNotificationEntity> cachedNotificationList = redisTemplate.opsForList().range(cacheKey, start, end);

        if (nonNull(cachedNotificationList) && !cachedNotificationList.isEmpty()) {
            log.info("Unread notification list was found in cache for user with id: {}", user.id());
            return cachedNotificationList.stream()
                    .map(mapper::toDomain)
                    .sorted(Comparator.comparing(EventNotification::id))
                    .collect(Collectors.toList());
        }

        Long cachedSize = redisTemplate.opsForList().size(cacheKey);
        if (nonNull(cachedSize) && cachedSize > 0) {
            log.info("Unread notification list exists in cache but requested page is empty for user with id: {}", user.id());
            return List.of();
        }

        log.info("Unread notification list not found in cache for user with id: {}", user.id());

        List<EventNotificationEntity> notificationListFromDb = notificationRepository.findAllUnredNotificationsByUserId(user.id());
        if (nonNull(notificationListFromDb) && !notificationListFromDb.isEmpty()) {
            notificationListFromDb = notificationListFromDb
                    .stream()
                    .sorted(Comparator.comparing(EventNotificationEntity::getId))
                    .toList();

            redisTemplate.delete(cacheKey);
            redisTemplate.opsForList().rightPushAll(cacheKey, notificationListFromDb);
            redisTemplate.expire(cacheKey, cacheTtl);

            List<EventNotificationEntity> notificationListFromDbPage = notificationListFromDb.subList(
                    (int) Math.min(start, notificationListFromDb.size()),
                    (int) Math.min(end + 1, notificationListFromDb.size())
            );

            log.info("Unread notification list cached for user with id: {}", user.id());

            return notificationListFromDbPage.stream()
                    .map(mapper::toDomain)
                    .collect(Collectors.toList());
        }

        return List.of();
    }

    @Transactional
    public void markNotificationsAsRead(AuthUser user, List<Long> notificationIds) {
        if (isNull(user)) {
            log.warn("Error while user authentication");
            throw new IllegalStateException("Authentication not present");
        }
        if (isNull(notificationIds) || notificationIds.isEmpty()) {
            return;
        }

        notificationRepository.markUserNotificationsAsRead(user.id(), notificationIds);

        String cacheKey = CACHE_KEY_PREFIX + user.id();
        redisTemplate.delete(cacheKey);
        log.info("Cache invalidated for marked notifications as read for user with id:{}", user.id());
    }

    @Transactional
    public void saveNotificationsFromKafka(EventChangeKafkaMessage message) {
        if (isNull(message) || isNull(message.users()) || message.users().isEmpty()) {
            return;
        }

        String text = buildMessageText(message);

        List<Long> userIds = message.users().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<EventNotificationEntity> notificationEntities = userIds.stream()
                .map(userId -> EventNotificationEntity.builder()
                        .id(null)
                        .userId(userId)
                        .eventId(message.eventId())
                        .createdAt(LocalDateTime.now())
                        .read(false)
                        .message(text)
                        .build())
                .toList();

        notificationRepository.saveAll(notificationEntities);

        for (Long userId : userIds) {
            String cacheKey = CACHE_KEY_PREFIX + userId;
            redisTemplate.delete(cacheKey);
            log.info("Notifications cache invalidated for user with id:{}", userId);
        }
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
