package com.bluewave.apexbank.auth;

import com.bluewave.apexbank.auth.dto.AuthLoginRequestDTO;
import com.bluewave.apexbank.auth.dto.AuthNewTokenRequestDTO;
import com.bluewave.apexbank.auth.dto.AuthResponseDTO;
import com.bluewave.apexbank.auth.dto.AuthSignupDTO;
import com.bluewave.apexbank.otp.OtpService;
import com.bluewave.apexbank.otp.VerifyOtpRequestDTO;
import com.bluewave.apexbank.users.Roles;
import com.bluewave.apexbank.users.RolesRepo;
import com.bluewave.apexbank.users.Users;
import com.bluewave.apexbank.users.UsersRepo;
import com.bluewave.apexbank.utils.common.AppRole;
import com.bluewave.apexbank.utils.exceptions.BadRequestExceptoin;
import com.bluewave.apexbank.utils.exceptions.ResourceNotFoundException;
import com.bluewave.apexbank.utils.services.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    // Repositories for DB interaction
    private final UsersRepo usersRepo;
    private final RolesRepo rolesRepo;

    // Core Security, OTP Services & Redis Access
    private final JwtService jwtService;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final StringRedisTemplate redisTemplate;

    // Constants for Session Storage
    private static final String REDIS_SESSION_PREFIX = "user:";
    private static final long REFRESH_TOKEN_VALIDITY_DAYS = 7;


    @Transactional(readOnly = true)
    public Map<String, Boolean> checkAvailable(String email, String username) {
        Map<String, Boolean> result = new HashMap<>();
        if (username != null && !username.isBlank()) {
            boolean isUsernameAvailable = usersRepo.findByUsername(username.trim()).isEmpty();
            result.put("usernameAvailable", isUsernameAvailable);
        }
        if (email != null && !email.isBlank()) {
            boolean isEmailAvailable = usersRepo.findByEmail(email.trim().toLowerCase()).isEmpty();
            result.put("emailAvailable", isEmailAvailable);
        }

        return result;
    }

    /**
     * Step 1 of Signup Flow:
     * Validates that username/email are not in DB, then caches signup data in Redis and sends OTP email.
     */
    public String signup(AuthSignupDTO dto) {
        String normalizedEmail = dto.getEmail().trim().toLowerCase();
        String normalizedUsername = dto.getUsername().trim();

        // 1. Double-check if email already exists in Postgres DB
        if (usersRepo.findByEmail(normalizedEmail).isPresent()) {
            throw new BadRequestExceptoin("Email is already registered. Please login.");
        }

        // 2. Double-check if username already exists in Postgres DB
        if (usersRepo.findByUsername(normalizedUsername).isPresent()) {
            throw new BadRequestExceptoin("Username is already taken. Please choose another.");
        }

        // 3. Delegate to OtpService: Stores signup DTO temporarily in Redis & triggers email
        otpService.generateAndSendOtp(dto);

        log.info("OTP generated and sent to email: {}", normalizedEmail);
        return "OTP sent successfully to your email. Please verify to complete registration.";
    }

    /**
     * Step 2 of Signup Flow:
     * Verifies OTP from Redis, creates user in PostgreSQL, generates a Session ID,
     * stores the refresh token in Redis under 'user:<username>:<sessionId>', and returns tokens.
     */
    @Transactional
    public AuthResponseDTO verify(VerifyOtpRequestDTO dto) {
        // 1. Verify OTP using OtpService and retrieve cached AuthSignupDTO payload from Redis
        AuthSignupDTO cachedSignupData = otpService.verifyAndGetPayload(dto);

        String email = cachedSignupData.getEmail().trim().toLowerCase();
        String username = cachedSignupData.getUsername().trim();

        // 2. Final concurrency check before saving to DB
        if (usersRepo.findByEmail(email).isPresent() || usersRepo.findByUsername(username).isPresent()) {
            throw new BadRequestExceptoin("User account already exists in database.");
        }

        // 3. Fetch default ROLE_CUSTOMER from DB (or create if not present)
        Roles customerRole = rolesRepo.findByAppRole(AppRole.ROLE_CUSTOMER)
                .orElseGet(() -> rolesRepo.save(new Roles(null, AppRole.ROLE_CUSTOMER, Set.of())));

        // 4. Build and save new Users entity to PostgresSQL
        Users newUser = new Users();
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(cachedSignupData.getPassword()));
        newUser.setRoles(Set.of(customerRole));

        Users savedUser = usersRepo.save(newUser);
        log.info("New user successfully registered in PostgreSQL with ID: {}", savedUser.getId());

        // 5. Generate a unique Session ID for stateful session tracking
        String sessionId = UUID.randomUUID().toString();

        // 6. Create Authentication Token for Access Token generation
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(AppRole.ROLE_CUSTOMER.name()));
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(savedUser.getUsername(), null, authorities);

        // 7. Generate JWT Access Token and Refresh Token containing the sessionId claim
        String accessToken = jwtService.generateAccessToken(authenticationToken);
        String refreshToken = jwtService.generateRefreshToken(username, sessionId);

        // 8. Store Refresh Token in Redis: key = "user:<username>:<sessionId>" with 7-day TTL
        saveSessionToRedis(username, sessionId, refreshToken);

        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .roles(Set.of(AppRole.ROLE_CUSTOMER.name()))
                .message("User verified and registered successfully!")
                .build();
    }

    /**
     * Login Endpoint:
     * Authenticates credentials via AuthenticationManager, generates a unique Session ID,
     * stores the Refresh Token in Redis under 'user:<username>:<sessionId>', and returns tokens.
     */
    @Transactional(readOnly = true)
    public AuthResponseDTO login(AuthLoginRequestDTO dto) {
        String identifier = dto.getUsername().trim();

        // 1. Authenticate credentials via Spring Security
        Authentication authResult;
        try {
            authResult = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(identifier, dto.getPassword())
            );
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid username/email or password.");
        }

        // 2. Load user details from DB to fetch RBAC roles
        String authenticatedUsername = authResult.getName();
        Users user = usersRepo.findByUsername(authenticatedUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authenticatedUsername));

        // 3. Extract roles for RBAC frontend routing
        Set<String> roleNames = user.getRoles().stream()
                .map(role -> role.getAppRole().name())
                .collect(Collectors.toSet());

        // 4. Generate a unique Session ID for this specific device/session
        String sessionId = UUID.randomUUID().toString();

        // 5. Generate Access Token and Refresh Token (embedding the sessionId inside the Refresh JWT)
        String accessToken = jwtService.generateAccessToken(authResult);
        String refreshToken = jwtService.generateRefreshToken(authenticatedUsername, sessionId);

        // 6. Store Refresh Token in Redis: key = "user:<username>:<sessionId>" with 7-day TTL
        saveSessionToRedis(authenticatedUsername, sessionId, refreshToken);

        log.info("User {} successfully logged in. Session created in Redis with ID: {}", authenticatedUsername, sessionId);

        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .roles(roleNames)
                .message("Login successful")
                .build();
    }

    /**
     * Token Refresh Endpoint (Stateful Session Check - NO Token Rotation):
     * 1. Validates Refresh Token cryptographic signature.
     * 2. Extracts username & sessionId claims from the Refresh Token.
     * 3. Checks Redis for key 'user:<username>:<sessionId>'.
     *    - If missing or mismatched: session was revoked/logged out -> Reject with 401.
     *    - If valid: Keeps the SAME Refresh Token and issues only a NEW Access Token.
     */
    @Transactional(readOnly = true)
    public AuthResponseDTO refresh(AuthNewTokenRequestDTO dto) {
        String incomingRefreshToken = dto.getRefreshToken();

        // 1. Validate JWT cryptographic signature and expiration
        if (!jwtService.isTokenValid(incomingRefreshToken)) {
            throw new BadCredentialsException("Refresh token is invalid or expired.");
        }

        // 2. Extract claims from incoming token
        String username = jwtService.extractUsernameFromToken(incomingRefreshToken);
        String sessionId = jwtService.extractSessionIdFromToken(incomingRefreshToken);

        if (sessionId == null || sessionId.isBlank()) {
            throw new BadCredentialsException("Invalid session identifier in refresh token.");
        }

        // 3. Redis Validation: Verify active session exists in Redis
        String redisKey = buildRedisKey(username, sessionId);
        String storedRefreshToken = redisTemplate.opsForValue().get(redisKey);

        if (storedRefreshToken == null) {
            log.warn("Refresh attempt failed: Session {} for user {} has been revoked or expired in Redis", sessionId, username);
            throw new BadCredentialsException("Session has been revoked or logged out. Please login again.");
        }

        if (!storedRefreshToken.equals(incomingRefreshToken)) {
            log.error("Security Alert: Mismatched refresh token for user {} on session {}", username, sessionId);
            throw new BadCredentialsException("Invalid session token payload.");
        }

        // 4. Load user details from DB to build fresh Spring Security GrantedAuthorities
        Users user = usersRepo.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getAppRole().name()))
                .toList();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user.getUsername(), null, authorities);

        // 5. Issue ONLY a fresh Access Token (Reuse the existing valid Refresh Token)
        String newAccessToken = jwtService.generateAccessToken(auth);

        log.info("Access token successfully refreshed for user {} on session {}", username, sessionId);

        return AuthResponseDTO.builder()
                .accessToken(newAccessToken)
                .refreshToken(incomingRefreshToken) // 👈 Returns the existing valid Refresh Token without rotating
                .roles(user.getRoles().stream().map(r -> r.getAppRole().name()).collect(Collectors.toSet()))
                .message("Access token refreshed successfully")
                .build();
    }

    /**
     * Logout Endpoint (Explicit Invalidation):
     * Validates incoming Refresh Token, extracts username and sessionId,
     * and deletes 'user:<username>:<sessionId>' from Redis.
     * This instantly invalidates the session and blocks any future token refresh calls.
     */
    public String logout(AuthNewTokenRequestDTO dto) {
        String refreshToken = dto.getRefreshToken();

        // 1. Validate refresh token structure and signature
        if (!jwtService.isTokenValid(refreshToken)) {
            throw new BadCredentialsException("Invalid or expired refresh token.");
        }

        // 2. Extract claims
        String username = jwtService.extractUsernameFromToken(refreshToken);
        String sessionId = jwtService.extractSessionIdFromToken(refreshToken);

        // 3. Remove session key from Redis to destroy session
        if (sessionId != null) {
            String redisKey = buildRedisKey(username, sessionId);
            Boolean deleted = redisTemplate.delete(redisKey);

            if (Boolean.TRUE.equals(deleted)) {
                log.info("Session {} for user {} successfully deleted from Redis", sessionId, username);
            } else {
                log.warn("Logout executed, but session key {} was already missing in Redis", redisKey);
            }
        }

        return "Logged out successfully.";
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    /**
     * Helper to construct unified Redis Key structure: user:<username>:<sessionId>
     */
    private String buildRedisKey(String username, String sessionId) {
        return REDIS_SESSION_PREFIX + username + ":" + sessionId;
    }

    /**
     * Saves session payload into Redis with explicit 7-day TTL matching refresh token lifespan.
     */
    private void saveSessionToRedis(String username, String sessionId, String refreshToken) {
        String redisKey = buildRedisKey(username, sessionId);
        redisTemplate.opsForValue().set(
                redisKey,
                refreshToken,
                REFRESH_TOKEN_VALIDITY_DAYS,
                TimeUnit.DAYS
        );
    }
}