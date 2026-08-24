package com.bluewave.apexbank.otp;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyOtpRequestDTO {
    @Email(message = "invalid email format")
    @NotBlank(message = "email is required")
    private String email;


    @NotBlank(message = "otp is required")
    @Pattern(regexp = "^\\d{6}$",message = "otp must of 6 digit")
    private String otp;


}
