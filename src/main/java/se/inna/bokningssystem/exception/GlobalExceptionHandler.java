package se.inna.bokningssystem.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;


import java.util.Objects;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity
                .status(400)
                .body(new ErrorResponse(400, message));
    }

    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateUser(
            DuplicateUserException ex) {

        return ResponseEntity
                .status(409)
                .body(new ErrorResponse(409, ex.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UserNotFoundException ex) {

        return ResponseEntity
                .status(404)
                .body(new ErrorResponse(404, ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(401, ex.getMessage()));
    }

    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBookingNotFound(
            BookingNotFoundException ex) {

        return ResponseEntity
                .status(404)
                .body(new ErrorResponse(404, ex.getMessage()));
    }

    @ExceptionHandler(TreatmentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTreatmentNotFound(
            TreatmentNotFoundException ex) {

        return ResponseEntity
                .status(404)
                .body(new ErrorResponse(404, ex.getMessage()));
    }

    @ExceptionHandler(BookingConflictException.class)
    public ResponseEntity<ErrorResponse> handleBookingConflict(
            BookingConflictException ex) {

        return ResponseEntity
                .status(409)
                .body(new ErrorResponse(409, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {

        return ResponseEntity
                .status(500)
                .body(new ErrorResponse(500, "Något gick fel"));
    }


    @ExceptionHandler(DuplicateTreatmentException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateTreatment(
            DuplicateTreatmentException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, ex.getMessage()));
    }





}