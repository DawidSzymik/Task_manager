package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller do obsługi React Router (SPA routing)
 * Przekierowuje wszystkie non-API routes do index.html
 */
@Controller
public class SpaController {

    @GetMapping(value = {
            "/",
            "/dashboard",
            "/dashboard/**",
            "/tasks",
            "/tasks/**",
            "/projects",
            "/projects/**",
            "/teams",
            "/teams/**",
            "/profile",
            "/profile/**",
            "/login",
            "/register",
            "/settings",
            "/settings/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}