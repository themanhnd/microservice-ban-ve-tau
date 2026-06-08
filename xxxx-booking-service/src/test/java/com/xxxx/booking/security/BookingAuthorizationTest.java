package com.xxxx.booking.security;

import com.xxxx.booking.repository.BookingRepository;
import com.xxxx.booking.repository.entity.BookingEntity;
import com.xxxx.common.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BookingAuthorizationTest {

    private final BookingRepository bookingRepository = mock(BookingRepository.class);
    private final BookingAuthorization authorization = new BookingAuthorization(bookingRepository);

    @Test
    void deniesUserAccessingAnotherUsersBooking() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(new BookingEntity().setId(10L).setUserId(2L)));

        assertThat(authorization.canAccessBooking(10L, auth(1L, "USER"))).isFalse();
    }

    @Test
    void allowsOwnerAndAdmin() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(new BookingEntity().setId(10L).setUserId(1L)));

        assertThat(authorization.canAccessBooking(10L, auth(1L, "USER"))).isTrue();
        assertThat(authorization.canAccessBooking(10L, auth(9L, "ADMIN"))).isTrue();
        assertThat(authorization.canAccessUserBookings(1L, auth(1L, "USER"))).isTrue();
        assertThat(authorization.canAccessUserBookings(2L, auth(9L, "ADMIN"))).isTrue();
    }

    private UsernamePasswordAuthenticationToken auth(Long userId, String role) {
        AuthenticatedUser user = new AuthenticatedUser(userId, "user@example.com", Set.of(role), "token-id");
        return new UsernamePasswordAuthenticationToken(user, null);
    }
}