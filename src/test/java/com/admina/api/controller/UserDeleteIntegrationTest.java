package com.admina.api.controller;

import com.admina.api.config.rabbit.UserDeleteRabbitConfig;
import com.admina.api.security.AuthenticatedPrincipal;
import com.admina.api.service.auth.AuthService;
import com.admina.api.user.events.UserDeletedEvent;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class UserDeleteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Test
    void deleteCurrentUser_deletesUserAndCascadeDeletesRelatedRows() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UUID userId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        String email = "it-delete-" + suffix + "@example.com";
        String stripeSubId = "sub_" + suffix;
        String stripeCustomerId = "cus_" + suffix;

        jdbcTemplate.update(
                "INSERT INTO users (id, email, oid, username, role, plan, plan_limit_max, plan_limit_current, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                userId,
                email,
                "oid-" + suffix,
                "user-" + suffix,
                "ROLE_USER",
                "FREE",
                2,
                2);

        jdbcTemplate.update(
                "INSERT INTO documents (id, user_id, target_language, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())",
                docId,
                userId,
                "en");

        jdbcTemplate.update(
                "INSERT INTO action_plan_tasks (id, document_id, user_id, title, due_date, completed, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                taskId,
                docId,
                userId,
                "Task title",
                LocalDate.now(),
                false);

        Instant periodStart = Instant.now();
        Instant periodEnd = periodStart.plusSeconds(86400);
        jdbcTemplate.update(
                "INSERT INTO subscriptions (id, user_id, stripe_subscription_id, stripe_customer_id, status, plan, current_period_start, current_period_end, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                subscriptionId,
                userId,
                stripeSubId,
                stripeCustomerId,
                "active",
                "PREMIUM",
                Timestamp.from(periodStart),
                Timestamp.from(periodEnd));

        AuthenticatedPrincipal principal = AuthenticatedPrincipal.builder()
                .oid("oid-" + suffix)
                .email(email)
                .username("user-" + suffix)
                .build();
        when(authService.extractPrincipal(nullable(Jwt.class))).thenReturn(principal);

        mockMvc.perform(delete("/api/v1/users"))
                .andExpect(status().isNoContent());

        ArgumentCaptor<UserDeletedEvent> eventCaptor = ArgumentCaptor.forClass(UserDeletedEvent.class);
        verify(rabbitTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq(UserDeleteRabbitConfig.USER_DELETE_EXCHANGE),
                org.mockito.ArgumentMatchers.eq(UserDeleteRabbitConfig.USER_DELETE_ROUTING_KEY),
                eventCaptor.capture());
        UserDeletedEvent publishedEvent = eventCaptor.getValue();
        Assertions.assertEquals("oid-" + suffix, publishedEvent.userOid());
        Assertions.assertEquals(email, publishedEvent.email());

        Integer userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ?",
                Integer.class,
                userId);
        Integer documentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM documents WHERE user_id = ?",
                Integer.class,
                userId);
        Integer taskCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM action_plan_tasks WHERE user_id = ?",
                Integer.class,
                userId);
        Integer subscriptionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM subscriptions WHERE user_id = ?",
                Integer.class,
                userId);

        Assertions.assertEquals(0, userCount);
        Assertions.assertEquals(0, documentCount);
        Assertions.assertEquals(0, taskCount);
        Assertions.assertEquals(0, subscriptionCount);
    }

    @Test
    void deleteCurrentUser_whenUserDoesNotExist_returnsNotFound() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.builder()
                .oid("oid-missing-" + suffix)
                .email("missing-" + suffix + "@example.com")
                .username("missing-user-" + suffix)
                .build();
        when(authService.extractPrincipal(nullable(Jwt.class))).thenReturn(principal);

        mockMvc.perform(delete("/api/v1/users"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.message", is("User could not be found")));
        verify(rabbitTemplate, never()).convertAndSend(
                org.mockito.ArgumentMatchers.eq(UserDeleteRabbitConfig.USER_DELETE_EXCHANGE),
                org.mockito.ArgumentMatchers.eq(UserDeleteRabbitConfig.USER_DELETE_ROUTING_KEY),
                org.mockito.ArgumentMatchers.any(UserDeletedEvent.class));
    }
}
