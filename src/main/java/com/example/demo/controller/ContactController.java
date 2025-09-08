package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class ContactController {

    @GetMapping("/kontakt")
    public String showContactForm() {
        return "kontakt";
    }

    @PostMapping("/kontakt")
    public String handleContactForm(
            @RequestParam String email,
            @RequestParam String subject,
            @RequestParam String message
    ) {
        // Tu możesz dodać np. zapis do bazy danych lub wysyłkę e-maila
        System.out.println("Formularz kontaktowy: " + email + ", " + subject + ", " + message);
        return "redirect:/kontakt?success";
    }
}
