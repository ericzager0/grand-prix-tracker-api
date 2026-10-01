package com.uade.grandprixtracker.user.handler;

import com.uade.grandprixtracker.user.dto.UpdateUserProfileRequestDto;
import com.uade.grandprixtracker.user.dto.UserProfileResponseDto;
import com.uade.grandprixtracker.user.service.UserService;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class UserHandler {

    private final UserService userService;

    public UserHandler(UserService userService) {
        this.userService = userService;
    }

    public UserProfileResponseDto handleUpdateProfile(Jwt jwt, UpdateUserProfileRequestDto request) {
        UUID userId = extractUserId(jwt);
        String email = extractEmail(jwt);
        return userService.upsertProfile(userId, request, email);
    }

    public UserProfileResponseDto handleGetProfile(Jwt jwt) {
        UUID userId = extractUserId(jwt);
        return userService.getProfile(userId);
    }

    public UUID extractUserId(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new IllegalArgumentException("Token JWT inválido: no contiene identificador de usuario");
        }
        return UUID.fromString(jwt.getSubject());
    }

    public String extractEmail(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        return jwt.getClaimAsString("email");
    }
}
