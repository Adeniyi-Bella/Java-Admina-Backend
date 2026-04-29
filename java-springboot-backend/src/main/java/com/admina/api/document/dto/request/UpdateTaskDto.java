package com.admina.api.document.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateTaskDto {
    public record AddTaskToDocument(
            @NotBlank(message = "Task title is required") @Size(max = 1000, message = "Task title must be at most 1000 characters") @Pattern(regexp = "^[\\p{L}\\p{N}\\s\\-_.,!?()']+$", message = "Task title contains invalid characters.") String title,

            LocalDate dueDate,

            @Size(max = 500, message = "Task location must be at most 500 characters") @Pattern(regexp = "^[\\p{L}\\p{N}\\s\\-_.,!?()']*$", message = "Location contains invalid characters.") String location) {
    }

    public record UpdateExistingTask(
        @Size(max = 1000, message = "Task title must be at most 1000 characters") @Pattern(regexp = "^[\\p{L}\\p{N}\\s\\-_.,!?()']*$", message = "Title contains invalid characters.") String title,

        LocalDate dueDate,

        Boolean completed,

        @Size(max = 500, message = "Task location must be at most 500 characters") @Pattern(regexp = "^[\\p{L}\\p{N}\\s\\-_.,!?()']*$", message = "Location contains invalid characters.") String location) {

    public boolean hasUpdates() {
        return title != null || dueDate != null || completed != null || location != null;
    }
}
}
