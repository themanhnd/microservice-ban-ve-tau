package com.xxxx.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Paginated API response wrapper extending ApiResponse.
 *
 * @param <T> the type of the response data (typically a List)
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> extends ApiResponse<T> {

    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public static <T> PageResponse<T> of(T data, int page, int size, long totalElements) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;

        PageResponse<T> r = new PageResponse<>();
        r.setSuccess(true);
        r.setCode("200");
        r.setMessage("Success");
        r.setData(data);
        r.setTimestamp(LocalDateTime.now());
        r.setPage(page);
        r.setSize(size);
        r.setTotalElements(totalElements);
        r.setTotalPages(totalPages);
        return r;
    }

    public static <T> PageResponse<T> of(T data, int page, int size, long totalElements, String traceId) {
        PageResponse<T> r = of(data, page, size, totalElements);
        r.setTraceId(traceId);
        return r;
    }
}
