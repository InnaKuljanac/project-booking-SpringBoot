package se.inna.bokningssystem.exception;

public class DuplicateUserException extends RuntimeException {

    public DuplicateUserException(String message) {
        super(message);
    }
}
