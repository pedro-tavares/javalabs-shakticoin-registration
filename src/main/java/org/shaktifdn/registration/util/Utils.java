package org.shaktifdn.registration.util;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.gluu.oxtrust.model.scim2.user.Email;
import org.gluu.oxtrust.model.scim2.user.PhoneNumber;
import org.gluu.oxtrust.model.scim2.user.UserResource;
import org.shaktifdn.registration.exception.ExternalServiceDependencyFailure;
import org.shaktifdn.registration.exception.ShaktiWebClientException;
import org.shaktifdn.registration.request.OnboardShaktiUserRequest;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.TimeoutException;

@Slf4j
public class Utils {

    public static final String PLEASE_TRY_AGAIN_LATER = "An error has been occurred during the request processing , please try again later.";

    private Utils() {
    }

    public static String encodeBase64(String username, String password) {
        return Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }

    public static UserResource createUserModel(OnboardShaktiUserRequest onboardShakti, String ipAddress) {

        List<Email> emails = new ArrayList<>();
        Email email = new Email();
        email.setPrimary(onboardShakti.isEmailVerified());
        email.setValue(onboardShakti.getEmail().toLowerCase());
        email.setType("work");
        emails.add(email);

        List<PhoneNumber> phoneNumbers = new ArrayList<>();
        PhoneNumber phoneNumber = new PhoneNumber();
        phoneNumber.setPrimary(onboardShakti.isMobileVerified());
        phoneNumber.setValue(onboardShakti.getMobileNo());
        phoneNumber.setType("work");
        phoneNumbers.add(phoneNumber);

        LinkedHashMap<String, Object> customAttribute = new LinkedHashMap<>();
        customAttribute.put("shaktiID", onboardShakti.getShaktiID());
        customAttribute.put("primaryMobileCountryCode", onboardShakti.getCountryCode());
        if (StringUtils.isNotBlank(ipAddress)) {
            customAttribute.put("IPAddress", ipAddress);
        }
        Set<String> schema = new HashSet<>();
        schema.add("urn:ietf:params:scim:schemas:core:2.0:User");
        schema.add("urn:ietf:params:scim:schemas:extension:gluu:2.0:User");


        UserResource userResource = new UserResource();
        userResource.setActive(true);
        userResource.setDisplayName(onboardShakti.getEmail());
        userResource.setEmails(List.of());
        userResource.setPassword(onboardShakti.getPassword());
        userResource.setEmails(emails);
        userResource.setPhoneNumbers(phoneNumbers);
        userResource.setUserName(onboardShakti.getEmail());
        userResource.setSchemas(schema);
        userResource.getCustomAttributes().put("urn:ietf:params:scim:schemas:extension:gluu:2.0:User", customAttribute);

        return userResource;

    }

    public static <T> Mono<T> handleExternalServiceCallException(Throwable throwable, String message) {
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException exception = (WebClientResponseException) throwable;
            if (exception.getStatusCode().is5xxServerError()) {
                log.error("An error during calling an external service with http status code response {}  and error message {} ", exception.getStatusCode(), exception.getMessage(), exception);
                return Mono.error(ExternalServiceDependencyFailure
                        .builder()
                        .httpStatus(exception.getStatusCode())
                        .message(PLEASE_TRY_AGAIN_LATER)
                        .build());
            }
        }
        if (throwable instanceof TimeoutException) {
            log.info("Time out exception is thrown away with message: {}", message);
            return Mono.error(throwable);
        }
        return Mono.error(new ShaktiWebClientException(message));

    }

}
