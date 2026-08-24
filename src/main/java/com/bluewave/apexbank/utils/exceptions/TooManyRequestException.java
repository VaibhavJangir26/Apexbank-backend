package com.bluewave.apexbank.utils.exceptions;

public class TooManyRequestException extends RuntimeException {
    public TooManyRequestException(String message) {
        super(message);
    }
}
