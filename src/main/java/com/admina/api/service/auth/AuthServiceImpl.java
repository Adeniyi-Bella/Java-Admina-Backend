package com.admina.api.service.auth;

import com.admina.api.security.AuthenticatedPrincipal;
import com.admina.api.security.PrincipalValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String CLAIM_OID = "oid";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_PREFERRED_USERNAME = "preferred_username";
    private static final String CLAIM_UNIQUE_NAME = "unique_name";
    private static final String CLAIM_UPN = "upn";
    private static final String CLAIM_NAME = "name";

    private final PrincipalValidator principalValidator;

    @Override
    public AuthenticatedPrincipal extractPrincipal(Jwt jwt) {
        String oid = jwt.getClaimAsString(CLAIM_OID);
        String email = firstNonBlank(
            jwt.getClaimAsString(CLAIM_EMAIL),
            jwt.getClaimAsString(CLAIM_PREFERRED_USERNAME),
            jwt.getClaimAsString(CLAIM_UNIQUE_NAME),
            jwt.getClaimAsString(CLAIM_UPN)
        );
        String username = firstNonBlank(
            jwt.getClaimAsString(CLAIM_PREFERRED_USERNAME),
            jwt.getClaimAsString(CLAIM_UNIQUE_NAME),
            jwt.getClaimAsString(CLAIM_UPN),
            jwt.getClaimAsString(CLAIM_NAME)
        );

        AuthenticatedPrincipal principal = AuthenticatedPrincipal.builder()
            .oid(oid)
            .email(email)
            .username(username)
            .build();

        principalValidator.validate(principal);
        return principal;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
