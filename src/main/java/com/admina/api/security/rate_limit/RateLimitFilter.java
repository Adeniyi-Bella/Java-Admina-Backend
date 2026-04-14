package com.admina.api.security.rate_limit;

import com.admina.api.config.properties.AppRateLimitProperties;
import com.admina.api.exceptions.HttpErrorResponder;
import com.admina.api.redis.RedisKeys;
import com.admina.api.redis.RedisService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String HEADER_LIMIT = "X-RateLimit-Limit";
    private static final String HEADER_REMAINING = "X-RateLimit-Remaining";
    private static final String HEADER_RESET = "X-RateLimit-Reset";

    private final RedisService redisService;
    private final AppRateLimitProperties properties;
    private final HttpErrorResponder httpErrorResponder;
    private final Duration window;

    public RateLimitFilter(
            RedisService redisService,
            AppRateLimitProperties properties,
            HttpErrorResponder httpErrorResponder) {
        this.redisService = redisService;
        this.properties = properties;
        this.httpErrorResponder = httpErrorResponder;
        this.window = Duration.ofSeconds(properties.windowSeconds());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return properties.excludePaths().stream().anyMatch(path::endsWith);
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
        String key = RedisKeys.rateLimitIp(ip);
        int limit = properties.requestsPerMinute();

        // 2. Check Redis
        RateLimitResult result;
        try {
            result = redisService.checkRateLimit(
                    key,
                    limit,
                    window);
        } catch (Exception e) {
            log.warn("Rate limit check failed for ip={}, failing open", ip, e);
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Set Headers
        response.setHeader(HEADER_LIMIT, String.valueOf(result.limit()));
        response.setHeader(HEADER_REMAINING, String.valueOf(result.remaining()));
        response.setHeader(HEADER_RESET, String.valueOf(result.resetSeconds()));

        // 4. Reject if over limit
        if (!result.allowed()) {
            response.setHeader("Retry-After", String.valueOf(result.resetSeconds()));
            httpErrorResponder.write(response, 429, "Too many requests");
            return;
        }

        // 5. Proceed to Authentication
        filterChain.doFilter(request, response);
    }
}
