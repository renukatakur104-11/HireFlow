package com.hireflow.modules.auth;

import com.hireflow.common.exception.ConflictException;
import com.hireflow.common.exception.UnauthorizedException;
import com.hireflow.modules.auth.dto.*;
import com.hireflow.modules.student.Skill;
import com.hireflow.modules.student.SkillRepository;
import com.hireflow.modules.student.StudentProfile;
import com.hireflow.modules.student.StudentProfileRepository;
import com.hireflow.modules.user.Role;
import com.hireflow.modules.user.User;
import com.hireflow.modules.user.UserRepository;
import com.hireflow.security.JwtTokenProvider;
import com.hireflow.security.RateLimiterService;
import com.hireflow.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final SkillRepository skillRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final RateLimiterService rateLimiterService;

    @Value("${hireflow.jwt.access-token-expiration-ms:900000}")
    private long accessTokenExpirationMs;

    @Value("${hireflow.jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered: " + email);
        }

        // 1. Create and persist User (strictly STUDENT role)
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.STUDENT)
                .active(true)
                .build();
        user = userRepository.save(user);

        // 2. Resolve Skills
        Set<Skill> studentSkills = new HashSet<>();
        if (request.getSkills() != null) {
            for (String skillName : request.getSkills()) {
                String cleanName = skillName.trim();
                if (!cleanName.isBlank()) {
                    Skill skill = skillRepository.findByNameIgnoreCase(cleanName)
                            .orElseGet(() -> skillRepository.save(Skill.builder().name(cleanName).build()));
                    studentSkills.add(skill);
                }
            }
        }

        // 3. Create and persist Student Profile
        StudentProfile profile = StudentProfile.builder()
                .user(user)
                .fullName(request.getFullName().trim())
                .phone(request.getPhone().trim())
                .branch(request.getBranch().trim())
                .graduationYear(request.getGraduationYear())
                .cgpa(request.getCgpa())
                .skills(studentSkills)
                .build();
        profile.recomputeCompleteness();
        studentProfileRepository.save(profile);

        // 4. Generate Auth Tokens
        UserPrincipal principal = UserPrincipal.create(user);
        String accessToken = tokenProvider.generateAccessToken(principal);
        String rawRefreshToken = UUID.randomUUID().toString();
        saveRefreshToken(user, rawRefreshToken, httpRequest);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .expiresInMs(accessTokenExpirationMs)
                .user(buildUserSummary(user, profile))
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        String rateLimitKey = clientIp + ":" + request.getEmail();
        rateLimiterService.checkLoginRateLimit(rateLimitKey);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail().trim().toLowerCase(), request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

            User user = userRepository.findById(principal.getId())
                    .orElseThrow(() -> new UnauthorizedException("User not found"));

            rateLimiterService.resetAttempts(rateLimitKey);

            String accessToken = tokenProvider.generateAccessToken(principal);
            String rawRefreshToken = UUID.randomUUID().toString();
            saveRefreshToken(user, rawRefreshToken, httpRequest);

            StudentProfile profile = studentProfileRepository.findByUserId(user.getId()).orElse(null);

            return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .expiresInMs(accessTokenExpirationMs)
                .user(buildUserSummary(user, profile))
                .build();

        } catch (BadCredentialsException ex) {
            rateLimiterService.recordFailedAttempt(rateLimitKey);
            throw new UnauthorizedException("Invalid email or password");
        }
    }

    @Transactional
    public AuthResponse refresh(TokenRefreshRequest request, HttpServletRequest httpRequest) {
        String rawToken = request.getRefreshToken();
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid or unrecognized refresh token"));

        User user = refreshToken.getUser();

        // Security reuse alarm: if revoked token is presented, invalidate ALL tokens for user!
        if (refreshToken.isRevoked()) {
            log.warn("Security Alert: Reused revoked refresh token detected for user: {}", user.getEmail());
            refreshTokenRepository.revokeAllActiveTokensForUser(user);
            throw new UnauthorizedException("Refresh token reuse detected. All active sessions invalidated.");
        }

        if (refreshToken.isExpired()) {
            throw new UnauthorizedException("Refresh token has expired. Please log in again.");
        }

        // Token rotation: revoke current, issue new
        String newRawRefreshToken = UUID.randomUUID().toString();
        String newTokenHash = hashToken(newRawRefreshToken);

        refreshToken.setRevoked(true);
        refreshToken.setReplacedByTokenHash(newTokenHash);
        refreshTokenRepository.save(refreshToken);

        // Save new active refresh token
        RefreshToken newRefreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(newTokenHash)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000))
                .ipAddress(httpRequest.getRemoteAddr())
                .userAgent(httpRequest.getHeader("User-Agent"))
                .revoked(false)
                .build();
        refreshTokenRepository.save(newRefreshToken);

        UserPrincipal principal = UserPrincipal.create(user);
        String accessToken = tokenProvider.generateAccessToken(principal);
        StudentProfile profile = studentProfileRepository.findByUserId(user.getId()).orElse(null);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRawRefreshToken)
                .expiresInMs(accessTokenExpirationMs)
                .user(buildUserSummary(user, profile))
                .build();
    }

    @Transactional
    public void logout(TokenRefreshRequest request) {
        if (request != null && request.getRefreshToken() != null) {
            String tokenHash = hashToken(request.getRefreshToken());
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
        }
    }

    @Transactional(readOnly = true)
    public UserSummaryResponse getCurrentUser(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        StudentProfile profile = studentProfileRepository.findByUserId(user.getId()).orElse(null);
        return buildUserSummary(user, profile);
    }

    private void saveRefreshToken(User user, String rawToken, HttpServletRequest request) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(rawToken))
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000))
                .ipAddress(request != null ? request.getRemoteAddr() : null)
                .userAgent(request != null ? request.getHeader("User-Agent") : null)
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);
    }

    private UserSummaryResponse buildUserSummary(User user, StudentProfile profile) {
        return UserSummaryResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .fullName(profile != null ? profile.getFullName() : (user.getRole() == Role.ADMIN ? "Placement Admin" : "User"))
                .branch(profile != null ? profile.getBranch() : null)
                .cgpa(profile != null ? profile.getCgpa() : null)
                .profileComplete(profile != null && profile.isProfileComplete())
                .hasResume(profile != null && profile.getResumeFilePath() != null)
                .build();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
