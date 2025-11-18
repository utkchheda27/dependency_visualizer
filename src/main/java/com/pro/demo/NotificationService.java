package com.pro.demo;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private int notificationCount = 0;

    public int notifyUsers(List<User> users, String message) {
        // Simulate notification logic
        if (users == null || users.isEmpty()) {
            return 0;
        }

        for (User user : users) {
            // Simulate sending notification
            System.out.println("Sending notification to " + user.getName() + ": " + message);
        }

        notificationCount += users.size();
        return users.size();
    }

    public int getNotificationCount() {
        return notificationCount;
    }

    public void resetNotificationCount() {
        notificationCount = 0;
    }

    public boolean sendSingleNotification(User user, String message) {
        if (user == null || message == null) {
            return false;
        }

        System.out.println("Sending notification to " + user.getName() + ": " + message);
        notificationCount++;
        return true;
    }
}