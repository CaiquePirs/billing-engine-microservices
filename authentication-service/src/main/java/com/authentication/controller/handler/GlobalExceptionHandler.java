package com.authentication.controller.handler;

import com.authentication.controller.advice.dto.ErrorMessageDTO;
import com.authentication.controller.advice.dto.ErrorResponseDTO;
import com.authentication.controller.advice.exceptions.AuthenticationRegisterFailException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = AuthenticationRegisterFailException.class)
    public ResponseEntity<ErrorResponseDTO> handleException(AuthenticationRegisterFailException e){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDTO(
                        HttpStatus.BAD_REQUEST.value(),
                        e.getMessage(),
                        LocalDateTime.now(),
                        List.of(new ErrorMessageDTO("Error", e.getMessage()))
                ));
    }
}
