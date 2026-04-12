package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String subject;
    private String description;
    private String status = "OPEN";
    
    private Long categoryId; // Category ko save karne ke liye
    private Long userId;
    private int upvotes = 0;   // Like button ke liye
    private int downvotes = 0; // Dislike button ke liye

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}