package com.admina.api.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlanType {

    FREE(2, 5),
    STANDARD(5, 20),
    PREMIUM(7, 50);

    private final int maxDocuments;
    private final int maxPromptsPerDocument;
}
