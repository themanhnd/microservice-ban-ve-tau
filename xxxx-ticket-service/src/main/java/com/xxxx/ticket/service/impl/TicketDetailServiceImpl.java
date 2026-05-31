package com.xxxx.ticket.service.impl;

import com.xxxx.common.exception.ResourceNotFoundException;
import com.xxxx.ticket.controller.dto.request.CreateTicketDetailRequest;
import com.xxxx.ticket.controller.dto.request.UpdateTicketDetailRequest;
import com.xxxx.ticket.controller.dto.response.TicketDetailResponse;
import com.xxxx.ticket.repository.TicketDetailRepository;
import com.xxxx.ticket.repository.TicketRepository;
import com.xxxx.ticket.repository.entity.TicketDetailEntity;
import com.xxxx.ticket.repository.entity.TicketEntity;
import com.xxxx.ticket.service.TicketDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketDetailServiceImpl implements TicketDetailService {

    private final TicketDetailRepository ticketDetailRepository;
    private final TicketRepository ticketRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "ticketDetails", key = "'ticket:' + #ticketId")
    public List<TicketDetailResponse> getDetailsByTicketId(Long ticketId) {
        log.info("Getting all active details for ticket id: {}", ticketId);

        // Validate ticket exists
        ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));

        List<TicketDetailEntity> details = ticketDetailRepository.findByTicketIdAndStatusNot(ticketId, 2);
        return details.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "ticketDetails", key = "#id")
    public TicketDetailResponse getDetailById(Long id) {
        log.info("Getting ticket detail by id: {}", id);
        TicketDetailEntity detail = ticketDetailRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TicketDetail", id));
        return mapToResponse(detail);
    }

    @Override
    @Transactional
    @CacheEvict(value = "ticketDetails", allEntries = true)
    public TicketDetailResponse createDetail(CreateTicketDetailRequest request) {
        log.info("Creating ticket detail for ticket id: {}", request.getTicketId());

        // Validate ticket exists
        TicketEntity ticket = ticketRepository.findById(request.getTicketId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", request.getTicketId()));

        TicketDetailEntity detail = TicketDetailEntity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .stockInitial(request.getStockInitial())
                .stockAvailable(request.getStockInitial()) // stockAvailable = stockInitial on creation
                .isStockPrepared(false)
                .priceOriginal(request.getPriceOriginal())
                .priceFlash(request.getPriceFlash())
                .saleStartTime(request.getSaleStartTime())
                .saleEndTime(request.getSaleEndTime())
                .status(1) // ACTIVE
                .ticket(ticket)
                .build();

        TicketDetailEntity savedDetail = ticketDetailRepository.save(detail);
        log.info("Created ticket detail with id: {}", savedDetail.getId());
        return mapToResponse(savedDetail);
    }

    @Override
    @Transactional
    @CacheEvict(value = "ticketDetails", allEntries = true)
    public TicketDetailResponse updateDetail(Long id, UpdateTicketDetailRequest request) {
        log.info("Updating ticket detail with id: {}", id);

        TicketDetailEntity detail = ticketDetailRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TicketDetail", id));

        detail.setName(request.getName());
        detail.setDescription(request.getDescription());
        detail.setStockInitial(request.getStockInitial());
        detail.setPriceOriginal(request.getPriceOriginal());
        detail.setPriceFlash(request.getPriceFlash());
        detail.setSaleStartTime(request.getSaleStartTime());
        detail.setSaleEndTime(request.getSaleEndTime());
        detail.setStatus(request.getStatus());

        TicketDetailEntity updatedDetail = ticketDetailRepository.save(detail);
        log.info("Updated ticket detail with id: {}", updatedDetail.getId());
        return mapToResponse(updatedDetail);
    }

    @Override
    @Transactional
    @CacheEvict(value = "ticketDetails", allEntries = true)
    public void deleteDetail(Long id) {
        log.info("Soft deleting ticket detail with id: {}", id);

        TicketDetailEntity detail = ticketDetailRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TicketDetail", id));

        detail.setStatus(2); // DELETED
        ticketDetailRepository.save(detail);
        log.info("Soft deleted ticket detail with id: {}", id);
    }

    // ========== PRIVATE HELPER METHODS ==========

    private TicketDetailResponse mapToResponse(TicketDetailEntity entity) {
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
