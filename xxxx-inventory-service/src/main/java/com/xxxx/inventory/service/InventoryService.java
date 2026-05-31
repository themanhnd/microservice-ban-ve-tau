package com.xxxx.inventory.service;

import com.xxxx.inventory.controller.dto.request.CreateBucketConfigRequest;
import com.xxxx.inventory.controller.dto.request.ReleaseStockRequest;
import com.xxxx.inventory.controller.dto.request.ReserveStockRequest;
import com.xxxx.inventory.controller.dto.response.ReserveStockResponse;
import com.xxxx.inventory.controller.dto.response.StockLevelResponse;
import com.xxxx.inventory.repository.entity.InventoryBucketConfigEntity;

import java.util.List;

/**
 * Service interface cho các thao tác quản lý tồn kho.
 */
public interface InventoryService {

    /**
     * Lấy thông tin mức tồn kho hiện tại cho một ticket detail.
     *
     * @param ticketDetailId ID của ticket detail cần kiểm tra
     * @return thông tin mức tồn kho
     */
    StockLevelResponse getStockLevel(Long ticketDetailId);

    /**
     * Đặt trước (reserve) tồn kho cho một đơn hàng.
     * Sử dụng optimistic locking hoặc distributed lock để đảm bảo concurrency control.
     *
     * @param request thông tin đặt trước tồn kho
     * @return kết quả đặt trước
     */
    ReserveStockResponse reserveStock(ReserveStockRequest request);

    /**
     * Giải phóng (release) tồn kho đã đặt trước - compensation action khi đơn hàng bị hủy.
     *
     * @param request thông tin giải phóng tồn kho
     */
    void releaseStock(ReleaseStockRequest request);

    /**
     * Lấy danh sách tất cả cấu hình phân mảnh tồn kho.
     *
     * @return danh sách bucket configs
     */
    List<InventoryBucketConfigEntity> getAllBucketConfigs();

    /**
     * Tạo mới một cấu hình phân mảnh tồn kho.
     *
     * @param request thông tin cấu hình mới
     * @return entity đã được lưu
     */
    InventoryBucketConfigEntity createBucketConfig(CreateBucketConfigRequest request);
}
