package com.admina.api.dto.ai.gemini;

import java.time.LocalDate;

public record GeminiActionPlanTaskDto(String title, LocalDate dueDate, boolean completed, String location) {}