package com.xxxx.ticket.repository;

import com.xxxx.ticket.repository.entity.TicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<TicketEntity, Long> {

    /**
     * Tìm tất cả ticket có trạng thái khác giá trị truyền vào.
     * Thường dùng để loại bỏ ticket đã xóa mềm, ví dụ status = 2.
     */
    List<TicketEntity> findAllByStatusNot(Integer status);
}
