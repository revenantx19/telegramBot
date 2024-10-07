--liquidbase formated sql

-- changeset konstantin:1
CREATE TABLE telegram_bot_model
(
    id            BIGINT PRIMARY KEY,
    chat_id       BIGINT,
    date_and_time TIMESTAMP WITH TIME ZONE,
    message       TEXT
);

CREATE TABLE register
(
    id              BIGINT PRIMARY KEY,
    user_id         BIGINT,
    user_nick       TEXT,
    count_of_pretty BIGINT,
    chat_id         BIGINT
);