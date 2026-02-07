package ru.haritonenko.eventmanager.cache.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import ru.haritonenko.eventmanager.event.domain.Event;
import ru.haritonenko.eventmanager.location.domain.EventLocation;
import ru.haritonenko.eventmanager.user.domain.User;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EnableCaching
@Configuration
public class CacheConfig {

    @Value("${app.cache.default-ttl:30s}")
    private Duration defaultTtl;
    @Value("${app.cache.events-ttl:30s}")
    private Duration eventsTtl;
    @Value("${app.cache.locations-ttl:30s}")
    private Duration locationsTtl;
    @Value("${app.cache.users-ttl:30s}")
    private Duration usersTtl;
    @Value("${app.cache.user-booked-events-ttl:30s}")
    private Duration userBookedEventsTtl;
    @Value("${app.cache.user-created-events-ttl:30s}")
    private Duration userCreatedEventsTtl;

    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        return mapper;
    }

    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            ObjectMapper redisObjectMapper
    ) {
        var keySerializer = new StringRedisSerializer();

        RedisCacheConfiguration baseConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(defaultTtl)
                .computePrefixWith(cacheName -> "eventmanager:v1:" + cacheName + "::")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer));

        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();

        perCache.put("events",
                cacheConfig(baseConfig, eventsTtl, serializer(redisObjectMapper, Event.class)));

        perCache.put("locations",
                cacheConfig(baseConfig, locationsTtl, serializer(redisObjectMapper, EventLocation.class)));

        perCache.put("users",
                cacheConfig(baseConfig, usersTtl, serializer(redisObjectMapper, User.class)));

        perCache.put("user-created-events",
                cacheConfig(baseConfig, userCreatedEventsTtl,
                        listSerializer(redisObjectMapper, Event.class)));

        perCache.put("user-booked-events",
                cacheConfig(baseConfig, userBookedEventsTtl,
                        listSerializer(redisObjectMapper, Event.class)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(baseConfig)
                .withInitialCacheConfigurations(perCache)
                .transactionAware()
                .build();
    }

    private <T> Jackson2JsonRedisSerializer<T> serializer(
            ObjectMapper mapper,
            Class<T> type
    ) {
        return new Jackson2JsonRedisSerializer<>(mapper, type);
    }

    private <T> Jackson2JsonRedisSerializer<T> listSerializer(
            ObjectMapper mapper,
            Class<T> elementType
    ) {
        var javaType = mapper.getTypeFactory()
                .constructCollectionType(List.class, elementType);
        return new Jackson2JsonRedisSerializer<>(mapper, javaType);
    }

    private RedisCacheConfiguration cacheConfig(
            RedisCacheConfiguration base,
            Duration ttl,
            Jackson2JsonRedisSerializer<?> valueSerializer
    ) {
        return base.entryTtl(ttl)
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer)
                );
    }
}
