package com.xxxx.inventory.lock;

import com.xxxx.common.exception.BusinessException;

/**
 * Ném ra khi không giành được khóa phân tán (hệ thống đang bận xử lý cùng tài nguyên).
 * Kế thừa BusinessException nên trả về HTTP 400 với mã lỗi rõ ràng cho client biết để thử lại.
 */
public class LockAcquisitionException extends BusinessException {

    public static final String ERROR_CODE = "SYSTEM_BUSY";

    public LockAcquisitionException(String lockKey) {
        super(ERROR_CODE, "System busy, please retry. Resource is locked: " + lockKey);
    }
}
