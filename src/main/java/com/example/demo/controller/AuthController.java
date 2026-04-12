package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    public static class RegisterRequest {
        public String username;
        public String email;
        public String password;
        public String role;
        public List<String> specializations;
        public String adminKey;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        if (userRepository.findByEmail(req.email) != null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already exists!"));
        }

        if ("admin".equals(req.role) && !"QUICKDESK-PRO".equals(req.adminKey)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid Admin Key!"));
        }

        User user = new User();
        user.setUsername(req.username);
        user.setEmail(req.email);
        user.setPassword(req.password);
        user.setRole(req.role != null ? req.role : "user");
        user.setSpecializations(req.specializations);

        User savedUser = userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        // SMART TOKEN: Ab token ke andar User ID chhupi hai
        response.put("token", "dummy-jwt-" + savedUser.getId());
        response.put("user", savedUser);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        User user = userRepository.findByEmail(email);

        if (user != null && user.getPassword().equals(password)) {
            Map<String, Object> response = new HashMap<>();
            // SMART TOKEN: Ab token ke andar User ID chhupi hai
            response.put("token", "dummy-jwt-" + user.getId());
            response.put("user", user);
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.badRequest().body(Map.of("error", "Invalid Email or Password!"));
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }
}