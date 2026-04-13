package com.admina.api.user.service;

import com.admina.api.user.dto.UserDto;
import com.admina.api.user.model.User;

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
                user.getDocumentsUsed(),
                user.getPlan(),
                user.getStripeCustomerId());
    }
}
