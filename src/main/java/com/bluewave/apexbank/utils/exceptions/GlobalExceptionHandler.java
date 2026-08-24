package com.bluewave.apexbank.utils.exceptions;

import com.bluewave.apexbank.utils.common.CommonApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    private ResponseEntity<CommonApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                CommonApiResponse.<Void>builder()
                        .message(ex.getMessage())
                        .data(null)
                        .success(false)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
    @ExceptionHandler(BadRequestExceptoin.class)
    private ResponseEntity<CommonApiResponse<Void>> handleBadRequestException(BadRequestExceptoin ex){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                CommonApiResponse.<Void>builder()
                        .message(ex.getMessage())
                        .data(null)
                        .success(false)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
    @ExceptionHandler(ConflictException.class)
    private ResponseEntity<CommonApiResponse<Void>> handleConflictException(ConflictException ex){
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                CommonApiResponse.<Void>builder()
                        .message(ex.getMessage())
                        .data(null)
                        .success(false)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
    @ExceptionHandler(GeneralException.class)
    private ResponseEntity<CommonApiResponse<Void>> handleGeneralException(GeneralException ex){
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                CommonApiResponse.<Void>builder()
                        .message(ex.getMessage())
                        .data(null)
                        .success(false)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
    @ExceptionHandler(TooManyRequestException.class)
    private ResponseEntity<CommonApiResponse<Void>> handleTooManyRequestException(TooManyRequestException ex){
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                CommonApiResponse.<Void>builder()
                        .message(ex.getMessage())
                        .data(null)
                        .success(false)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
    @ExceptionHandler(UnauthorizedException.class)
    private ResponseEntity<CommonApiResponse<Void>> handleUnauthorizedException(UnauthorizedException ex){
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                CommonApiResponse.<Void>builder()
                        .message(ex.getMessage())
                        .data(null)
                        .success(false)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    private ResponseEntity<CommonApiResponse<Map<String,String>>> handleValidationException(MethodArgumentNotValidException ex){
        Map<String,String> error=new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e->error.put(e.getField(),e.getDefaultMessage()));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                CommonApiResponse.<Map<String,String>>builder()
                        .message("validation failed")
                        .data(error)
                        .success(false)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

}
