package com.bluewave.apexbank.profiles.dto;


import com.bluewave.apexbank.profiles.Address;
import com.bluewave.apexbank.users.Users;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileResponseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID=1L;

    private String id;
    private String fullName;
    private String mobileNo;
    private Address address;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String username;
    private String email;
    private Set<String> roles;

}
