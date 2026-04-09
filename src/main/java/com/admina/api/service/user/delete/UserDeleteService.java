package com.admina.api.service.user.delete;

import com.admina.api.security.AuthenticatedPrincipal;

public interface UserDeleteService {
    void deleteCurrentUser(AuthenticatedPrincipal principal);
}
