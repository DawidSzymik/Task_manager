// src/main/java/com/example/demo/model/Project.java - POPRAWIONY
package com.example.demo.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(length = 1000)
    private String description;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime deadline;

    // Twórca projektu (zawsze admin)
    @ManyToOne
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    // POPRAWKA: Używamy ProjectMember zamiast bezpośredniej relacji
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<ProjectMember> members = new HashSet<>();

    // Konstruktory
    public Project() {}

    public Project(String name, String description, User createdBy) {
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
        // NIE DODAWAJ TUTAJ ProjectMember - to spowoduje błąd cykliczny
        // Dodaj to przez ProjectMemberService po zapisaniu Project
    }

    // Metody pomocnicze
    public boolean isAdmin(User user) {
        return members.stream()
                .anyMatch(member -> member.getUser().equals(user) &&
                        member.getRole() == ProjectRole.ADMIN);
    }

    public boolean isMember(User user) {
        return members.stream()
                .anyMatch(member -> member.getUser().equals(user));
    }

    public ProjectRole getUserRole(User user) {
        return members.stream()
                .filter(member -> member.getUser().equals(user))
                .map(ProjectMember::getRole)
                .findFirst()
                .orElse(null);
    }

    public List<User> getAdmins() {
        return members.stream()
                .filter(member -> member.getRole() == ProjectRole.ADMIN)
                .map(ProjectMember::getUser)
                .collect(Collectors.toList());
    }

    // USUŃ TE METODY - używaj ProjectMemberService
    // public void addMember(User user, ProjectRole role) { ... }
    // public void removeMember(User user) { ... }

    // Gettery i settery
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public Set<ProjectMember> getMembers() { return members; }
    public void setMembers(Set<ProjectMember> members) { this.members = members; }

    // Kompatybilność z starym kodem
    public Set<User> getAssignedUsers() {
        return members.stream()
                .map(ProjectMember::getUser)
                .collect(Collectors.toSet());
    }

    // Helper metoda dla equals w ProjectMember
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Project project = (Project) o;
        return id != null && id.equals(project.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}