package com.xxxx.payment.service;

import com.xxxx.payment.controller.dto.request.InitiatePaymentRequest;
import com.xxxx.payment.controller.dto.request.VnPayCallbackRequest;
import com.xxxx.payment.controller.dto.response.PaymentInitiateResponse;
import com.xxxx.payment.controller.dto.response.PaymentStatusResponse;

/**
 * Service interface định nghĩa các nghiệp vụ thanh toán như tạo giao dịch, xử lý callback và xem trạng thái.
 */
public interface PaymentService {

    /**
     * Khởi tạo một giao dịch thanh toán mới.
     * Tạo transaction trạng thái PENDING và sinh URL thanh toán VnPay cho client.
     *
     * @param request thông tin cần thiết để khởi tạo thanh toán
     * @param clientIp địa chỉ IP của client, gửi sang VnPay qua tham số vnp_IpAddr
     * @return phản hồi gồm mã giao dịch, URL thanh toán và trạng thái hiện tại
     */
    PaymentInitiateResponse initiatePayment(InitiatePaymentRequest request, String clientIp);

    /**
     * Xử lý callback IPN (Instant Payment Notification) do VnPay gọi về server.
     * Xác thực chữ ký, cập nhật trạng thái giao dịch và phát event cho Order Service.
     *
     * @param request các tham số callback nhận từ VnPay
     * @return acknowledgment result
     */
    String handleVnPayCallback(VnPayCallbackRequest request);

    /**
     * Xử lý luồng redirect return URL sau khi người dùng thanh toán trên VnPay.
     * Xác thực tham số và trả về trạng thái thanh toán hiện tại.
     *
     * @param txnRef mã tham chiếu giao dịch do hệ thống gửi sang VnPay
     * @param responseCode mã kết quả thanh toán do VnPay trả về
     * @param transactionNo mã giao dịch phía VnPay
     * @param secureHash chữ ký bảo mật dùng để kiểm tra dữ liệu return
     * @return thông tin trạng thái thanh toán hiện tại
     */
    PaymentStatusResponse handleVnPayReturn(String txnRef, String responseCode, String transactionNo, String secureHash);

    /**
     * Lấy trạng thái thanh toán theo transaction ID nội bộ.
     *
     * @param transactionId mã định danh duy nhất của giao dịch thanh toán
     * @return thông tin trạng thái thanh toán hiện tại
     */
    PaymentStatusResponse getPaymentStatus(String transactionId);
}
