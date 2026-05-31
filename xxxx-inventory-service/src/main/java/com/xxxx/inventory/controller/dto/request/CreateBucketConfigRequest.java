package com.xxxx.inventory.controller.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO cho việc tạo cấu hình phân mảnh tồn kho (Bucket Configuration).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBucketConfigRequest {

    @NotBlank(message = "templateName is required")
    private String templateName;

    @NotNull(message = "bucketNum is required")
    @Min(value = 1, message = "bucketNum must be at least 1")
    private Integer bucketNum;

    @NotNull(message = "maxDepthNum is required")
    @Min(value = 1, message = "maxDepthNum must be at least 1")
    private Integer maxDepthNum;

    @NotNull(message = "minDepthNum is required")
    @Min(value = 0, message = "minDepthNum must be at least 0")
    private Integer minDepthNum;

    @NotNull(message = "thresholdValue is required")
    @Min(value = 0, message = "thresholdValue must be at least 0")
    private Integer thresholdValue;

    @NotNull(message = "backSourceProportion is required")
    @Min(value = 1, message = "backSourceProportion must be at least 1")
    @Max(value = 100, message = "backSourceProportion must be at most 100")
    private Integer backSourceProportion;

    @NotNull(message = "backSourceStep is required")
    @Min(value = 1, message = "backSourceStep must be at least 1")
    private Integer backSourceStep;
}
