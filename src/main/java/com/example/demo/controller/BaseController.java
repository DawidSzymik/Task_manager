// src/main/java/com/example/demo/controller/BaseController.java - NOWY
package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.service.NotificationService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class BaseController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    @ModelAttribute("unreadNotificationCount")
    public Integer getUnreadNotificationCount(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return 0;
        }

        try {
            User currentUser = userService.getUserByUsername(userDetails.getUsername()).orElse(null);
            if (currentUser == null) {
                return 0;
            }
            return notificationService.getUnreadCount(currentUser);
        } catch (Exception e) {
            return 0;
        }
    }
}