package se.inna.bokningssystem.exception;

public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(Long id) {
        super("Booking med id " + id + " hittades inte");
    }
}