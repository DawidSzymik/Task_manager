package com.example.demo.controller;

import com.example.demo.model.Team;
import com.example.demo.model.User;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Set;

@Controller
public class WelcomeController {

    @Autowired
    private UserService userService;  // 🔥 Teraz jest wstrzyknięte poprawnie

    @GetMapping("/welcome")
    public String welcome(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            throw new RuntimeException("Błąd: Brak zalogowanego użytkownika");
        }

        // Pobranie użytkownika z bazy danych
        User user = userService.getUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Użytkownik nie istnieje"));

        // Pobranie zespołów użytkownika
        Set<Team> teams = user.getTeams();
        System.out.println("Zespoły użytkownika: " + teams); // 🔥 Logowanie do konsoli

        model.addAttribute("username", user.getUsername());
        model.addAttribute("teams", teams);

        return "welcome";
    }
}
