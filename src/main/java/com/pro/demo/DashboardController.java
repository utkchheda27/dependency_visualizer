package com.pro.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalUsers", userService.countUsers());
        stats.put("activeUsers", userService.findActiveUsers().size());
        stats.put("notifications", notificationService.getNotificationCount());
        stats.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/recent-users")
    public ResponseEntity<List<User>> getRecentUsers() {
        List<User> recentUsers = userService.findAll();
        return ResponseEntity.ok(recentUsers);
    }

    @PostMapping("/notify-users")
    public ResponseEntity<Map<String, String>> notifyUsers(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        List<User> users = userService.findAll();

        int notified = notificationService.notifyUsers(users, message);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Notified " + notified + " users");

        return ResponseEntity.ok(response);
    }
}