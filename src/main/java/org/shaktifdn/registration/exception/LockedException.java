package org.shaktifdn.registration.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.LOCKED)
public class LockedException extends AbstractException {

    public LockedException(String exception) {
        super(exception);
    }

}
