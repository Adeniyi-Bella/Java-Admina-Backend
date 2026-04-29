package com.admina.api.document.service;

import java.util.UUID;

import com.admina.api.document.dto.response.ChatJobResponse;
import com.admina.api.document.dto.response.ChatJobStatusResponse;
import com.admina.api.document.dto.request.ChatbotPromptRequest;
import com.admina.api.security.auth.AuthenticatedPrincipal;

public interface ChatbotService {
    ChatJobResponse createChatJob(
            AuthenticatedPrincipal principal,
            UUID docId,
            ChatbotPromptRequest request);

    ChatJobStatusResponse getChatJobStatus(
            AuthenticatedPrincipal principal,
            UUID docId,
            UUID chatbotPollingId);
}
