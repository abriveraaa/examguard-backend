package com.example.backend.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        ObjectMapper mapper = new ObjectMapper();

        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        RedisSerializer<Object> serializer = new JdkSerializationRedisSerializer();

        RedisCacheConfiguration defaultConfig =
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(10))
                        .disableCachingNullValues()
                        .computePrefixWith(cacheName -> "examguard:v3:" + cacheName + "::")
                        .serializeKeysWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(
                                        new StringRedisSerializer()
                                )
                        )
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(serializer)
                        );

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        // =============
        // STUDENTS
        // =============

        cacheConfigs.put("studentDashboardProfile", defaultConfig.entryTtl(Duration.ofDays(150)));
        cacheConfigs.put("studentDashboardUpcomingExams", defaultConfig.entryTtl(Duration.ofDays(7)));
        cacheConfigs.put("studentDashboardResults", defaultConfig.entryTtl(Duration.ofDays(7)));
        cacheConfigs.put("studentDashboardViolations", defaultConfig.entryTtl(Duration.ofDays(7)));

        // =============
        // EXAMS
        // =============
        cacheConfigs.put("studentExamsRaw", defaultConfig.entryTtl(Duration.ofDays(7)));
        cacheConfigs.put("examTakingRawContent", defaultConfig.entryTtl(Duration.ofDays(7)));


        // =============
        // FACULTY
        // =============

        cacheConfigs.put("allStudents", defaultConfig.entryTtl(Duration.ofDays(10)));
        cacheConfigs.put("allFaculty", defaultConfig.entryTtl(Duration.ofDays(10)));
        cacheConfigs.put("studentById", defaultConfig.entryTtl(Duration.ofDays(10)));
        cacheConfigs.put("facultyById", defaultConfig.entryTtl(Duration.ofDays(10)));
        cacheConfigs.put("studentDashboard", defaultConfig.entryTtl(Duration.ofDays(10)));
        cacheConfigs.put("studentExams", defaultConfig.entryTtl(Duration.ofDays(10)));
        cacheConfigs.put("profileMe", defaultConfig.entryTtl(Duration.ofDays(1)));
        cacheConfigs.put("facultyProfile", defaultConfig.entryTtl(Duration.ofDays(12)));
        cacheConfigs.put("facultyDashboard", defaultConfig.entryTtl(Duration.ofDays(10)));
        cacheConfigs.put("adminMonitoringSummary", defaultConfig.entryTtl(Duration.ofSeconds(30)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware()
                .build();
    }
}