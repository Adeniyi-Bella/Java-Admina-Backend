package com.admina.api.service.user;

import com.admina.api.dto.user.UserDto;
import com.admina.api.dto.user.UserWithDocumentsResponse;
import com.admina.api.security.AuthenticatedPrincipal;

public interface UserService {
    UserWithDocumentsResponse authenticate(AuthenticatedPrincipal principal);
    UserDto getExistingUserByEmail(String email);
}
