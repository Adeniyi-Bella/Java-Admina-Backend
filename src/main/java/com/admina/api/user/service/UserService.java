package com.admina.api.user.service;

import com.admina.api.security.AuthenticatedPrincipal;
import com.admina.api.user.dto.UserAuthenticationResult;
import com.admina.api.user.dto.UserDto;

public interface UserService {
    UserAuthenticationResult authenticate(AuthenticatedPrincipal principal);
    UserDto getExistingUserByEmail(String email);
    void updateStripeCustomerId(String email, String stripeCustomerId);
}
