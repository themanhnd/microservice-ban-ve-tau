package com.xxxx.ticket.repository;

import com.xxxx.ticket.repository.entity.TicketDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketDetailRepository extends JpaRepository<TicketDetailEntity, Long> {

    /**
     * Tìm tất cả chi tiết vé của một ticket có trạng thái khác giá trị truyền vào.
     * Thường dùng để loại bỏ chi tiết vé đã xóa mềm, ví dụ status = 2.
     */
    List<TicketDetailEntity> findByTicketIdAndStatusNot(Long ticketId, Integer status);
}
