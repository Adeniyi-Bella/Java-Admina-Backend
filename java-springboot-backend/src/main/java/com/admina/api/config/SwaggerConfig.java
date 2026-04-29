package com.admina.api.config;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.admina.api.exceptions.ResponseDtos;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class SwaggerConfig {
    private static final String APP_JSON = "application/json";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components())
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("Admina API")
                        .version("v1")
                        .description("This document provides API details for the admina app."));
    }

    @Bean
    public OpenApiCustomizer defaultResponseEnvelopeCustomizer() {
        return openApi -> {
            ensureSharedSchemas(openApi);
            ensureSharedResponses(openApi);

            if (openApi.getPaths() == null) {
                return;
            }

            openApi.getPaths().values()
                    .forEach(pathItem -> pathItem.readOperations().forEach(this::normalizeDeclaredResponsesOnly));
        };
    }

    private void ensureSharedSchemas(OpenAPI openApi) {
        Components components = openApi.getComponents();

        ResolvedSchema errorSchema = ModelConverters.getInstance()
                .readAllAsResolvedSchema(ResponseDtos.ErrorResponse.class);
        if (errorSchema.referencedSchemas != null) {
            errorSchema.referencedSchemas.forEach(components::addSchemas);
        }
        if (errorSchema.schema != null && errorSchema.schema.getName() != null) {
            components.addSchemas(errorSchema.schema.getName(), errorSchema.schema);
        }

        Schema<?> successEnvelope = new ObjectSchema()
                .addProperty("success", new BooleanSchema().example(true))
                .addProperty("data", new ObjectSchema().description("Endpoint-specific success payload"))
                .addProperty("error", new Schema<>().nullable(true).example(null))
                .addProperty("timestamp", new StringSchema().example("2026-04-04T10:00:00Z"));

        Schema<?> errorEnvelope = new ObjectSchema()
                .addProperty("success", new BooleanSchema().example(false))
                .addProperty("data", new Schema<>().nullable(true).example(null))
                .addProperty("error", new Schema<>().$ref("#/components/schemas/ErrorResponse"))
                .addProperty("timestamp", new StringSchema().example("2026-04-04T10:00:00Z"));

        components.addSchemas("SuccessEnvelope", successEnvelope);
        components.addSchemas("ErrorEnvelope", errorEnvelope);
    }

    private void ensureSharedResponses(OpenAPI openApi) {
        Components components = openApi.getComponents();
        Map<String, String> errorDescriptions = new LinkedHashMap<>();
        errorDescriptions.put("400", "Bad request");
        errorDescriptions.put("401", "Unauthorized");
        errorDescriptions.put("403", "Forbidden");
        errorDescriptions.put("404", "Not found");
        errorDescriptions.put("409", "Conflict");
        errorDescriptions.put("415", "Unsupported media type");
        errorDescriptions.put("429", "Too many requests");
        errorDescriptions.put("500", "Internal server error");
        errorDescriptions.put("503", "Service unavailable");

        components.addResponses("Standard200", buildResponse(
                "Success response",
                "#/components/schemas/SuccessEnvelope"));
        components.addResponses("Standard201", buildResponse(
                "Resource created",
                "#/components/schemas/SuccessEnvelope"));

        errorDescriptions.forEach((code, description) ->
                components.addResponses("Standard" + code, buildResponse(description, "#/components/schemas/ErrorEnvelope")));
    }

    private ApiResponse buildResponse(String description, String schemaRef) {
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(
                        APP_JSON,
                        new MediaType().schema(new Schema<>().$ref(schemaRef))));
    }

    private void normalizeDeclaredResponsesOnly(io.swagger.v3.oas.models.Operation operation) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            return;
        }

        Map<String, String> standardByCode = Map.ofEntries(
                Map.entry("200", "#/components/responses/Standard200"),
                Map.entry("201", "#/components/responses/Standard201"),
                Map.entry("400", "#/components/responses/Standard400"),
                Map.entry("401", "#/components/responses/Standard401"),
                Map.entry("403", "#/components/responses/Standard403"),
                Map.entry("404", "#/components/responses/Standard404"),
                Map.entry("409", "#/components/responses/Standard409"),
                Map.entry("415", "#/components/responses/Standard415"),
                Map.entry("429", "#/components/responses/Standard429"),
                Map.entry("500", "#/components/responses/Standard500"),
                Map.entry("503", "#/components/responses/Standard503"));

        responses.entrySet().forEach(entry -> {
            String code = entry.getKey();
            String targetRef = standardByCode.get(code);
            if (targetRef == null) {
                return;
            }
            forceStatusResponse(responses, code, targetRef);
        });
    }

    private void forceStatusResponse(ApiResponses responses, String statusCode, String responseRef) {
        ApiResponse existing = responses.get(statusCode);
        if (existing == null) return;
        existing.set$ref(responseRef);
        existing.setContent(null);
        existing.setHeaders(null);
        existing.setLinks(null);
        existing.setExtensions(null);
    }
}
