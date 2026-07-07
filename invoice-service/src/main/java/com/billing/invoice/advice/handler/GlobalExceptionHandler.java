package com.billing.invoice.advice.handler;

import com.billing.invoice.advice.dto.ErrorMessageDTO;
import com.billing.invoice.advice.dto.ErrorResponseDTO;
import com.billing.invoice.advice.exceptions.InternalErrorException;
import com.billing.invoice.advice.exceptions.InvoiceGenerationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvoiceGenerationException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvoiceGenerationException(InvoiceGenerationException e) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    @ExceptionHandler(InternalErrorException.class)
    public ResponseEntity<ErrorResponseDTO> handleInternalErrorException(InternalErrorException e) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    private ResponseEntity<ErrorResponseDTO> buildResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ErrorResponseDTO(
                        status.value(),
                        message,
                        LocalDateTime.now(),
                        List.of(new ErrorMessageDTO("Error", message))
                ));
    }
}
