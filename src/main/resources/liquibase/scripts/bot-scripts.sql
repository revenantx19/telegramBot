--liquidbase formated sql

-- changeset konstantin:1
CREATE TABLE telegram_bot_model (
                                   id BIGINT PRIMARY KEY,
                                   chat_id BIGINT,
                                   date_and_time TIMESTAMP WITH TIME ZONE,
                                   message TEXT
)