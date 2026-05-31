package com.xxxx.event.service;

import com.xxxx.event.controller.dto.request.CreateEventRequest;
import com.xxxx.event.controller.dto.request.UpdateEventRequest;
import com.xxxx.event.controller.dto.response.EventResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {

    List<EventResponse> getAllEvents(String status, LocalDateTime startDate, LocalDateTime endDate);

    EventResponse getEventById(Long id);

    EventResponse createEvent(CreateEventRequest request);

    EventResponse updateEvent(Long id, UpdateEventRequest request);

    void deleteEvent(Long id);
}
