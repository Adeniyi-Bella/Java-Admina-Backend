package com.admina.api.service.user;

import com.admina.api.dto.user.UserDto;

import com.admina.api.dto.user.UserAuthenticationResult;
import com.admina.api.security.AuthenticatedPrincipal;

public interface UserService {
    UserAuthenticationResult authenticate(AuthenticatedPrincipal principal);
    UserDto getExistingUserByEmail(String email);
    void updateStripeCustomerId(String email, String stripeCustomerId);
}
