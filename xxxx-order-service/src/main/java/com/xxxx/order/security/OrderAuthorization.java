package com.xxxx.order.security;

import com.xxxx.common.security.AuthenticatedUser;
import com.xxxx.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("orderAuthorization")
@RequiredArgsConstructor
public class OrderAuthorization {

    private final OrderRepository orderRepository;

    public boolean canAccessOrder(String orderNo, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return false;
        }
        if (user.hasRole("ADMIN")) {
            return true;
        }
        return orderRepository.findByOrderNo(orderNo)
                .map(order -> order.getUserId().equals(String.valueOf(user.userId())))
                .orElse(false);
    }

    public boolean canAccessUserOrders(String userId, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return false;
        }
        return user.hasRole("ADMIN") || String.valueOf(user.userId()).equals(userId);
    }
}
