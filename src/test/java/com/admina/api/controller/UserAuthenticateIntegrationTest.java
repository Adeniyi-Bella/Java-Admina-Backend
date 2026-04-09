package com.admina.api.controller;

import com.admina.api.exceptions.AppExceptions;
import com.admina.api.security.AuthenticatedPrincipal;
import com.admina.api.service.auth.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the User Authentication endpoint (/api/v1/users/authenticate).
 * 
 * These tests verify the full request-response cycle including:
 * 
 *   MockMvc for HTTP simulation
 *   AuthService mocking for JWT principal extraction
 *   Database interactions via JdbcTemplate
 *   Rate limiting disabled for test isolation
 *   RabbitMQ listeners disabled
 * 
 * Each test uses unique, auto-generated data (UUID suffixes) and cleans up the database after execution.
 * Tests cover happy path (user creation + reuse), unauthorized, and server error scenarios.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "app.ratelimit.enabled=false",
        "app.security.entra.validate-on-startup=false",
        "app.security.entra.tenant-id=test-tenant",
        "app.security.entra.client-id=test-client",
        "app.security.entra.client-secret=test-secret",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.listener.direct.auto-startup=false"
})
class UserAuthenticateIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Mock the AuthService to control JWT principal extraction behavior during tests.
     * Allows simulating valid/invalid principals without real JWT tokens.
     */
    @MockitoBean
    private AuthService authService;

    private String testEmail;

    /**
     * Clean up test data from database after each test to ensure isolation.
     * Only deletes if testEmail was set during the test.
     */
    @AfterEach
    void cleanup() {
        if (testEmail != null) {
            jdbcTemplate.update("DELETE FROM users WHERE email = ?", testEmail);
        }
    }

    /**
     * Happy-path integration test:
     * 
     * <ol>
     * First call to /authenticate creates new user (201 Created)
     * Second call returns existing user (200 OK)
     * Database has exactly 1 row (no duplicates)
     * </ol>
     * 
     * Verifies idempotency, proper HTTP status codes, JSON response structure,
     * and database persistence logic.
     */
    @Test
    void authenticate_firstCallCreatesUser_secondCallReturnsExistingUser_withoutDuplicateRows() throws Exception {
        // Generate unique test data using UUID suffix to avoid collisions
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        testEmail = "it-user-" + suffix + "@example.com";
        String testOid = "oid-" + suffix;
        String username = "it-user-" + suffix;

        // Mock successful principal extraction from JWT
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.builder()
                .oid(testOid)
                .email(testEmail)
                .username(username)
                .build();
        when(authService.extractPrincipal(nullable(Jwt.class))).thenReturn(principal);

        // First call: expect user creation
        mockMvc.perform(post("/api/v1/users/authenticate"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.user.email", is(testEmail)))
                .andExpect(jsonPath("$.data.user.oid", is(testOid)));

        // Verify exactly 1 user created in DB
        Integer afterFirst = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ?",
                Integer.class,
                testEmail);
        Assertions.assertEquals(1, afterFirst);

        // Second call: expect existing user return (no recreation)
        mockMvc.perform(post("/api/v1/users/authenticate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.user.email", is(testEmail)))
                .andExpect(jsonPath("$.data.user.oid", is(testOid)));

        // Verify still exactly 1 row (idempotent)
        Integer afterSecond = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ?",
                Integer.class,
                testEmail);
        Assertions.assertEquals(1, afterSecond);
    }

    /**
     * Unauthorized error path test:
     * 
     * Simulates AuthService failing to extract principal (e.g., invalid JWT).
     * Expects proper 401 Unauthorized response with standard error envelope.
     */
    @Test
    void authenticate_whenPrincipalExtractionFails_returnsUnauthorized() throws Exception {
        // Mock AuthService to throw UnauthorizedException
        when(authService.extractPrincipal(nullable(Jwt.class)))
                .thenThrow(new AppExceptions.UnauthorizedException("Authentication failed"));

        mockMvc.perform(post("/api/v1/users/authenticate"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    /**
     * Server error path test:
     * 
     * Simulates unexpected exception during principal extraction.
     * Expects proper 500 Internal Server Error with standard error envelope.
     */
    @Test
    void authenticate_whenUnexpectedExceptionOccurs_returnsInternalServerError() throws Exception {
        // Mock AuthService to throw unexpected RuntimeException
        when(authService.extractPrincipal(nullable(Jwt.class)))
                .thenThrow(new RuntimeException("unexpected test failure"));

        mockMvc.perform(post("/api/v1/users/authenticate"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)));
    }
}
