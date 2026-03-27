package com.admina.api.security;

import com.admina.api.config.AppRateLimitProperties;
import com.admina.api.dto.redis.RateLimitResult;
import com.admina.api.exceptions.AppExceptions;
import com.admina.api.service.redis.RedisService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.time.Duration;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String HEADER_LIMIT = "X-RateLimit-Limit";
    private static final String HEADER_REMAINING = "X-RateLimit-Remaining";
    private static final String HEADER_RESET = "X-RateLimit-Reset";

    private final RedisService redisService;
    private final AppRateLimitProperties properties;
    private final HandlerExceptionResolver exceptionResolver;

    public RateLimitFilter(
        RedisService redisService,
        AppRateLimitProperties properties,
        HandlerExceptionResolver exceptionResolver
    ) {
        this.redisService = redisService;
        this.properties = properties;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "/actuator/health".equals(path) || "/actuator/info".equals(path);
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        if (!properties.enabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = authentication != null
            && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken);

        String key;
        int limit;
        if (authenticated) {
            String userId = resolveUserId(authentication);
            key = "rl:user:" + userId;
            limit = properties.authenticatedPerMinute();
        } else {
            String ip = resolveClientIp(request);
            key = "rl:ip:" + ip;
            limit = properties.unauthenticatedPerMinute();
        }

        RateLimitResult result = redisService.checkRateLimit(
            key,
            limit,
            Duration.ofSeconds(properties.windowSeconds())
        );

        response.setHeader(HEADER_LIMIT, String.valueOf(result.limit()));
        response.setHeader(HEADER_REMAINING, String.valueOf(result.remaining()));
        response.setHeader(HEADER_RESET, String.valueOf(result.resetSeconds()));

        if (!result.allowed()) {
            exceptionResolver.resolveException(
                request,
                response,
                null,
                new AppExceptions.TooManyRequestsException("Too many requests")
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            String oid = jwt.getClaimAsString("oid");
            if (oid != null && !oid.isBlank()) {
                return oid;
            }
            String sub = jwt.getSubject();
            if (sub != null && !sub.isBlank()) {
                return sub;
            }
        }
        return authentication.getName();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

}
