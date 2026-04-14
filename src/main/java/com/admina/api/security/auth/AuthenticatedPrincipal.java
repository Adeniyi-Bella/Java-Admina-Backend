package com.admina.api.security.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthenticatedPrincipal {

    @NotBlank(message = "oid must not be blank")
    private final String oid;

    @NotBlank(message = "email must not be blank")
    @Email(message = "email must be valid")
    private final String email;

    @NotBlank(message = "username must not be blank")
    private final String username;
}
