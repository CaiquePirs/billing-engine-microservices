package com.billing.customers.controller.advice.handler;

import com.billing.customers.controller.advice.dto.ErrorMessageDTO;
import com.billing.customers.controller.advice.dto.ErrorResponseDTO;
import com.billing.customers.controller.advice.exceptions.CustomerExistException;
import com.billing.customers.controller.advice.exceptions.CustomerNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<ErrorMessageDTO> listErrors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new ErrorMessageDTO(fe.getField(), fe.getDefaultMessage()))
                .collect(Collectors.toList());

        ErrorResponseDTO error = new ErrorResponseDTO(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "Validation error",
                LocalDateTime.now(),
                listErrors
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

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

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleCustomerNotFoundException(CustomerNotFoundException exception) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponseDTO(
                        HttpStatus.NOT_FOUND.value(),
                        exception.getMessage(),
                        LocalDateTime.now(),
                        List.of(new ErrorMessageDTO("Not Found", exception.getMessage()))
                ));
    }
}
