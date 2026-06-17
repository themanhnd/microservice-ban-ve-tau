package com.xxxx.order.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request dùng khi admin quyết định bỏ qua một outbox record đã rơi vào DLQ nội bộ.
 *
 * <p>Record ở trạng thái {@code FAILED} nghĩa là worker đã retry nhiều lần nhưng vẫn không publish được Kafka.
 * Khi lỗi hạ tầng không cần replay nữa, admin gửi lý do vào request này để hệ thống lưu lại trong {@code lastError},
 * giúp người vận hành sau này biết vì sao record được resolve thủ công.</p>
 */
@Data
public class OutboxResolveRequest {

    @NotBlank(message = "Lý do bỏ qua outbox record không được để trống")
    @Size(max = 500, message = "Lý do bỏ qua outbox record tối đa 500 ký tự")
    private String reason;
}
