package ru.haritonenko.eventmanager.kafka.producer.configuration;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import ru.haritonenko.commonlibs.dto.notification.EventChangeKafkaMessage;

import java.util.Map;

@Configuration
public class KafkaProducerConfiguration {

    @Bean
    public DefaultKafkaProducerFactory<Long, EventChangeKafkaMessage> producerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = kafkaProperties.buildProducerProperties();
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<Long, EventChangeKafkaMessage> kafkaTemplate(DefaultKafkaProducerFactory<Long, EventChangeKafkaMessage> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
