package com.joklek.rentbot.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

@Entity
@Table(name = "sent_messages")
public class SentMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private Long chatId;
    @NotNull
    private Integer messageId;
    @NotNull
    private String type;

    public SentMessage(Long chatId, Integer messageId, String type) {
        this.chatId = chatId;
        this.messageId = messageId;
        this.type = type;
    }

    public SentMessage() {
    }

    public Long getId() {
        return id;
    }

    public SentMessage setId(Long id) {
        this.id = id;
        return this;
    }

    public @NotNull Long getChatId() {
        return chatId;
    }

    public SentMessage setChatId(@NotNull Long chatId) {
        this.chatId = chatId;
        return this;
    }

    public @NotNull Integer getMessageId() {
        return messageId;
    }

    public SentMessage setMessageId(@NotNull Integer messageId) {
        this.messageId = messageId;
        return this;
    }

    public @NotNull String getType() {
        return type;
    }

    public SentMessage setType(@NotNull String type) {
        this.type = type;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var message = (SentMessage) o;
        return this.id != null &&
                Objects.equals(this.id, message.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}
