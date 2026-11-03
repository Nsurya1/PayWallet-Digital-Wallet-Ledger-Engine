package com.paywallet.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Username or email is required")
        String identifier,

        @NotBlank(message = "Reset code is required")
        @Size(min = 6, max = 64, message = "Reset code must be valid")
        String resetCode,

        @NotBlank(message = "New password cannot be blank")
        @Size(min = 8, max = 100, message = "New password must be at least 8 characters long")
        String newPassword
) {}
