package com.example.demo.repository;

import com.example.demo.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // Ye magical line saare messages ko time ke hisaab se line mein laga degi
    List<ChatMessage> findByTicketIdOrderByTimestampAsc(Long ticketId);
}