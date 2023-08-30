package org.shaktifdn.registration.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ExternalServiceDependencyFailure extends RuntimeException {

    HttpStatus httpStatus;
    String message;
}
