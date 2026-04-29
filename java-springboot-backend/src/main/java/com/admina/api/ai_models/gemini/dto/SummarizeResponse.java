package com.admina.api.ai_models.gemini.dto;

import java.util.List;

import com.admina.api.document.model.ActionPlanItem;

public record SummarizeResponse(
        String title,
        String sender,
        String receivedDate,
        String summary,
        List<ActionPlanItem> actionPlan,
        List<GeminiActionPlanTaskDto> actionPlans) {
}