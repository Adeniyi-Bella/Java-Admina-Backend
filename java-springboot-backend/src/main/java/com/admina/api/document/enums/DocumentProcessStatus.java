package com.admina.api.document.enums;

public enum DocumentProcessStatus {
    PENDING, // job queued in RabbitMQ
    QUEUE, // job picked up by worker
    TRANSLATE, // translating document
    SUMMARIZE, // summarizing document
    SAVING, // saving to DB
    COMPLETED, // done
    ERROR, // failed
    CANCELLED // cancelled
}
