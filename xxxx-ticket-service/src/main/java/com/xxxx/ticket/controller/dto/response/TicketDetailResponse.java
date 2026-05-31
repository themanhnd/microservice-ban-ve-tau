package com.xxxx.ticket.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketDetailResponse {

    private Long id;
    private String name;
    private String description;
    private Integer stockInitial;
    private Integer stockAvailable;
    private Boolean isStockPrepared;
    private BigDecimal priceOriginal;
    private BigDecimal priceFlash;
    private LocalDateTime saleStartTime;
    private LocalDateTime saleEndTime;
    private Integer status;
    private Long ticketId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
