package org.shaktifdn.registration.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class RecordNotFoundException extends AbstractException {
    public RecordNotFoundException(String exception) {
        super(exception);
    }
}