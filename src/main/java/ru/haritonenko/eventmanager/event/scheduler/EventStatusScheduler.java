package ru.haritonenko.eventmanager.event.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.haritonenko.eventmanager.event.domain.status.EventStatus;
import ru.haritonenko.eventmanager.event.domain.db.entity.EventEntity;
import ru.haritonenko.eventmanager.event.domain.db.repository.EventRepository;
import ru.haritonenko.eventmanager.event.registration.db.repository.EventRegistrationRepository;
import ru.haritonenko.eventmanager.event.registration.status.EventRegistrationStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventStatusScheduler {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final EventRepository eventRepository;
    private final EventRegistrationRepository eventRegistrationRepository;

    @Transactional
    @Scheduled(fixedDelayString = "${scheduler.event-status.fixed-delay-ms}")
    public void updateEventStatuses() {
        var now = LocalDateTime.now();

        var events = eventRepository.findByStatusIn(
                List.of(EventStatus.WAIT_START, EventStatus.STARTED)
        );

        int startedEventsCounter = 0;
        int finishedEventsCounter = 0;
        int finishedRegistrationsEventsCounter = 0;
        int skippedEventBadDateCounter = 0;

        List<Integer> finishedEventsIds = new ArrayList<>();

        log.info("Parsing date");
        for (EventEntity event : events) {
            LocalDateTime start;
            try {
                start = LocalDateTime.parse(event.getDate(), FORMATTER);
            } catch (DateTimeParseException ex) {
                skippedEventBadDateCounter++;
                log.warn("Skip event id={} due to invalid date='{}'", event.getId(), event.getDate());
                continue;
            }

            var duration = isNull(event.getDuration()) ? 0 : event.getDuration();
            var end = start.plusMinutes(duration);

            if (event.getStatus() == EventStatus.WAIT_START) {
                if (!now.isBefore(end) && duration > 0) {
                    event.setStatus(EventStatus.FINISHED);
                    finishedEventsCounter++;
                    finishedEventsIds.add(event.getId());
                    continue;
                }
                if (!now.isBefore(start) && now.isBefore(end)) {
                    event.setStatus(EventStatus.STARTED);
                    startedEventsCounter++;
                }
                continue;
            }

            if (event.getStatus() == EventStatus.STARTED) {
                if (!now.isBefore(end)) {
                    event.setStatus(EventStatus.FINISHED);
                    finishedEventsCounter++;
                    finishedEventsIds.add(event.getId());
                }
            }
        }

        for (Integer eventId : finishedEventsIds) {
            finishedRegistrationsEventsCounter += eventRegistrationRepository.updateStatusByEventId(
                    eventId,
                    EventRegistrationStatus.FINISHED,
                    EventRegistrationStatus.ACTIVE
            );
            eventRepository.resetOccupiedPlaces(eventId);
        }

        if (startedEventsCounter > 0 || finishedEventsCounter > 0 || skippedEventBadDateCounter > 0 || finishedRegistrationsEventsCounter > 0) {
            log.info("Scheduler updated events: STARTED={}, FINISHED={}, finishedRegistrationsEventsCounter={}, skippedBadDate={}, checked={}",
                    startedEventsCounter, finishedEventsCounter, finishedRegistrationsEventsCounter, skippedEventBadDateCounter, events.size());
        }
    }
}
