package com.example.demo.model;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users") // 🔥 Nazwa tabeli w bazie danych
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String password;

    @ManyToMany(mappedBy = "users", fetch = FetchType.EAGER)  // 🔥 Wymuszenie ładowania zespołów
    private Set<Team> teams = new HashSet<>();


    // 🔥 Konstruktor bezargumentowy (potrzebny dla Hibernate)
    public User() {}

    // 🔥 Konstruktor do rejestracji użytkownika
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }
    @ManyToMany(mappedBy = "assignedUsers")
    private Set<Task> tasks = new HashSet<>();


    // 🔥 GETTERY I SETTERY

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<Team> getTeams() {
        return teams;
    }

    public void setTeams(Set<Team> teams) {
        this.teams = teams;
    }
}
