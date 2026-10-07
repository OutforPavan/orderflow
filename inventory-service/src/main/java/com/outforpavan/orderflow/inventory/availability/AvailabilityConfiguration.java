package com.outforpavan.orderflow.inventory.availability;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import java.time.Clock;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AvailabilityConfiguration {
    @Bean
    @ConditionalOnMissingBean
    Ticker availabilityTicker() {
        return Ticker.systemTicker();
    }

    @Bean
    @ConditionalOnMissingBean
    Clock availabilityClock() {
        return Clock.systemUTC();
    }

    @Bean
    Cache<Long, AvailabilitySnapshot> availabilityCache(Ticker ticker) {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofSeconds(5))
                .ticker(ticker)
                .recordStats()
                .build();
    }
}
