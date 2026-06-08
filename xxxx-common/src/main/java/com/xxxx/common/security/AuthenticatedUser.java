package com.xxxx.common.security;

import java.util.Set;

public record AuthenticatedUser(
        Long userId,
        String email,
        Set<String> roles,
        String tokenId
) {
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}
