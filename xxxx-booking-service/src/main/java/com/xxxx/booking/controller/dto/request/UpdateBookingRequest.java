package com.xxxx.booking.controller.dto.request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBookingRequest {

    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;

    private String notes;
}
