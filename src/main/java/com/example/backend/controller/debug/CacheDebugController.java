package com.example.backend.controller.debug;

import com.example.backend.service.student.StudentEvictCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class CacheDebugController {

    private final CacheManager cacheManager;
    private final RedisConnectionFactory redisConnectionFactory;

    @GetMapping("/debug/cache")
    public Map<String, Object> cacheDebug() {

        Map<String, Object> result = new LinkedHashMap<>();

        result.put(
                "cacheManager",
                cacheManager.getClass().getName()
        );

        result.put(
                "cacheNames",
                cacheManager.getCacheNames()
        );

        try (
                RedisConnection connection =
                        redisConnectionFactory.getConnection()
        ) {

            result.put(
                    "redisConnectionFactory",
                    redisConnectionFactory.getClass().getName()
            );

            result.put(
                    "redisPing",
                    connection.ping()
            );

            result.put(
                    "dbSize",
                    connection.serverCommands().dbSize()
            );

        } catch (Exception e) {

            result.put(
                    "redisError",
                    e.getMessage()
            );
        }

        return result;
    }

    @GetMapping("/debug/cache/evict/{studentId}")
    public String evictStudent(
            @PathVariable String studentId,
            StudentEvictCacheService service
    ) {
        service.evictStudent(studentId);
        return "evicted";
    }
}