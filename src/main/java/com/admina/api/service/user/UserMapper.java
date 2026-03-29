package com.admina.api.service.user;

import com.admina.api.dto.user.UserDto;
import com.admina.api.model.user.User;

import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .oid(user.getOid())
                .username(user.getUsername())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .planLimitCurrent(user.getPlanLimitCurrent())
                .build();
    }

}
