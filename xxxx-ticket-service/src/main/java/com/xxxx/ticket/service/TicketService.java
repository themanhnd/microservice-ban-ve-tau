package com.xxxx.ticket.service;

import com.xxxx.ticket.controller.dto.request.CreateTicketRequest;
import com.xxxx.ticket.controller.dto.request.UpdateTicketRequest;
import com.xxxx.ticket.controller.dto.response.TicketResponse;

import java.util.List;

/**
 * Service interface for Ticket operations.
 */
public interface TicketService {

    List<TicketResponse> getAllTickets();

    TicketResponse getTicketById(Long id);

    TicketResponse createTicket(CreateTicketRequest request);

    TicketResponse updateTicket(Long id, UpdateTicketRequest request);

    void deleteTicket(Long id);
}
