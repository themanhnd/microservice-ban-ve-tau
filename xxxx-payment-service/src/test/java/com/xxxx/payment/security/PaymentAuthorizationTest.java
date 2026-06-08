package com.xxxx.payment.security;

import com.xxxx.common.security.AuthenticatedUser;
import com.xxxx.payment.repository.PaymentRepository;
import com.xxxx.payment.repository.entity.PaymentTransactionEntity;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentAuthorizationTest {

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final PaymentAuthorization authorization = new PaymentAuthorization(paymentRepository);

    @Test
    void deniesUserAccessingAnotherUsersTransaction() {
        when(paymentRepository.findByTransactionId("TX-1")).thenReturn(Optional.of(PaymentTransactionEntity.builder().transactionId("TX-1").userId("2").build()));

        assertThat(authorization.canAccessTransaction("TX-1", auth(1L, "USER"))).isFalse();
    }

    @Test
    void allowsOwnerAndAdmin() {
        when(paymentRepository.findByTransactionId("TX-1")).thenReturn(Optional.of(PaymentTransactionEntity.builder().transactionId("TX-1").userId("1").build()));

        assertThat(authorization.canAccessTransaction("TX-1", auth(1L, "USER"))).isTrue();
        assertThat(authorization.canAccessTransaction("TX-1", auth(9L, "ADMIN"))).isTrue();
    }

    private UsernamePasswordAuthenticationToken auth(Long userId, String role) {
        AuthenticatedUser user = new AuthenticatedUser(userId, "user@example.com", Set.of(role), "token-id");
        return new UsernamePasswordAuthenticationToken(user, null);
    }
}