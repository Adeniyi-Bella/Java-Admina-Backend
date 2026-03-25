package com.admina.api.service.user;

import com.admina.api.dto.user.UserDto;
import com.admina.api.model.User;
import com.admina.api.security.AuthenticatedPrincipal;

public interface UserService {
    UserDto authenticate(AuthenticatedPrincipal principal);
    User getExistingUserByEmail(String email);
}
