package com.admina.api.service.user;

import com.admina.api.dto.user.UserDto;
import com.admina.api.model.user.User;

import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getOid(),
                user.getUsername(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getPlanLimitCurrent(),
                user.getPlan(),
                user.getStripeCustomerId());
    }
}
