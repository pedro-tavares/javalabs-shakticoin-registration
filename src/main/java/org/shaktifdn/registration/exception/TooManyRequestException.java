package org.shaktifdn.registration.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Map;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
@Getter
public class TooManyRequestException extends AbstractException {

    private final Map<String, Object> payload;

    public TooManyRequestException(String exception, Map<String, Object> payload) {
        super(exception);
        this.payload = payload;
    }

}
