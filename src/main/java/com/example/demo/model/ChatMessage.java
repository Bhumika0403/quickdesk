package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long ticketId; // Kis ticket ki chat hai
    private Long senderId; // Kisne bheja
    private String senderName; // Bhejne wale ka naam
    
    @Column(columnDefinition = "TEXT")
    private String content; // Message kya hai
    
    private LocalDateTime timestamp = LocalDateTime.now();
}