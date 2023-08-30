package org.shaktifdn.registration.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for Spring Security.
 */
public final class SecurityUtils {
    private SecurityUtils() {
    }

    public static Mono<Authentication> getAuthentication() {
        return ReactiveSecurityContextHolder
                .getContext()
                .map(SecurityContext::getAuthentication);
    }

    public static List<GrantedAuthority> extractAuthorityFromClaims(Map<String, Object> claims) {
        return mapRolesToGrantedAuthorities(getRolesFromClaims(claims));
    }

    @SuppressWarnings("unchecked")
    private static Collection<String> getRolesFromClaims(Map<String, Object> claims) {
        return (Collection<String>) claims.getOrDefault("scope", new ArrayList<>());
    }

    private static List<GrantedAuthority> mapRolesToGrantedAuthorities(Collection<String> roles) {
        return roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }
}
