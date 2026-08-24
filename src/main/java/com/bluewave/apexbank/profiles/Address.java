package com.bluewave.apexbank.profiles;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class Address implements Serializable {

    @Serial
    private static final long serialVersionUID=1L;

    private String city;
    private String state;
    private String addressLine;
    private String postalCode;

}