package ru.haritonenko.eventnotificator.kafka.consumer.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.haritonenko.commonlibs.dto.notification.EventChangeKafkaMessage;
import ru.haritonenko.eventnotificator.domain.service.EventNotificationService;

import static java.util.Objects.isNull;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventKafkaListener {

    private final EventNotificationService notificationService;

    @KafkaListener(
            topics = "${kafka-topic}",
            containerFactory = "containerFactory"
    )
    public void listenEvents(ConsumerRecord<Integer, EventChangeKafkaMessage> record) {
        var message = record.value();
        if (isNull(message)) {
            log.warn("Skip null kafka message. key={}", record.key());
            return;
        }
        notificationService.saveNotificationsFromKafka(message);
    }
}
