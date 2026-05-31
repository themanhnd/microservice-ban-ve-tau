package com.xxxx.event.scheduler;

import com.xxxx.event.repository.EventRepository;
import com.xxxx.event.repository.entity.EventEntity;
import com.xxxx.event.repository.entity.EventStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Scheduler to warmup Redis cache for upcoming events.
 * Runs daily at 6 AM to proactively load events for the next 7 days into cache.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WarmupDataScheduler {

    private final EventRepository eventRepository;
    private final CacheManager cacheManager;

    @Scheduled(cron = "0 0 6 * * *")
    public void warmupUpcomingEvents() {
        log.info("Starting cache warmup for upcoming events...");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysLater = now.plusDays(7);

        List<EventEntity> upcomingEvents = eventRepository
                .findByStartDateBetweenAndDeletedFalse(now, sevenDaysLater);

        var eventsCache = Objects.requireNonNull(cacheManager.getCache("events"));

        for (EventEntity event : upcomingEvents) {
            eventsCache.put(event.getId(), event);
            log.debug("Cached event: id={}, name={}, startDate={}",
                    event.getId(), event.getName(), event.getStartDate());
        }

        log.info("Cache warmup completed. Loaded {} upcoming events into cache.", upcomingEvents.size());
    }
}
