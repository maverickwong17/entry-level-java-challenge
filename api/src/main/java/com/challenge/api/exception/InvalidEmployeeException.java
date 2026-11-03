package com.challenge.api.exception;

import java.util.List;
import lombok.Getter;

@Getter
public class InvalidEmployeeException extends RuntimeException {

    private final List<String> errors;

    public InvalidEmployeeException(List<String> errors) {
        super("Invalid employee: " + String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }
}
