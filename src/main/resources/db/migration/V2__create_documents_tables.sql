-- Documents table
CREATE TABLE documents (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                     UUID NOT NULL,
    target_language             VARCHAR(10) NOT NULL,
    title                       VARCHAR(500),
    sender                      VARCHAR(255),
    received_date               VARCHAR(50),
    summary                     TEXT,
    translated_text             TEXT,
    structured_translated_text  JSONB,
    action_plan                 JSONB,
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_documents_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- Action plan tasks table
CREATE TABLE action_plan_tasks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL,
    title       TEXT,
    due_date    VARCHAR(50),
    completed   BOOLEAN NOT NULL DEFAULT FALSE,
    location    VARCHAR(500),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_action_plan_tasks_document
        FOREIGN KEY (document_id)
        REFERENCES documents(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_documents_user_id ON documents(user_id);
CREATE INDEX idx_action_plan_tasks_document_id ON action_plan_tasks(document_id);