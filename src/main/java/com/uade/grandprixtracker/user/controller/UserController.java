package com.uade.grandprixtracker.user.controller;

import com.uade.grandprixtracker.shared.response.ApiResponse;
import com.uade.grandprixtracker.user.dto.UpdateUserProfileRequestDto;
import com.uade.grandprixtracker.user.dto.UserProfileResponseDto;
import com.uade.grandprixtracker.user.handler.UserHandler;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserHandler userHandler;

    public UserController(UserHandler userHandler) {
        this.userHandler = userHandler;
    }

    @PutMapping({"/users/profile", "/profile"})
    public ResponseEntity<ApiResponse<UserProfileResponseDto>> updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateUserProfileRequestDto request) {
        UserProfileResponseDto response = userHandler.handleUpdateProfile(jwt, request);
        return ResponseEntity.ok(ApiResponse.success("Perfil actualizado correctamente", response));
    }

    @GetMapping({"/users/profile", "/profile"})
    public ResponseEntity<ApiResponse<UserProfileResponseDto>> getProfile(
            @AuthenticationPrincipal Jwt jwt) {
        UserProfileResponseDto response = userHandler.handleGetProfile(jwt);
        return ResponseEntity.ok(ApiResponse.success("Perfil obtenido correctamente", response));
    }
}
