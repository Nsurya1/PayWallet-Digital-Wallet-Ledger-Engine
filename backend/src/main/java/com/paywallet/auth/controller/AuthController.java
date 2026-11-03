package com.paywallet.auth.controller;

import com.paywallet.auth.dto.AuthResponse;
import com.paywallet.auth.dto.ChangePasswordRequest;
import com.paywallet.auth.dto.ForgotPasswordRequest;
import com.paywallet.auth.dto.ForgotPasswordResponse;
import com.paywallet.auth.dto.LoginRequest;
import com.paywallet.auth.dto.MessageResponse;
import com.paywallet.auth.dto.RegisterRequest;
import com.paywallet.auth.dto.ResetPasswordRequest;
import com.paywallet.auth.dto.UserSummary;
import com.paywallet.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "User registration, authentication, and credential lifecycle management")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user profile with hashed credentials and returns a JWT access token")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Verifies user credentials and returns a JWT access token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get current authenticated user profile", description = "Returns identity details extracted from the security context")
    public ResponseEntity<UserSummary> getCurrentUser() {
        UserSummary summary = authService.getCurrentUserProfile();
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset", description = "Generates a time-limited 6-digit OTP reset code for a given username or email")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        ForgotPasswordResponse response = authService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using OTP code", description = "Validates the reset OTP code and updates user's credentials with BCrypt encryption")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        MessageResponse response = authService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/change-password")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Change password for authenticated user", description = "Updates credentials for currently signed in user after verifying current password")
    public ResponseEntity<MessageResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        MessageResponse response = authService.changePassword(request);
        return ResponseEntity.ok(response);
    }
}
