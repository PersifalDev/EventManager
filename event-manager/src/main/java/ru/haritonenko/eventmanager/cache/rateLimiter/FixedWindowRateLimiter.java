package ru.haritonenko.eventmanager.cache.rateLimiter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
public class FixedWindowRateLimiter {

    private final StringRedisTemplate stringRedisTemplate;

    public boolean allowRequest(
            String clientId,
            int limit,
            Duration windowSize
    ){
        long windowIndex = System.currentTimeMillis() / windowSize.toMillis();
        String key = String.format("rate:%s%s",clientId,windowIndex);

        Long countHits = stringRedisTemplate.opsForValue()
                .increment(key);
        if(nonNull(countHits) && countHits == 1L){
            stringRedisTemplate.expire(key,windowSize);
        }

        return nonNull(countHits) && countHits <= limit;
    }
}
