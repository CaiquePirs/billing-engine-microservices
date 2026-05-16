package com.authentication.controller.handler;

import com.authentication.controller.advice.dto.ErrorMessageDTO;
import com.authentication.controller.advice.dto.ErrorResponseDTO;
import com.authentication.controller.advice.exceptions.AuthLoginFailException;
import com.authentication.controller.advice.exceptions.AuthRegisterFailException;
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

    @ExceptionHandler(value = AuthRegisterFailException.class)
    public ResponseEntity<ErrorResponseDTO> handleRegisterException(AuthRegisterFailException e){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDTO(
                        HttpStatus.BAD_REQUEST.value(),
                        e.getMessage(),
                        LocalDateTime.now(),
                        List.of(new ErrorMessageDTO("Error", e.getMessage()))
                ));
    }

    @ExceptionHandler(value = AuthLoginFailException.class)
    public ResponseEntity<ErrorResponseDTO> handleLoginException(AuthLoginFailException e){
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponseDTO(
                        HttpStatus.UNAUTHORIZED.value(),
                        "Email or password is incorrect",
                        LocalDateTime.now(),
                        List.of(new ErrorMessageDTO("Error", e.getMessage()))
                ));
    }
}
