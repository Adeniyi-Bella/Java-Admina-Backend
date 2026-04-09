package com.admina.api.service.user.delete;

import com.admina.api.exceptions.AppExceptions;
import com.admina.api.repository.UserRepository;
import com.admina.api.security.AuthenticatedPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDeleteServiceImpl implements UserDeleteService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void deleteCurrentUser(AuthenticatedPrincipal principal) {
        int deleted = userRepository.deleteByEmail(principal.getEmail());
        if (deleted == 0) {
            throw new AppExceptions.ResourceNotFoundException("User could not be found");
        }
    }
}
