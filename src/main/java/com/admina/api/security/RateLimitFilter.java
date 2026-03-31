package com.admina.api.security;

import com.admina.api.config.AppRateLimitProperties;
import com.admina.api.dto.redis.RateLimitResult;
import com.admina.api.exceptions.HttpErrorResponder;
import com.admina.api.service.redis.RedisService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String HEADER_LIMIT = "X-RateLimit-Limit";
    private static final String HEADER_REMAINING = "X-RateLimit-Remaining";
    private static final String HEADER_RESET = "X-RateLimit-Reset";

    private final RedisService redisService;
    private final AppRateLimitProperties properties;
    private final HttpErrorResponder httpErrorResponder;

    public RateLimitFilter(
            RedisService redisService,
            AppRateLimitProperties properties,
            HttpErrorResponder httpErrorResponder) {
        this.redisService = redisService;
        this.properties = properties;
        this.httpErrorResponder = httpErrorResponder;
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
            FilterChain filterChain) throws ServletException, IOException {

        if (!properties.enabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1. Safely grab the IP (Backed by server.forward-headers-strategy=framework)
        String ip = request.getRemoteAddr();
        String key = "rl:ip:" + ip;
        int limit = properties.requestsPerMinute();

        // 2. Check Redis
        RateLimitResult result;
        try {
            result = redisService.checkRateLimit(
                    key,
                    limit,
                    Duration.ofSeconds(properties.windowSeconds()));
        } catch (Exception e) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Set Headers
        response.setHeader(HEADER_LIMIT, String.valueOf(result.limit()));
        response.setHeader(HEADER_REMAINING, String.valueOf(result.remaining()));
        response.setHeader(HEADER_RESET, String.valueOf(result.resetSeconds()));

        // 4. Reject if over limit
        if (!result.allowed()) {
            httpErrorResponder.write(response, 429, "Too many requests");
            return;
        }

        // 5. Proceed to Authentication
        filterChain.doFilter(request, response);
    }
}
