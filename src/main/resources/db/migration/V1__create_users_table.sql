CREATE TABLE users (
    id UUID PRIMARY KEY,

    email VARCHAR(255) NOT NULL,
    oid VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL,

    role VARCHAR(50) NOT NULL,
    plan VARCHAR(50) NOT NULL DEFAULT 'FREE',

    plan_limit_max INTEGER NOT NULL DEFAULT 2,
    plan_limit_current INTEGER NOT NULL DEFAULT 2,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT user_email UNIQUE (email)
);