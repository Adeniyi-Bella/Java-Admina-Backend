package com.admina.api.dto.document;

import java.util.UUID;

public record DocumentJobResponse(UUID docId, String status) {
}
