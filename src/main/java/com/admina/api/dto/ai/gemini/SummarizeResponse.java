package com.admina.api.dto.ai.gemini;

import java.util.List;

import com.admina.api.model.document.ActionPlanItem;

public record SummarizeResponse(
        String title,
        String sender,
        String receivedDate,
        String summary,
        List<ActionPlanItem> actionPlan,
        List<GeminiActionPlanTaskDto> actionPlans) {
}