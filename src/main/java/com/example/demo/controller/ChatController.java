package com.example.demo.controller;

import com.example.demo.model.ChatMessage;
import com.example.demo.model.User;
import com.example.demo.repository.ChatMessageRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    @Autowired
    private UserRepository userRepository;

    // application.properties se key uthayega (Secure Way)
    @Value("${google.api.key:YOUR_GEMINI_API_KEY_HERE}") 
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/{ticketId}")
    public ResponseEntity<Map<String, Object>> getChatHistory(@PathVariable Long ticketId) {
        List<ChatMessage> messages = chatMessageRepository.findByTicketIdOrderByTimestampAsc(ticketId);
        Map<String, Object> response = new HashMap<>();
        response.put("chat", messages);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{ticketId}/message")
    public ResponseEntity<ChatMessage> sendMessage(
            @PathVariable Long ticketId, 
            @RequestHeader(value = "Authorization", defaultValue = "") String authHeader,
            @RequestBody Map<String, String> request) {
        
        String content = request.get("content");
        Long currentUserId = extractUserId(authHeader);

        User currentUser = userRepository.findById(currentUserId).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String senderName = currentUser.getUsername();
        String role = currentUser.getRole();

        // 1. User Message Save
        ChatMessage userMsg = new ChatMessage();
        userMsg.setTicketId(ticketId);
        userMsg.setContent(content);
        userMsg.setSenderId(currentUserId);
        userMsg.setSenderName(senderName + " (" + role.substring(0, 1).toUpperCase() + role.substring(1) + ")");
        chatMessageRepository.save(userMsg);

        // 2. AI Reply (Sirf tab jab User message bhej raha ho)
        if ("user".equalsIgnoreCase(role)) {
            new Thread(() -> { // Thread use kiya taaki UI hang na ho
                String aiReplyContent = getAiReply(content, senderName);
                ChatMessage aiMsg = new ChatMessage();
                aiMsg.setTicketId(ticketId);
                aiMsg.setContent(aiReplyContent);
                aiMsg.setSenderId(0L); 
                aiMsg.setSenderName("Quickie AI (Support Bot)");
                chatMessageRepository.save(aiMsg);
            }).start();
        }

        return ResponseEntity.ok(userMsg);
    }

    private Long extractUserId(String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer dummy-jwt-")) {
                return Long.parseLong(authHeader.replace("Bearer dummy-jwt-", "").trim());
            }
        } catch (Exception e) {
            System.out.println("Token error: " + e.getMessage());
        }
        return 1L; // Fallback
    }

    private String getAiReply(String userMessage, String userName) {
        try {
            String url = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent" + apiKey;

            String prompt = "You are Quickie, a helpful IT Support AI for a project built by Bhumika. " +
                            "User " + userName + " says: " + userMessage + 
                            ". Provide a very short, professional 2-line solution.";

            // Request Structure
            Map<String, Object> textPart = Map.of("text", prompt);
            Map<String, Object> partMap = Map.of("parts", Collections.singletonList(textPart));
            Map<String, Object> requestBody = Map.of("contents", Collections.singletonList(partMap));

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(requestBody), 
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getBody() != null && response.getBody().containsKey("candidates")) {
                List<?> candidates = (List<?>) response.getBody().get("candidates");
                Map<?, ?> firstCandidate = (Map<?, ?>) candidates.get(0);
                Map<?, ?> contentMap = (Map<?, ?>) firstCandidate.get("content");
                List<?> parts = (List<?>) contentMap.get("parts");
                Map<?, ?> firstPart = (Map<?, ?>) parts.get(0);
                return (String) firstPart.get("text");
            }
        } catch (Exception e) {
            return "Hi " + userName + ", I'm analyzing this. One moment please!";
        }
        return "Our support team will assist you soon!";
    }
}