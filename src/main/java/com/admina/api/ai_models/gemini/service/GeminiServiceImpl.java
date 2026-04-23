package com.admina.api.ai_models.gemini.service;

import com.admina.api.ai_models.gemini.dto.GeminiActionPlanTaskDto;
import com.admina.api.ai_models.gemini.dto.SummarizeResponse;
import com.admina.api.ai_models.gemini.dto.TranslateResponse;
import com.admina.api.config.properties.DocumentProcessingProperties;
import com.admina.api.document.model.ActionPlanItem;
import com.admina.api.exceptions.AppExceptions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GeminiServiceImpl implements GeminiService {

    private static final String MODEL = "gemini-2.5-flash";

    private final Client client;
    private final GeminiPrompt prompt;
    private final ObjectMapper objectMapper;
    private final GenerateContentConfig jsonConfig;

    public GeminiServiceImpl(DocumentProcessingProperties appDocumentProperties, ObjectMapper objectMapper) {
        this.client = Client.builder()
                .apiKey(appDocumentProperties.geminiApiKey())
                .build();
        this.prompt = new GeminiPrompt();
        this.objectMapper = objectMapper;
        this.jsonConfig = GenerateContentConfig.builder()
                .responseMimeType("application/json")
                .build();
    }

    @Override
    public TranslateResponse translateDocument(byte[] fileBytes, String mimeType, String targetLanguage) {
        String userPrompt = prompt.buildTranslatePrompt(targetLanguage);
        Content content = Content.fromParts(
                Part.fromText(userPrompt),
                Part.fromBytes(fileBytes, mimeType));
        try {
            GenerateContentResponse response = client.models.generateContent(MODEL, content, jsonConfig);
            String responseText = response.text();
            if (responseText == null || responseText.isBlank()) {
                log.error("Gemini returned empty response for document translation");
                throw new AppExceptions.BadGatewayException(
                        "The translation service failed to generate a response. Please try again with a valid document.");
            }
            return parseTranslateResponse(responseText);
        } catch (AppExceptions.BadGatewayException | AppExceptions.GatewayTimeoutException ex) {
            throw ex;
        } catch (Exception ex) {
            handleGeminiError(ex, "document translation");
        }
        // unreachable but required by compiler
        throw new AppExceptions.BadGatewayException("Translation failed");
    }

    @Override
    public SummarizeResponse summarizeDocument(String translatedText, String targetLanguage) {
        String userPrompt = prompt.buildSummarizePrompt(translatedText, targetLanguage);
        try {
            GenerateContentResponse response = client.models.generateContent(MODEL, userPrompt, jsonConfig);
            String responseText = response.text();
            if (responseText == null || responseText.isBlank()) {
                log.error("Gemini returned empty response for document summarization");
                throw new AppExceptions.BadGatewayException(
                        "The summarization service failed to generate a response. Please try again with a valid document.");
            }
            return parseSummarizeResponse(responseText);
        } catch (AppExceptions.BadGatewayException | AppExceptions.GatewayTimeoutException ex) {
            throw ex;
        } catch (Exception ex) {
            handleGeminiError(ex, "document summarization");
        }
        throw new AppExceptions.BadGatewayException("Summarization failed");
    }

    @Override
    public String generateChatbotResponse(
            String translatedText,
            List<GeminiService.ChatHistoryMessage> history,
            String userPrompt) {
        try {
            String systemInstructionText = prompt.buildChatbotSystemInstruction(translatedText, userPrompt);
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .systemInstruction(Content.fromParts(Part.fromText(systemInstructionText)))
                    .build();

            List<Content> contents = new ArrayList<>();
            for (GeminiService.ChatHistoryMessage turn : history) {
                if (turn == null || turn.content() == null || turn.content().isBlank()) {
                    continue;
                }
                String role = "AGENT".equalsIgnoreCase(turn.role()) ? "model" : "user";
                contents.add(Content.builder()
                        .role(role)
                        .parts(Part.fromText(turn.content()))
                        .build());
            }

            GenerateContentResponse response = client.models.generateContent(MODEL, contents, config);
            String responseText = response.text();
            if (responseText == null || responseText.isBlank()) {
                log.error("Gemini returned empty response for chatbot generation");
                throw new AppExceptions.BadGatewayException(
                        "The chatbot service failed to generate a response. Please try again.");
            }
            return responseText;
        } catch (Exception ex) {
            handleGeminiError(ex, "chatbot generation");
        }
        throw new AppExceptions.BadGatewayException("Chatbot generation failed");
    }

    private TranslateResponse parseTranslateResponse(String responseText) {
        try {
            String clean = cleanResponse(responseText);
            JsonNode node = objectMapper.readTree(clean);

            String translatedText = node.path("translatedText").asText("");
            Map<String, String> structuredText = new HashMap<>();
            JsonNode structuredNode = node.path("structuredTranslatedText");
            if (structuredNode.isObject()) {
                structuredNode.properties()
                        .forEach(entry -> structuredText.put(entry.getKey(), entry.getValue().asText("")));
            }
            return new TranslateResponse(translatedText, structuredText);
        } catch (Exception ex) {
            log.error("Failed to parse Gemini translate response", ex);
            throw new AppExceptions.BadGatewayException(
                    "The translation service returned an unreadable result. Please try again with a valid document.");
        }
    }

    private SummarizeResponse parseSummarizeResponse(String responseText) {
        try {
            String clean = cleanResponse(responseText);
            JsonNode node = objectMapper.readTree(clean);

            String title = node.path("title").asText("");
            String sender = node.path("sender").asText("");
            String receivedDate = node.path("receivedDate").asText("");
            String summary = node.path("summary").asText("");

            List<ActionPlanItem> actionPlan = new ArrayList<>();
            if (node.path("actionPlan").isArray()) {
                for (JsonNode item : node.path("actionPlan")) {
                    actionPlan.add(new ActionPlanItem(
                            item.path("title").asText(""),
                            item.path("reason").asText("")));
                }
            }

            List<GeminiActionPlanTaskDto> actionPlans = new ArrayList<>();
            if (node.path("actionPlans").isArray()) {
                for (JsonNode task : node.path("actionPlans")) {
                    LocalDate dueDate = parseDueDate(task.path("due_date").asText(""));
                    actionPlans.add(new GeminiActionPlanTaskDto(
                            task.path("title").asText(""),
                            dueDate,
                            task.path("completed").asBoolean(false),
                            task.path("location").asText("")));
                }
            }

            return new SummarizeResponse(title, sender, receivedDate, summary, actionPlan, actionPlans);
        } catch (Exception ex) {
            log.error("Failed to parse Gemini summarize response", ex);
            throw new AppExceptions.BadGatewayException(
                    "The summarization service returned an unreadable result. Please try again with a valid document.");
        }
    }

    private LocalDate parseDueDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (Exception ex) {
            log.warn("Invalid due date format '{}', defaulting to today", raw);
            return LocalDate.now();
        }
    }

    private String cleanResponse(String response) {
        String clean = response.trim();
        // Strip markdown code fences if present
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("```(?:json)?\\s*([\\s\\S]*?)\\s*```", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(clean);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        // Fallback: extract from first { to last }
        int firstBrace = clean.indexOf('{');
        int lastBrace = clean.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return clean.substring(firstBrace, lastBrace + 1);
        }
        return clean;
    }

    private void handleGeminiError(Exception error, String context) {
        String message = error.getMessage() != null ? error.getMessage()
                : "An unknown error occurred while processing the document.";

        if (message.contains("deadline exceeded") || message.contains("timeout")) {
            throw new AppExceptions.GatewayTimeoutException(
                    "The request took too long to process. Please try again with a smaller file.");
        }
        if (message.contains("fetch failed") || message.contains("network")) {
            throw new AppExceptions.BadGatewayException(
                    "The AI processing service is temporarily unreachable. Please try again later.");
        }
        throw new AppExceptions.BadGatewayException(message.replaceAll("^\\d{3}\\s*\\.\\s*", "").trim());
    }
}
