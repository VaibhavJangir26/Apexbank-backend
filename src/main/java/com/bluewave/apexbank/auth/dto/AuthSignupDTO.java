package com.bluewave.apexbank.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthSignupDTO {

    @NotBlank(message = "username is required")
    private String username;

    @NotBlank(message = "email is required")
    @Email
    private String email;

    @NotBlank(message = "password is required")
    @Size(min = 6)
    private String password;

}
