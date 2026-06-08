package com.xxxx.order.security;

import com.xxxx.common.security.AuthenticatedUser;
import com.xxxx.order.repository.OrderRepository;
import com.xxxx.order.repository.entity.OrderEntity;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderAuthorizationTest {

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final OrderAuthorization authorization = new OrderAuthorization(orderRepository);

    @Test
    void deniesUserAccessingAnotherUsersOrder() {
        when(orderRepository.findByOrderNo("ORD-1")).thenReturn(Optional.of(OrderEntity.builder().orderNo("ORD-1").userId("2").build()));

        boolean allowed = authorization.canAccessOrder("ORD-1", auth(1L, "USER"));

        assertThat(allowed).isFalse();
    }

    @Test
    void allowsOwnerAndAdmin() {
        when(orderRepository.findByOrderNo("ORD-1")).thenReturn(Optional.of(OrderEntity.builder().orderNo("ORD-1").userId("1").build()));

        assertThat(authorization.canAccessOrder("ORD-1", auth(1L, "USER"))).isTrue();
        assertThat(authorization.canAccessOrder("ORD-1", auth(9L, "ADMIN"))).isTrue();
        assertThat(authorization.canAccessUserOrders("1", auth(1L, "USER"))).isTrue();
        assertThat(authorization.canAccessUserOrders("2", auth(9L, "ADMIN"))).isTrue();
    }

    private UsernamePasswordAuthenticationToken auth(Long userId, String role) {
        AuthenticatedUser user = new AuthenticatedUser(userId, "user@example.com", Set.of(role), "token-id");
        return new UsernamePasswordAuthenticationToken(user, null);
    }
}