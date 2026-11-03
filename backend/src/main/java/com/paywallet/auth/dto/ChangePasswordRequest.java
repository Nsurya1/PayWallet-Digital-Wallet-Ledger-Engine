package com.paywallet.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password cannot be blank")
        String currentPassword,

        @NotBlank(message = "New password cannot be blank")
        @Size(min = 8, max = 100, message = "New password must be at least 8 characters long")
        String newPassword
) {}
