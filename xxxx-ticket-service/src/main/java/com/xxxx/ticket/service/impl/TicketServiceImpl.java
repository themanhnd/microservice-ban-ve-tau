package com.xxxx.ticket.service.impl;

import com.xxxx.common.exception.ResourceNotFoundException;
import com.xxxx.ticket.controller.dto.request.CreateTicketRequest;
import com.xxxx.ticket.controller.dto.request.UpdateTicketRequest;
import com.xxxx.ticket.controller.dto.response.TicketDetailResponse;
import com.xxxx.ticket.controller.dto.response.TicketResponse;
import com.xxxx.ticket.repository.TicketRepository;
import com.xxxx.ticket.repository.entity.TicketDetailEntity;
import com.xxxx.ticket.repository.entity.TicketEntity;
import com.xxxx.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> getAllTickets() {
        log.info("Getting all active tickets");
        List<TicketEntity> tickets = ticketRepository.findAllByStatusNot(2);
        return tickets.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicketById(Long id) {
        log.info("Getting ticket by id: {}", id);
        TicketEntity ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
        return mapToResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request) {
        log.info("Creating ticket with name: {}", request.getName());

        TicketEntity ticket = TicketEntity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(1) // ACTIVE
                .build();

        TicketEntity savedTicket = ticketRepository.save(ticket);
        log.info("Created ticket with id: {}", savedTicket.getId());
        return mapToResponse(savedTicket);
    }

    @Override
    @Transactional
    public TicketResponse updateTicket(Long id, UpdateTicketRequest request) {
        log.info("Updating ticket with id: {}", id);

        TicketEntity ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));

        ticket.setName(request.getName());
        ticket.setDescription(request.getDescription());
        ticket.setStartTime(request.getStartTime());
        ticket.setEndTime(request.getEndTime());
        ticket.setStatus(request.getStatus());

        TicketEntity updatedTicket = ticketRepository.save(ticket);
        log.info("Updated ticket with id: {}", updatedTicket.getId());
        return mapToResponse(updatedTicket);
    }

    @Override
    @Transactional
    public void deleteTicket(Long id) {
        log.info("Soft deleting ticket with id: {}", id);

        TicketEntity ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));

        ticket.setStatus(2); // DELETED
        ticketRepository.save(ticket);
        log.info("Soft deleted ticket with id: {}", id);
    }

    // ========== Các hàm hỗ trợ nội bộ ==========

    private TicketResponse mapToResponse(TicketEntity entity) {
        List<TicketDetailResponse> detailResponses = Collections.emptyList();
        if (entity.getTicketDetails() != null && !entity.getTicketDetails().isEmpty()) {
            detailResponses = entity.getTicketDetails().stream()
                    .filter(detail -> detail.getStatus() != 2) // Exclude deleted details
                    .map(this::mapDetailToResponse)
                    .collect(Collectors.toList());
        }

        return TicketResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .status(entity.getStatus())
                .ticketDetails(detailResponses)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private TicketDetailResponse mapDetailToResponse(TicketDetailEntity entity) {
        return TicketDetailResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .stockInitial(entity.getStockInitial())
                .stockAvailable(entity.getStockAvailable())
                .isStockPrepared(entity.getIsStockPrepared())
                .priceOriginal(entity.getPriceOriginal())
                .priceFlash(entity.getPriceFlash())
                .saleStartTime(entity.getSaleStartTime())
                .saleEndTime(entity.getSaleEndTime())
                .status(entity.getStatus())
                .ticketId(entity.getTicket() != null ? entity.getTicket().getId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
