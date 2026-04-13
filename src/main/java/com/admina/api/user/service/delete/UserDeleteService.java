package com.admina.api.user.service.delete;

import com.admina.api.security.AuthenticatedPrincipal;

public interface UserDeleteService {
    void deleteCurrentUser(AuthenticatedPrincipal principal);

    void disableUserInEntra(String userOid);
}
