package com.paywallet.auth.dto;

public record ForgotPasswordResponse(
        String message,
        String identifier,
        String maskedEmail,
        String resetCode,
        long expiresInSeconds
) {}
