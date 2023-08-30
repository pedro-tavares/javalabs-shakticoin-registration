package org.shaktifdn.registration.exception;

public class UnauthorizedException extends RuntimeException {

    private static final long serialVersionUID = -2710220100884473075L;

    public UnauthorizedException(String message) {
        super(message);
    }

}
