package org.shaktifdn.registration.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictRecordsException extends AbstractException {

    public ConflictRecordsException(String exception) {
        super(exception);
    }
}
