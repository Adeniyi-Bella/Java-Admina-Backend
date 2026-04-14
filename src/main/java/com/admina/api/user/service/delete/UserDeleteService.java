package com.admina.api.user.service.delete;

import com.admina.api.security.auth.AuthenticatedPrincipal;

public interface UserDeleteService {
    void deleteCurrentUser(AuthenticatedPrincipal principal);

    void disableUserInEntra(String userOid);
}
