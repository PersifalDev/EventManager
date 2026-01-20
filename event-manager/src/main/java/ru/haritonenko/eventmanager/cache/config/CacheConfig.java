package ru.haritonenko.eventmanager.cache.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
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
    @Value("${app.cache.registrations-ttl:30s}")
    private Duration registrationsTtl;
    @Value("${app.cache.users-ttl:30s}")
    private Duration usersTtl;
    @Value("${app.cache.user-booked-events-ttl:30s}")
    private Duration userBookedEventsTtl;
    @Value("${app.cache.user-created-events-ttl:30s}")
    private Duration userCreatedEventsTtl;

    @Bean
    public GenericJackson2JsonRedisSerializer redisValueSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();

        var ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("ru.haritonenko.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.time.")
                .allowIfSubType("java.math.")
                .build();

        mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.EVERYTHING, JsonTypeInfo.As.PROPERTY);
        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            GenericJackson2JsonRedisSerializer redisValueSerializer
    ) {
        var keySerializer = new StringRedisSerializer();

        RedisCacheConfiguration baseConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(defaultTtl)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisValueSerializer));

        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        perCache.put("events", baseConfig.entryTtl(eventsTtl));
        perCache.put("locations", baseConfig.entryTtl(locationsTtl));
        perCache.put("registrations", baseConfig.entryTtl(registrationsTtl));
        perCache.put("users", baseConfig.entryTtl(usersTtl));
        perCache.put("user-created-events", baseConfig.entryTtl(userCreatedEventsTtl));
        perCache.put("user-booked-events", baseConfig.entryTtl(userBookedEventsTtl));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(baseConfig)
                .withInitialCacheConfigurations(perCache)
                .transactionAware()
                .build();
    }
}
