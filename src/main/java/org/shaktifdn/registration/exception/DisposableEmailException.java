package org.shaktifdn.registration.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
public class DisposableEmailException extends AbstractException {

    public DisposableEmailException(String exception) {
        super(exception);
    }
}
