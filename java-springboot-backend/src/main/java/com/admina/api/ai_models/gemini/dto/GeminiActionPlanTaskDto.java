package com.admina.api.ai_models.gemini.dto;

import java.time.LocalDate;

public record GeminiActionPlanTaskDto(String title, LocalDate dueDate, boolean completed, String location) {}