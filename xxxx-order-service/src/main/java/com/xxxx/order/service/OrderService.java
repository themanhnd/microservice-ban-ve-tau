package com.xxxx.order.service;

import com.xxxx.common.response.PageResponse;
import com.xxxx.order.controller.dto.request.PlaceOrderRequest;
import com.xxxx.order.controller.dto.response.OrderResponse;
import com.xxxx.order.controller.dto.response.OrderStatusResponse;

/**
 * Service interface định nghĩa các nghiệp vụ chính của Order Service.
 * Quản lý vòng đời đơn hàng bao gồm Saga orchestration.
 */
public interface OrderService {

    /**
     * Đặt hàng mới - khởi tạo Saga flow.
     * Tạo order với trạng thái PENDING, publish OrderPlaced event.
     *
     * @param request thông tin đặt hàng
     * @return thông tin đơn hàng đã tạo
     */
    OrderResponse placeOrder(PlaceOrderRequest request);

    /**
     * Lấy thông tin đơn hàng theo mã đơn hàng.
     *
     * @param orderNo mã đơn hàng
     * @return thông tin đơn hàng
     */
    OrderResponse getOrderByOrderNo(String orderNo);

    /**
     * Lấy trạng thái đơn hàng (lightweight).
     *
     * @param orderNo mã đơn hàng
     * @return trạng thái đơn hàng
     */
    OrderStatusResponse getOrderStatus(String orderNo);

    /**
     * Lấy thông tin checkout rõ ràng cho frontend polling/redirect.
     *
     * @param orderNo mã đơn hàng
     * @return trạng thái checkout
     */
    OrderStatusResponse getCheckoutInfo(String orderNo);

    /**
     * Lấy danh sách đơn hàng theo userId (phân trang).
     *
     * @param userId ID người dùng
     * @param page   số trang (0-based)
     * @param size   kích thước trang
     * @return danh sách đơn hàng phân trang
     */
    PageResponse<java.util.List<OrderResponse>> getOrdersByUserId(String userId, int page, int size);

    /**
     * Hủy đơn hàng - trigger compensation nếu cần.
     *
     * @param orderNo mã đơn hàng cần hủy
     */
    void cancelOrder(String orderNo);
}
