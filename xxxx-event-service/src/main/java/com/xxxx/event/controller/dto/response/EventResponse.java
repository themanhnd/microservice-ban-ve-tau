package com.xxxx.event.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {

    private Long id;
    private String name;
    private String description;
    private String venue;
    private String address;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;
    private Integer capacity;
    private String organizer;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
