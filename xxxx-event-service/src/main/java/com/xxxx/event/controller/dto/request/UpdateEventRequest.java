package com.xxxx.event.controller.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventRequest {

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
}
