-- User table

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    email VARCHAR(255) NOT NULL,
    oid VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL,
    stripe_customer_id VARCHAR(255),
    role VARCHAR(50) NOT NULL,
    plan VARCHAR(50) NOT NULL DEFAULT 'FREE',
    plan_limit_max INTEGER NOT NULL DEFAULT 2,
    plan_limit_current INTEGER NOT NULL DEFAULT 2,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT user_email UNIQUE (email),
    CONSTRAINT uq_stripe_customer_id UNIQUE (stripe_customer_id)
);

CREATE TABLE documents (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    target_language VARCHAR(10) NOT NULL,
    title VARCHAR(500),
    sender VARCHAR(255),
    received_date VARCHAR(50),
    summary TEXT,
    translated_text TEXT,
    structured_translated_text JSONB,
    action_plan JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_documents_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Action plan tasks table
CREATE TABLE action_plan_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    document_id UUID NOT NULL,
    user_id UUID NOT NULL,
    title TEXT,
    due_date DATE,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    location VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_action_plan_tasks_document FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE CASCADE,
    CONSTRAINT fk_action_plan_tasks_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    user_id UUID NOT NULL,
    stripe_subscription_id VARCHAR(255) NOT NULL,
    stripe_customer_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    plan VARCHAR(50) NOT NULL,
    current_period_start TIMESTAMP WITH TIME ZONE NOT NULL,
    current_period_end TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_stripe_subscription_id UNIQUE (stripe_subscription_id),
    CONSTRAINT fk_subscriptions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE webhook_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_webhook_event_id UNIQUE (event_id)
);

CREATE INDEX idx_subscriptions_user_id ON subscriptions (user_id);

CREATE INDEX idx_subscriptions_stripe_customer_id ON subscriptions (stripe_customer_id);

CREATE INDEX idx_webhook_events_event_id ON webhook_events (event_id);

CREATE INDEX idx_documents_user_id ON documents (user_id);

CREATE INDEX idx_action_plan_tasks_document_id ON action_plan_tasks (document_id);

CREATE INDEX idx_apt_user_due_incomplete ON action_plan_tasks (user_id, due_date ASC)
WHERE
    completed = false;

CREATE INDEX idx_users_stripe_customer_id ON users (stripe_customer_id);