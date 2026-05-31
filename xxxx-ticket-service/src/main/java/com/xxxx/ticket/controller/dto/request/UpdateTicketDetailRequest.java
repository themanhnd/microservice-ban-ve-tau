package com.xxxx.ticket.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class UpdateTicketDetailRequest {

    @NotBlank(message = "Ticket detail name is required")
    private String name;

    private String description;

    @NotNull(message = "Stock initial is required")
    private Integer stockInitial;

    @NotNull(message = "Original price is required")
    private BigDecimal priceOriginal;

    private BigDecimal priceFlash;

    private LocalDateTime saleStartTime;

    private LocalDateTime saleEndTime;

    @NotNull(message = "Status is required")
    private Integer status;
}
