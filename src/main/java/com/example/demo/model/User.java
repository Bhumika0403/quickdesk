package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String password;
    private String email;

    private String role; // Enum hata diya taaki lowercase/uppercase ka issue na aaye

    @ElementCollection
    private List<String> specializations; // Frontend se aane wali specializations ke liye

    private LocalDateTime createdAt = LocalDateTime.now();
}