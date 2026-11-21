package ru.mirea.appliancerepair.backend.error;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}
