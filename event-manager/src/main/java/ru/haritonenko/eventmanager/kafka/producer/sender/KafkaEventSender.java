package ru.haritonenko.eventmanager.kafka.producer.sender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.haritonenko.commonlibs.dto.notification.EventChangeKafkaMessage;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaEventSender {

    private final KafkaTemplate<Integer, EventChangeKafkaMessage> kafkaTemplate;

    @Value("${kafka-topic}")
    private String eventTopic;

    public void sendKafkaEvent(EventChangeKafkaMessage kafkaEventNotification) {
        kafkaTemplate.send(eventTopic, kafkaEventNotification.eventId(), kafkaEventNotification)
                .thenAccept(r -> log.info(
                        "Event notification was successfully sent. eventId={}", kafkaEventNotification.eventId()))
                .exceptionally(ex -> {
                    log.error("Failed to send event notification. eventId={}", kafkaEventNotification.eventId(), ex);
                    return null;
                });
    }
}
