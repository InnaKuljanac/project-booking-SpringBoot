
package se.inna.bokningssystem.exception;

public class DuplicateTreatmentException extends RuntimeException {

    public DuplicateTreatmentException(String name) {
        super("En behandling med namnet '" + name + "' finns redan");
    }
}

