// src/main/java/com/example/demo/api/dto/response/StatsDto.java
package com.example.demo.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StatsDto {

    // System-wide stats
    private Long totalUsers;
    private Long activeUsers;
    private Long totalProjects;
    private Long totalTeams;
    private Long totalTasks;
    private Long totalComments;
    private Long totalFiles;

    // Task breakdown
    private Long tasksNew;
    private Long tasksInProgress;
    private Long tasksCompleted;
    private Long tasksCancelled;

    // User-specific stats
    private Long userProjects;
    private Long userTeams;
    private Long userTasksAssigned;
    private Long userTasksCreated;
    private Long userComments;
    private Long userFilesUploaded;

    // Additional metrics
    private Double taskCompletionRate;
    private Long overdueTasks;
    private Long tasksCompletedThisWeek;
    private Long tasksCompletedThisMonth;

    public StatsDto() {}

    // Getters and setters
    public Long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(Long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public Long getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(Long activeUsers) {
        this.activeUsers = activeUsers;
    }

    public Long getTotalProjects() {
        return totalProjects;
    }

    public void setTotalProjects(Long totalProjects) {
        this.totalProjects = totalProjects;
    }

    public Long getTotalTeams() {
        return totalTeams;
    }

    public void setTotalTeams(Long totalTeams) {
        this.totalTeams = totalTeams;
    }

    public Long getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(Long totalTasks) {
        this.totalTasks = totalTasks;
    }

    public Long getTotalComments() {
        return totalComments;
    }

    public void setTotalComments(Long totalComments) {
        this.totalComments = totalComments;
    }

    public Long getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(Long totalFiles) {
        this.totalFiles = totalFiles;
    }

    public Long getTasksNew() {
        return tasksNew;
    }

    public void setTasksNew(Long tasksNew) {
        this.tasksNew = tasksNew;
    }

    public Long getTasksInProgress() {
        return tasksInProgress;
    }

    public void setTasksInProgress(Long tasksInProgress) {
        this.tasksInProgress = tasksInProgress;
    }

    public Long getTasksCompleted() {
        return tasksCompleted;
    }

    public void setTasksCompleted(Long tasksCompleted) {
        this.tasksCompleted = tasksCompleted;
    }

    public Long getTasksCancelled() {
        return tasksCancelled;
    }

    public void setTasksCancelled(Long tasksCancelled) {
        this.tasksCancelled = tasksCancelled;
    }

    public Long getUserProjects() {
        return userProjects;
    }

    public void setUserProjects(Long userProjects) {
        this.userProjects = userProjects;
    }

    public Long getUserTeams() {
        return userTeams;
    }

    public void setUserTeams(Long userTeams) {
        this.userTeams = userTeams;
    }

    public Long getUserTasksAssigned() {
        return userTasksAssigned;
    }

    public void setUserTasksAssigned(Long userTasksAssigned) {
        this.userTasksAssigned = userTasksAssigned;
    }

    public Long getUserTasksCreated() {
        return userTasksCreated;
    }

    public void setUserTasksCreated(Long userTasksCreated) {
        this.userTasksCreated = userTasksCreated;
    }

    public Long getUserComments() {
        return userComments;
    }

    public void setUserComments(Long userComments) {
        this.userComments = userComments;
    }

    public Long getUserFilesUploaded() {
        return userFilesUploaded;
    }

    public void setUserFilesUploaded(Long userFilesUploaded) {
        this.userFilesUploaded = userFilesUploaded;
    }

    public Double getTaskCompletionRate() {
        return taskCompletionRate;
    }

    public void setTaskCompletionRate(Double taskCompletionRate) {
        this.taskCompletionRate = taskCompletionRate;
    }

    public Long getOverdueTasks() {
        return overdueTasks;
    }

    public void setOverdueTasks(Long overdueTasks) {
        this.overdueTasks = overdueTasks;
    }

    public Long getTasksCompletedThisWeek() {
        return tasksCompletedThisWeek;
    }

    public void setTasksCompletedThisWeek(Long tasksCompletedThisWeek) {
        this.tasksCompletedThisWeek = tasksCompletedThisWeek;
    }

    public Long getTasksCompletedThisMonth() {
        return tasksCompletedThisMonth;
    }

    public void setTasksCompletedThisMonth(Long tasksCompletedThisMonth) {
        this.tasksCompletedThisMonth = tasksCompletedThisMonth;
    }
}