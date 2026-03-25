package com.admina.api.service.auth;

import com.admina.api.security.AuthenticatedPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

public interface AuthService {
    AuthenticatedPrincipal extractPrincipal(Jwt jwt);
}
