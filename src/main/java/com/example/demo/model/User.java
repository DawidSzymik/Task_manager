// src/main/java/com/example/demo/model/User.java - DODANO RELACJĘ Z PROJEKTAMI
package com.example.demo.model;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String password;

    @ManyToMany(mappedBy = "users", fetch = FetchType.EAGER)
    private Set<Team> teams = new HashSet<>();

    @ManyToMany(mappedBy = "assignedUsers")
    private Set<Task> tasks = new HashSet<>();

    // NOWA RELACJA: Projekty użytkownika
    @ManyToMany(mappedBy = "assignedUsers", fetch = FetchType.EAGER)
    private Set<Project> projects = new HashSet<>();

    // Konstruktory
    public User() {}

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Gettery i settery
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Set<Team> getTeams() { return teams; }
    public void setTeams(Set<Team> teams) { this.teams = teams; }

    public Set<Task> getTasks() { return tasks; }
    public void setTasks(Set<Task> tasks) { this.tasks = tasks; }

    public Set<Project> getProjects() { return projects; }
    public void setProjects(Set<Project> projects) { this.projects = projects; }
}