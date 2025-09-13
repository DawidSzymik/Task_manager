// src/main/java/com/example/demo/controller/NotificationController.java - ROZSZERZONY
package com.example.demo.controller;

import com.example.demo.model.Notification;
import com.example.demo.model.User;
import com.example.demo.service.NotificationService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    // Lista powiadomień
    @GetMapping
    public String listNotifications(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();

        List<Notification> notifications = notificationService.getUserNotifications(currentUser);
        int unreadCount = notificationService.getUnreadCount(currentUser);

        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("currentUsername", userDetails.getUsername());

        return "notifications";
    }

    // Oznacz jako przeczytane
    @PostMapping("/{notificationId}/mark-read")
    public String markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return "redirect:/notifications";
    }

    // Oznacz wszystkie jako przeczytane
    @PostMapping("/mark-all-read")
    public String markAllAsRead(@AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        notificationService.markAllAsRead(currentUser);
        return "redirect:/notifications";
    }

    // Usuń powiadomienie
    @PostMapping("/{notificationId}/delete")
    public String deleteNotification(@PathVariable Long notificationId) {
        notificationService.deleteNotification(notificationId);
        return "redirect:/notifications";
    }

    // API endpoint dla liczby nieprzeczytanych (AJAX)
    @GetMapping("/unread-count")
    @ResponseBody
    public int getUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElseThrow();
        return notificationService.getUnreadCount(currentUser);
    }
}