package com.bluewave.apexbank.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    // Cache Name Constants
    public static final String CACHE_ACCOUNT_DETAILS = "accountDetails";
    public static final String CACHE_TRANSACTION_HISTORY = "transactionHistory";
    public static final String CACHE_TRANSACTION_LIMIT = "transactionLimit";

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        // 1. Create a PolymorphicTypeValidator for type safety in Jackson 3
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();

        // 2. Build the serializer using Spring Data Redis's built-in builder
        GenericJacksonJsonRedisSerializer serializer = GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(ptv)
                .build();

        // 3. Configure Base Redis Cache Settings
        RedisCacheConfiguration baseConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)) // Default fallback TTL
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        // 4. Custom TTL Configuration per Cache Region
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Account profile & status details cached for 1 Hour
        cacheConfigurations.put(CACHE_ACCOUNT_DETAILS, baseConfig.entryTtl(Duration.ofHours(1)));

        // Transaction list statement cached for 15 Minutes
        cacheConfigurations.put(CACHE_TRANSACTION_HISTORY, baseConfig.entryTtl(Duration.ofMinutes(15)));

        cacheConfigurations.put(CACHE_TRANSACTION_LIMIT, baseConfig.entryTtl(Duration.ofMinutes(30))); // 30-min TTL

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(baseConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}