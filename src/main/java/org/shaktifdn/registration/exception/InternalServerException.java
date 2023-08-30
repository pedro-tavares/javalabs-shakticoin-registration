package org.shaktifdn.registration.exception;

public class InternalServerException extends AbstractException {

    public InternalServerException(String exception, Throwable throwable) {
        super(exception, throwable);
    }

    public InternalServerException(String exception) {
        super(exception);
    }

}
