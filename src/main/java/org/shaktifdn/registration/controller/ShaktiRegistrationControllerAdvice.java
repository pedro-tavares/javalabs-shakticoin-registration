package org.shaktifdn.registration.controller;

import lombok.extern.slf4j.Slf4j;
import org.shaktifdn.registration.exception.*;
import org.shaktifdn.registration.response.ShaktiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebInputException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.shaktifdn.registration.util.Utils.PLEASE_TRY_AGAIN_LATER;

@ControllerAdvice
@Slf4j
public class ShaktiRegistrationControllerAdvice {

    @SuppressWarnings("rawtypes")
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ShaktiResponse> handleServerWebInputException(BadRequestException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ShaktiResponse
                        .builder()
                        .status(false)
                        .message(ex.getMessage())
                        .build()
                );
    }

    @ExceptionHandler(UserAlreadyRegisteredException.class)
    public ResponseEntity<ShaktiResponse> handleUserAlreadyRegisteredException(UserAlreadyRegisteredException ex) {
        log.error("error occurred", ex);
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ShaktiResponse
                        .builder()
                        .status(false)
                        .message(ex.getMessage())
                        .build()
                );
    }


    @SuppressWarnings("rawtypes")
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ShaktiResponse> handle(LockedException ex) {
        return ResponseEntity
                .status(HttpStatus.LOCKED)
                .body(ShaktiResponse
                        .builder()
                        .status(false)
                        .message(ex.getMessage())
                        .build()
                );
    }

    @ExceptionHandler(DisposableEmailException.class)
    public ResponseEntity<ShaktiResponse> handle(DisposableEmailException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_ACCEPTABLE)
                .body(ShaktiResponse
                        .builder()
                        .status(false)
                        .message(ex.getMessage())
                        .build()
                );
    }

    @ExceptionHandler(TooManyRequestException.class)
    public ResponseEntity<ShaktiResponse> handle(TooManyRequestException ex) {
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ShaktiResponse
                        .builder()
                        .status(false)
                        .message(ex.getMessage())
                        .build()
                );
    }

    @SuppressWarnings("rawtypes")
    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ShaktiResponse> handleServerWebInputException(ServerWebInputException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ShaktiResponse
                        .builder()
                        .status(false)
                        .message(ex.getMessage())
                        .build()
                );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ShaktiResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<String> details = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            details.add(error.getField() + " failed with value :" + error.getRejectedValue() + " due to: " + error.getDefaultMessage());
        }
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ShaktiResponse
                        .builder()
                        .status(false)
                        .message("Request Input Validation Failed: " + details)
                        .build()
                );
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ShaktiResponse> handleWebExchangeBindException(WebExchangeBindException ex) {
        List<String> details = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            details.add(error.getDefaultMessage());
        }
        return ResponseEntity
                .status(ex.getStatus())
                .body(ShaktiResponse
                        .builder()
                        .status(false)
                        .message(String.join(",", details))
                        .build()
                );
    }


    @SuppressWarnings("rawtypes")
    @ExceptionHandler(ConflictRecordsException.class)
    public ResponseEntity<ShaktiResponse> handleServerWebInputException(ConflictRecordsException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ShaktiResponse
                        .builder()
                        .status(false)
                        .message(ex.getMessage())
                        .build()
                );
    }

    @SuppressWarnings("rawtypes")
    @ExceptionHandler(RecordNotFoundException.class)
    public ResponseEntity<ShaktiResponse> handleServerWebInputException(RecordNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ShaktiResponse
                        .builder()
                        .status(false)
                        .message(ex.getMessage())
                        .build()
                );
    }

    @SuppressWarnings("rawtypes")
    @ExceptionHandler(ShaktiWebClientException.class)
    public ResponseEntity<ShaktiResponse> handleServerWebInputException(ShaktiWebClientException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ShaktiResponse
                        .builder()
                        .status(false)
                        .message(ex.getMessage())
                        .build()
                );
    }

    @SuppressWarnings("rawtypes")
    @ExceptionHandler(AbstractException.class)
    public ResponseEntity<ShaktiResponse> handleException(AbstractException ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ShaktiResponse
                        .builder()
                        .status(false)
                        .message("Error while processing the request please try again")
                        .build()
                );
    }

    @SuppressWarnings("rawtypes")
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ShaktiResponse> handleException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ShaktiResponse
                        .builder()
                        .status(false)
                        .message("Error while processing the request please try again")
                        .build()
                );
    }

    @SuppressWarnings("rawtypes")
    @ExceptionHandler(value = SelfyIdBadRequestException.class)
    public ResponseEntity<ShaktiResponse> selfyIdBadRequestException(SelfyIdBadRequestException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ShaktiResponse
                        .builder()
                        .status(false)
                        .message(e.getMessage())
                        .build()
                );
    }

    @SuppressWarnings("rawtypes")
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ShaktiResponse> handleWebClientResponseException(WebClientResponseException ex) {
        return ResponseEntity
                .status(ex.getRawStatusCode())
                .body(ShaktiResponse
                        .builder()
                        .status(false)
                        .message(ex.getMessage())
                        .build()
                );
    }

    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ShaktiResponse> handleTimeOutException(TimeoutException ex) {
        return ResponseEntity
                .status(HttpStatus.GATEWAY_TIMEOUT)
                .body(ShaktiResponse
                        .builder()
                        .status(false)
                        .message(PLEASE_TRY_AGAIN_LATER)
                        .build()
                );
    }

    @ExceptionHandler(ExternalServiceDependencyFailure.class)
    public ResponseEntity<ShaktiResponse> handleExternalServiceDependencyFailure(ExternalServiceDependencyFailure ex) {
        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(ShaktiResponse
                        .builder()
                        .status(false)
                        .message(ex.getMessage())
                        .build()
                );
    }

    @SuppressWarnings("rawtypes")
    @ExceptionHandler(WalletAlreadyExistsException.class)
    public ResponseEntity<ShaktiResponse> handle(WalletAlreadyExistsException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ShaktiResponse
                        .builder()
                        .status(false)
                        .message("You already have a wallet")
                        .build()
                );
    }

    @SuppressWarnings("rawtypes")
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ShaktiResponse> handle(UnauthorizedException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ShaktiResponse
                        .builder()
                        .status(false)
                        .message(ex.getMessage())
                        .build()
                );
    }

}
