package com.xxxx.inventory.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request lưu lý do khi admin bỏ qua một inventory outbox record đã fail.
 *
 * <p>Inventory event liên quan trực tiếp tới giữ/hoàn vé, nên thao tác ignore luôn cần lý do rõ ràng để audit sau này.</p>
 */
@Data
public class OutboxResolveRequest {

    @NotBlank(message = "Lý do bỏ qua outbox record không được để trống")
    @Size(max = 500, message = "Lý do bỏ qua outbox record tối đa 500 ký tự")
    private String reason;
}
