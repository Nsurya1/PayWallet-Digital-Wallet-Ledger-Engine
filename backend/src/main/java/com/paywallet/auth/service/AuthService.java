package com.paywallet.auth.service;

import com.paywallet.auth.domain.PasswordResetToken;
import com.paywallet.auth.domain.User;
import com.paywallet.auth.dto.AuthResponse;
import com.paywallet.auth.dto.ChangePasswordRequest;
import com.paywallet.auth.dto.ForgotPasswordRequest;
import com.paywallet.auth.dto.ForgotPasswordResponse;
import com.paywallet.auth.dto.LoginRequest;
import com.paywallet.auth.dto.MessageResponse;
import com.paywallet.auth.dto.RegisterRequest;
import com.paywallet.auth.dto.ResetPasswordRequest;
import com.paywallet.auth.dto.UserSummary;
import com.paywallet.auth.jwt.JwtTokenProvider;
import com.paywallet.auth.repository.PasswordResetTokenRepository;
import com.paywallet.auth.repository.UserRepository;
import com.paywallet.auth.security.SecurityUtils;
import com.paywallet.common.exception.BusinessException;
import com.paywallet.common.exception.ResourceNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("DUPLICATE_USERNAME", "Username '" + request.username() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("DUPLICATE_EMAIL", "Email '" + request.email() + "' is already registered");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = new User(request.username(), request.email(), passwordHash, "ROLE_USER");
        User savedUser = userRepository.save(user);

        String token = jwtTokenProvider.generateToken(savedUser);
        return AuthResponse.of(token, jwtTokenProvider.getExpirationSeconds(), UserSummary.from(savedUser));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        String token = jwtTokenProvider.generateToken(user);
        return AuthResponse.of(token, jwtTokenProvider.getExpirationSeconds(), UserSummary.from(user));
    }

    @Transactional(readOnly = true)
    public UserSummary getCurrentUserProfile() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));
        return UserSummary.from(user);
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        String identifier = request.identifier().trim();
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "No account found matching '" + identifier + "'"));

        // Clean up previous tokens for this user
        passwordResetTokenRepository.deleteAllByUserId(user.getId());

        // Generate 6-digit numeric OTP code
        String resetCode = String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 1000000));
        Instant expiresAt = Instant.now().plusSeconds(900); // 15 minutes

        PasswordResetToken token = new PasswordResetToken(user.getId(), resetCode, expiresAt);
        passwordResetTokenRepository.save(token);

        String maskedEmail = maskEmail(user.getEmail());
        String message = "Password reset code generated. For local & demo environments, your code is: " + resetCode;

        return new ForgotPasswordResponse(
                message,
                user.getUsername(),
                maskedEmail,
                resetCode,
                900
        );
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String identifier = request.identifier().trim();
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "No account found matching '" + identifier + "'"));

        String rawCode = request.resetCode().trim();
        PasswordResetToken token = passwordResetTokenRepository
                .findFirstByUserIdAndTokenAndUsedFalseOrderByCreatedAtDesc(user.getId(), rawCode)
                .orElseThrow(() -> new BusinessException("INVALID_RESET_CODE", "Invalid or expired reset code. Please request a new one."));

        if (token.isExpired()) {
            throw new BusinessException("EXPIRED_RESET_CODE", "Reset code has expired. Please request a new one.");
        }

        // Update password
        String newPasswordHash = passwordEncoder.encode(request.newPassword());
        user.setPasswordHash(newPasswordHash);
        userRepository.save(user);

        // Mark token as used
        token.setUsed(true);
        passwordResetTokenRepository.save(token);

        return new MessageResponse("Password has been reset successfully. You can now log in with your new password.");
    }

    @Transactional
    public MessageResponse changePassword(ChangePasswordRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Current password does not match");
        }

        if (request.currentPassword().equals(request.newPassword())) {
            throw new BusinessException("SAME_PASSWORD", "New password cannot be the same as your current password");
        }

        String newPasswordHash = passwordEncoder.encode(request.newPassword());
        user.setPasswordHash(newPasswordHash);
        userRepository.save(user);

        return new MessageResponse("Password updated successfully.");
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@", 2);
        String name = parts[0];
        String domain = parts[1];

        if (name.length() <= 2) {
            return name.charAt(0) + "***@" + domain;
        }
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + "@" + domain;
    }
}
