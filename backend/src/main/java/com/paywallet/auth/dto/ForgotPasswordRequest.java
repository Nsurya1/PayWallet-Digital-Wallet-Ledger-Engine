package com.paywallet.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(
        @NotBlank(message = "Username or email is required")
        @Size(max = 255, message = "Identifier cannot exceed 255 characters")
        String identifier
) {}
