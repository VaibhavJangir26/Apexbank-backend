package com.bluewave.apexbank.otp;

import com.bluewave.apexbank.auth.dto.AuthSignupDTO;
import com.bluewave.apexbank.utils.exceptions.BadRequestExceptoin;
import com.bluewave.apexbank.utils.exceptions.GeneralException;
import com.bluewave.apexbank.utils.exceptions.TooManyRequestException;
import com.bluewave.apexbank.utils.services.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final String OTP_PREFIX = "OTP:";
    private static final String ATTEMPT_PREFIX = "ATTEMPT:";
    private static final String COOLDOWN_PREFIX = "COOLDOWN:";
    private static final String PAYLOAD_PREFIX = "PAYLOAD:";

    private static final int OTP_EXPIRE_TIME_MIN = 5;
    private static final int COOLDOWN_SECONDS = 120;
    private static final int MAX_ATTEMPTS = 3;

    private final StringRedisTemplate stringRedisTemplate;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int num = 100000 + random.nextInt(900000);
        return String.valueOf(num);
    }

    public void generateAndSendOtp(AuthSignupDTO dto) {
        String normalizeEmail = dto.getEmail().trim().toLowerCase();
        String cooldownKey = COOLDOWN_PREFIX + normalizeEmail;

        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(cooldownKey))) {
            throw new TooManyRequestException("Please wait 120 seconds before requesting a new OTP.");
        }

        String otp = generateOtp();
        String otpKey = OTP_PREFIX + normalizeEmail;
        String otpAttemptKey = ATTEMPT_PREFIX + normalizeEmail;
        String otpPayloadKey = PAYLOAD_PREFIX + normalizeEmail;

        try {
            String signupData = objectMapper.writeValueAsString(dto);

            stringRedisTemplate.opsForValue().set(otpKey, otp, Duration.ofMinutes(OTP_EXPIRE_TIME_MIN));
            stringRedisTemplate.opsForValue().set(otpAttemptKey, "0", Duration.ofMinutes(OTP_EXPIRE_TIME_MIN));
            stringRedisTemplate.opsForValue().set(otpPayloadKey, signupData, Duration.ofMinutes(OTP_EXPIRE_TIME_MIN));
            stringRedisTemplate.opsForValue().set(cooldownKey, "true", Duration.ofSeconds(COOLDOWN_SECONDS));

            String emailBody = String.format("Your ApexBank verification code is: %s.\nThis code is valid for %d minutes.", otp, OTP_EXPIRE_TIME_MIN);
            emailService.sendEmail(normalizeEmail, "Verification Code", emailBody);
        } catch (Exception e) {
            clearOtpSession(normalizeEmail);
            log.error("Failed to send OTP for {}, reason: {}", normalizeEmail, e.getMessage());
            throw new GeneralException("Failed to send verification code. Please try again.");
        }
    }

    public AuthSignupDTO verifyAndGetPayload(VerifyOtpRequestDTO dto) {
        String normalizeEmail = dto.getEmail().trim().toLowerCase();
        String otpKey = OTP_PREFIX + normalizeEmail;
        String otpAttemptKey = ATTEMPT_PREFIX + normalizeEmail;
        String otpPayloadKey = PAYLOAD_PREFIX + normalizeEmail;

        String cachedOtp = stringRedisTemplate.opsForValue().get(otpKey);
        if (cachedOtp == null || cachedOtp.isEmpty()) {
            throw new BadRequestExceptoin("OTP is expired or was not requested. Please try again.");
        }

        Long increaseAttempt = stringRedisTemplate.opsForValue().increment(otpAttemptKey);
        if (increaseAttempt != null && increaseAttempt > MAX_ATTEMPTS) {
            clearOtpSession(normalizeEmail);
            throw new TooManyRequestException("Maximum limit reached for OTP verification attempts.");
        }

        if (!cachedOtp.equals(dto.getOtp().trim())) {
            long remainingAttempts = Math.max(0, MAX_ATTEMPTS - (increaseAttempt != null ? increaseAttempt : 0));
            throw new BadRequestExceptoin("Invalid OTP. Remaining attempts: " + remainingAttempts);
        }

        String payloadData = stringRedisTemplate.opsForValue().get(otpPayloadKey);
        if (payloadData == null || payloadData.isEmpty()) {
            throw new BadRequestExceptoin("Registration session expired. Please try again.");
        }

        try {
            AuthSignupDTO signupDTO = objectMapper.readValue(payloadData, AuthSignupDTO.class);
            clearOtpSession(normalizeEmail);
            return signupDTO;
        } catch (Exception e) {
            log.error("Failed to process payload data for {}: {}", normalizeEmail, e.getMessage());
            throw new GeneralException("Verification processing failed. Please try again.");
        }
    }

    private void clearOtpSession(String email) {
        stringRedisTemplate.delete(OTP_PREFIX + email);
        stringRedisTemplate.delete(ATTEMPT_PREFIX + email);
        stringRedisTemplate.delete(PAYLOAD_PREFIX + email);
        stringRedisTemplate.delete(COOLDOWN_PREFIX + email);
        log.info("Redis OTP session cleaned for {}", email);
    }
}