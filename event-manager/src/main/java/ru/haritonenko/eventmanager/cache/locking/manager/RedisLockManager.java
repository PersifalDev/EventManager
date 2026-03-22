package ru.haritonenko.eventmanager.cache.locking.manager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static java.util.Objects.nonNull;

@Profile("dev")
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisLockManager {

    private final StringRedisTemplate redisTemplate;

    private static final String RELEASE_LOCK_LUA_SCRIPT = """
            if redis.call('GET', KEYS[1]) == ARGV[1] then
               return redis.call('DEL', KEYS[1])
            else return 0 end
            """;

    public String tryLock(String key, Duration ttl) {
        String lockKey = "lock:" + key;
        String lockId = UUID.randomUUID().toString();

        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, lockId, ttl);
        if (Boolean.TRUE.equals(locked)) {
            log.info("Lock has been acquired: lockKey={}, lockId={}", lockKey, lockId);
            return lockId;
        }
        return null;
    }

    public void unlockLock(String key, String lockId) {
        String lockKey = "lock:" + key;

        Long result = redisTemplate.execute(connection -> connection.scriptingCommands().eval(
                RELEASE_LOCK_LUA_SCRIPT.getBytes(StandardCharsets.UTF_8),
                ReturnType.INTEGER,
                1,
                lockKey.getBytes(StandardCharsets.UTF_8),
                lockId.getBytes(StandardCharsets.UTF_8)
        ), true);

        if (nonNull(result) && result == 1) {
            log.info("Lock has been released: lockKey={}, lockId={}", lockKey, lockId);
        } else {
            log.info("Lock was already released or re-acquired: lockKey={}, lockId={}", lockKey, lockId);
        }
    }
}
