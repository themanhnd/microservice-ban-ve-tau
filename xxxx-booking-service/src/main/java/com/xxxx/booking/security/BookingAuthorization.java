package com.xxxx.booking.security;

import com.xxxx.booking.repository.BookingRepository;
import com.xxxx.common.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("bookingAuthorization")
@RequiredArgsConstructor
public class BookingAuthorization {

    private final BookingRepository bookingRepository;

    public boolean canAccessBooking(Long bookingId, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return false;
        }
        if (user.hasRole("ADMIN")) {
            return true;
        }
        return bookingRepository.findById(bookingId)
                .map(booking -> booking.getUserId().equals(user.userId()))
                .orElse(false);
    }

    public boolean canAccessUserBookings(Long userId, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return false;
        }
        return user.hasRole("ADMIN") || user.userId().equals(userId);
    }
}
