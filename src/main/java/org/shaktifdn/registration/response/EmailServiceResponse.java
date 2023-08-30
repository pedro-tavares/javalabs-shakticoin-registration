package org.shaktifdn.registration.response;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@Data
@Builder
@Jacksonized
public class EmailServiceResponse {

    private int code;
    private EmailServiceResponseStatus status;
    private String message;
    private Map<String, Object> payload;

    public enum EmailServiceResponseStatus {
        COMPLETED, FAILED, PROCESSING, EXPIRED
    }
}

