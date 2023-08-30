package org.shaktifdn.registration.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ShaktiWebClientException extends AbstractException {

    public ShaktiWebClientException(String message) {
        super(message);
    }

}
