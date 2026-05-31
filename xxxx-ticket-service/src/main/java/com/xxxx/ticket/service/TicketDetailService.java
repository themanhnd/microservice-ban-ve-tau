package com.xxxx.ticket.service;

import com.xxxx.ticket.controller.dto.request.CreateTicketDetailRequest;
import com.xxxx.ticket.controller.dto.request.UpdateTicketDetailRequest;
import com.xxxx.ticket.controller.dto.response.TicketDetailResponse;

import java.util.List;

/**
 * Service interface for TicketDetail operations.
 */
public interface TicketDetailService {

    List<TicketDetailResponse> getDetailsByTicketId(Long ticketId);

    TicketDetailResponse getDetailById(Long id);

    TicketDetailResponse createDetail(CreateTicketDetailRequest request);

    TicketDetailResponse updateDetail(Long id, UpdateTicketDetailRequest request);

    void deleteDetail(Long id);
}
