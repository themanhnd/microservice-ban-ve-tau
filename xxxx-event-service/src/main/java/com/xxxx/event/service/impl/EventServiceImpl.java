package com.xxxx.event.service.impl;

import com.xxxx.event.controller.dto.request.CreateEventRequest;
import com.xxxx.event.controller.dto.request.UpdateEventRequest;
import com.xxxx.event.controller.dto.response.EventResponse;
import com.xxxx.event.repository.EventRepository;
import com.xxxx.event.repository.entity.EventEntity;
import com.xxxx.event.repository.entity.EventStatus;
import com.xxxx.event.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "events", key = "'all:' + #status + ':' + #startDate + ':' + #endDate")
    public List<EventResponse> getAllEvents(String status, LocalDateTime startDate, LocalDateTime endDate) {
        List<EventEntity> events;

        if (status != null && startDate != null && endDate != null) {
            EventStatus eventStatus = EventStatus.valueOf(status.toUpperCase());
            events = eventRepository.findByStatusAndDeletedFalse(eventStatus).stream()
                    .filter(e -> !e.getStartDate().isBefore(startDate) && !e.getStartDate().isAfter(endDate))
                    .toList();
        } else if (status != null) {
            EventStatus eventStatus = EventStatus.valueOf(status.toUpperCase());
            events = eventRepository.findByStatusAndDeletedFalse(eventStatus);
        } else if (startDate != null && endDate != null) {
            events = eventRepository.findByStartDateBetweenAndDeletedFalse(startDate, endDate);
        } else {
            events = eventRepository.findByDeletedFalse();
        }

        return events.stream()
                .map(this::toEventResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "events", key = "#id")
    public EventResponse getEventById(Long id) {
        EventEntity event = eventRepository.findById(id)
                .filter(e -> !e.getDeleted())
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
        return toEventResponse(event);
    }

    @Override
    @Transactional
    @CacheEvict(value = "events", allEntries = true)
    public EventResponse createEvent(CreateEventRequest request) {
        EventStatus eventStatus = EventStatus.DRAFT;
        if (request.getStatus() != null) {
            eventStatus = EventStatus.valueOf(request.getStatus().toUpperCase());
        }

        EventEntity event = EventEntity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .venue(request.getVenue())
                .address(request.getAddress())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(eventStatus)
                .capacity(request.getCapacity())
                .organizer(request.getOrganizer())
                .imageUrl(request.getImageUrl())
                .build();

        EventEntity savedEvent = eventRepository.save(event);
        log.info("Event created successfully: id={}, name={}", savedEvent.getId(), savedEvent.getName());
        return toEventResponse(savedEvent);
    }

    @Override
    @Transactional
    @CacheEvict(value = "events", allEntries = true)
    public EventResponse updateEvent(Long id, UpdateEventRequest request) {
        EventEntity event = eventRepository.findById(id)
                .filter(e -> !e.getDeleted())
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));

        if (request.getName() != null) {
            event.setName(request.getName());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getVenue() != null) {
            event.setVenue(request.getVenue());
        }
        if (request.getAddress() != null) {
            event.setAddress(request.getAddress());
        }
        if (request.getStartDate() != null) {
            event.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            event.setEndDate(request.getEndDate());
        }
        if (request.getStatus() != null) {
            event.setStatus(EventStatus.valueOf(request.getStatus().toUpperCase()));
        }
        if (request.getCapacity() != null) {
            event.setCapacity(request.getCapacity());
        }
        if (request.getOrganizer() != null) {
            event.setOrganizer(request.getOrganizer());
        }
        if (request.getImageUrl() != null) {
            event.setImageUrl(request.getImageUrl());
        }

        EventEntity updatedEvent = eventRepository.save(event);
        log.info("Event updated successfully: id={}, name={}", updatedEvent.getId(), updatedEvent.getName());
        return toEventResponse(updatedEvent);
    }

    @Override
    @Transactional
    @CacheEvict(value = "events", allEntries = true)
    public void deleteEvent(Long id) {
        EventEntity event = eventRepository.findById(id)
                .filter(e -> !e.getDeleted())
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));

        event.setDeleted(true);
        eventRepository.save(event);
        log.info("Event soft-deleted successfully: id={}", id);
    }

    private EventResponse toEventResponse(EventEntity entity) {
        return EventResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .venue(entity.getVenue())
                .address(entity.getAddress())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .status(entity.getStatus().name())
                .capacity(entity.getCapacity())
                .organizer(entity.getOrganizer())
                .imageUrl(entity.getImageUrl())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
