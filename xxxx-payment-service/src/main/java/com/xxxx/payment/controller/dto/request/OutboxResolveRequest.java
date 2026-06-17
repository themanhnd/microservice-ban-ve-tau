package com.xxxx.payment.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request ghi lý do khi admin bỏ qua một payment outbox record đã fail quá số lần retry.
 *
 * <p>Lý do này được lưu vào {@code lastError} để sau này audit biết record được resolve thủ công vì nguyên nhân gì.</p>
 */
@Data
public class OutboxResolveRequest {

    @NotBlank(message = "Lý do bỏ qua outbox record không được để trống")
    @Size(max = 500, message = "Lý do bỏ qua outbox record tối đa 500 ký tự")
    private String reason;
}
