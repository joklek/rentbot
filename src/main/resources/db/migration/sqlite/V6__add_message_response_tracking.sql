CREATE TABLE sent_messages
(
    id         INTEGER        NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
    chat_id    INTEGER        NOT NULL,
    message_id INTEGER        NOT NULL,
    type       VARCHAR(63)    NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS sent_messages_chat_message_ids ON sent_messages (chat_id, message_id);