package com.joklek.rentbot.repo;

import com.joklek.rentbot.entities.SentMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SentMessageRepo extends JpaRepository<SentMessage, Long> {

    Optional<SentMessage> findByChatIdAndMessageId(Long chatId, Integer messageId);

    Optional<SentMessage> findFirstByChatIdAndTypeOrderByMessageIdDesc(Long chatId, String type);
}
