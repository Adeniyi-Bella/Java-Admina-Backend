package com.admina.api.security.auth;

import org.springframework.security.oauth2.jwt.Jwt;

public interface AuthService {
    AuthenticatedPrincipal extractPrincipal(Jwt jwt);
}
