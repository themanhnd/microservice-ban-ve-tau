package com.xxxx.inventory.job;

import com.xxxx.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "inventory.reconciliation",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class InventoryReconciliationJob {

    private final InventoryService inventoryService;

    @Scheduled(fixedDelayString = "${inventory.reconciliation.fixed-delay:300000}")
    public void reconcileStock() {
        log.debug("Starting inventory reconciliation job");
        inventoryService.reconcileAllStockToRedis();
    }
}
