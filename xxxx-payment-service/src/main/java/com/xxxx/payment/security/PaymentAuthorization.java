package com.xxxx.payment.security;

import com.xxxx.common.security.AuthenticatedUser;
import com.xxxx.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("paymentAuthorization")
@RequiredArgsConstructor
public class PaymentAuthorization {

    private final PaymentRepository paymentRepository;

    public boolean canAccessTransaction(String transactionId, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return false;
        }
        if (user.hasRole("ADMIN")) {
            return true;
        }
        return paymentRepository.findByTransactionId(transactionId)
                .map(transaction -> transaction.getUserId().equals(String.valueOf(user.userId())))
                .orElse(false);
    }
}
