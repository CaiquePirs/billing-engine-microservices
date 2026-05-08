package com.billing.customers.controller.advice.handler;

import com.billing.customers.controller.advice.dto.ErrorMessageDTO;
import com.billing.customers.controller.advice.dto.ErrorResponseDTO;
import com.billing.customers.controller.advice.exceptions.CustomerExistException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomerExistException.class)
    public ResponseEntity<ErrorResponseDTO> handleCustomerExistException(CustomerExistException exception) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponseDTO(
                        HttpStatus.CONFLICT.value(),
                        exception.getMessage(),
                        LocalDateTime.now(),
                        List.of(new ErrorMessageDTO("Conflict", exception.getMessage()))
                ));
    }
}
