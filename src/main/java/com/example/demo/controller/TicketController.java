package com.example.demo.controller;

import com.example.demo.model.Ticket;
import com.example.demo.repository.TicketRepository;
import com.example.demo.repository.CategoryRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class TicketController {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Long getUserIdFromToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer dummy-jwt-")) {
            try {
                return Long.parseLong(authHeader.replace("Bearer dummy-jwt-", "").trim());
            } catch (Exception e) {}
        }
        return null;
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        Map<String, Object> stats = new HashMap<>();
        long total = ticketRepository.count();
        stats.put("totalTickets", total);
        stats.put("openTickets", total);
        stats.put("resolvedTickets", 0);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/notifications")
    public ResponseEntity<?> getNotifications() {
        return ResponseEntity.ok(new ArrayList<>());
    }

    @GetMapping({ "/tickets", "/tickets/my" })
    public ResponseEntity<?> getTickets(
            HttpServletRequest request,
            @RequestHeader(value = "Authorization", defaultValue = "") String authHeader,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false, defaultValue = "") String status,
            @RequestParam(required = false, defaultValue = "") String category,
            @RequestParam(required = false, defaultValue = "recent") String sort) {

        boolean isMyTicketsPage = request.getRequestURI().endsWith("/my");
        Long currentUserId = getUserIdFromToken(authHeader);

        List<Ticket> allTickets = ticketRepository.findAll();
        List<Map<String, Object>> enrichedTickets = new ArrayList<>();

        for (Ticket t : allTickets) {
            // My Tickets check
            if (isMyTicketsPage && (currentUserId == null || !currentUserId.equals(t.getUserId()))) {
                continue;
            }

            // 1. Sabse pehle asli Category Name nikalo (Filter karne ke liye)
            String catName = "General";
            if (t.getCategoryId() != null) {
                catName = categoryRepository.findById(t.getCategoryId())
                        .map(c -> c.getName()).orElse("Unknown");
            }

            // 2. Filters (Ab hum ID aur Name dono se match karenge)
            boolean matchesSearch = search.isEmpty() ||
                    (t.getSubject() != null && t.getSubject().toLowerCase().contains(search.toLowerCase()));

            boolean matchesStatus = status.isEmpty() || status.equalsIgnoreCase(t.getStatus());

            // Yahan fix kiya hai: Frontend Name bheje ya ID, dono handle ho jayega
            boolean matchesCategory = category.isEmpty() ||
                    catName.equalsIgnoreCase(category) || 
                    (t.getCategoryId() != null && t.getCategoryId().toString().equals(category));

            // Agar teeno filter pass ho jayein tabhi ticket dikhao
            if (matchesSearch && matchesStatus && matchesCategory) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", t.getId());
                map.put("subject", t.getSubject());
                map.put("description", t.getDescription());
                map.put("status", t.getStatus());
                map.put("createdAt", t.getCreatedAt());
                map.put("upvotes", t.getUpvotes());
                map.put("downvotes", t.getDownvotes());
                map.put("userId", t.getUserId());
                map.put("categoryName", catName);

                enrichedTickets.add(map);
            }
        }

        // Sorting Logic
        if ("recent".equals(sort)) {
            enrichedTickets.sort((t1, t2) -> ((java.time.LocalDateTime) t2.get("createdAt"))
                    .compareTo((java.time.LocalDateTime) t1.get("createdAt")));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("tickets", enrichedTickets);
        response.put("totalPages", 1);
        response.put("page", 1);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/tickets/{id}")
    public ResponseEntity<?> getTicketById(@PathVariable Long id) {
        Ticket t = ticketRepository.findById(id).orElse(null);
        if (t == null) return ResponseEntity.notFound().build();

        Map<String, Object> map = new HashMap<>();
        map.put("id", t.getId());
        map.put("subject", t.getSubject());
        map.put("description", t.getDescription());
        map.put("status", t.getStatus());

        String catName = "General";
        if (t.getCategoryId() != null) {
            catName = categoryRepository.findById(t.getCategoryId())
                    .map(c -> c.getName()).orElse("Unknown");
        }
        map.put("categoryName", catName);

        return ResponseEntity.ok(map);
    }

    @PostMapping("/tickets")
    public ResponseEntity<?> createTicket(
            @RequestHeader(value = "Authorization", defaultValue = "") String authHeader,
            @RequestParam("subject") String subject,
            @RequestParam("description") String description,
            @RequestParam(value = "categoryId", required = false) Long categoryId) {

        Long currentUserId = getUserIdFromToken(authHeader);

        Ticket ticket = new Ticket();
        ticket.setSubject(subject);
        ticket.setDescription(description);
        ticket.setCategoryId(categoryId);
        ticket.setUserId(currentUserId);

        Ticket savedTicket = ticketRepository.save(ticket);
        return ResponseEntity.ok(savedTicket);
    }

    @PostMapping("/tickets/{id}/vote")
    public ResponseEntity<?> voteTicket(@PathVariable Long id, @RequestBody Map<String, String> request) {
        Ticket ticket = ticketRepository.findById(id).orElse(null);
        if (ticket == null) return ResponseEntity.badRequest().body(Map.of("error", "Ticket not found"));

        String voteType = request.get("vote");
        if ("up".equals(voteType)) ticket.setUpvotes(ticket.getUpvotes() + 1);
        else if ("down".equals(voteType)) ticket.setDownvotes(ticket.getDownvotes() + 1);

        Ticket savedTicket = ticketRepository.save(ticket);
        return ResponseEntity.ok(savedTicket);
    }
}