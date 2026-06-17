package com.xxxx.inventory.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho kết quả giữ tồn kho.
 *
 * <p>inventory-service dùng response này để cho biết reserve thành công hay thất bại và còn bao nhiêu vé khả dụng.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReserveStockResponse {

    /** true nếu đã giữ được đủ số lượng vé yêu cầu. */
    private boolean success;
    /** Mã order đã yêu cầu reserve. */
    private String orderId;
    /** ID chi tiết vé được reserve. */
    private Long ticketDetailId;
    /** Số lượng thực tế đã giữ. */
    private Integer reservedQuantity;
    /** Số tồn khả dụng còn lại sau khi xử lý. */
    private Integer remainingStock;
}
