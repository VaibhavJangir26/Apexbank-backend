package com.bluewave.apexbank.utils.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommonApiResponse<T> {

    private String message;
    private boolean success;
    private LocalDateTime timestamp;
    private  T data;
    private int statusCode;


}
