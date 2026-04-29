package com.admina.api.security;

import com.admina.api.redis.RedisService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class JwtBlacklistValidator implements OAuth2TokenValidator<Jwt> {

    private final RedisService redisService;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String tokenId = token.getClaim("uti");
        if (!StringUtils.hasText(tokenId)) {
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Missing token identifier", null));
        }
        if (redisService.isJwtBlacklisted(tokenId)) {
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Token has been revoked", null));
        }
        return OAuth2TokenValidatorResult.success();
    }
}
