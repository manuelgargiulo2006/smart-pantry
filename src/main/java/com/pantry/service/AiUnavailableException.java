package com.pantry.service;

/** L'intelligenza artificiale non ha risposto o ha risposto in modo non valido. */
public class AiUnavailableException extends RuntimeException {

    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
