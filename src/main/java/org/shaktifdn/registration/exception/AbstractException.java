package org.shaktifdn.registration.exception;

public class AbstractException extends RuntimeException {
    public AbstractException(String exception) {
        super(exception);
    }

    public AbstractException(String exception, Throwable throwable) {
        super(exception, throwable);
    }
}
