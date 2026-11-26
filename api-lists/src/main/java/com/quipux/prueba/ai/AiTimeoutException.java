package com.quipux.prueba.ai;

public class AiTimeoutException extends RuntimeException {
    public AiTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public AiTimeoutException(String message) {
        super(message);
    }
}
