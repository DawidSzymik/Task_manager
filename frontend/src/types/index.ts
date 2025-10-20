// src/types/index.ts

// User Types
export interface User {
    id: number;
    username: string;
    email?: string;
    fullName?: string;
    systemRole?: string;
    active?: boolean;
    lastLogin?: string;
    createdAt?: string;
}

// Team Types
export interface Team {
    id: number;
    name: string;
    description?: string;
    createdAt?: string;
    createdBy?: User;
    members?: User[];
    memberCount?: number;
    isCreator?: boolean;
    canEdit?: boolean;
    canDelete?: boolean;
}

export interface CreateTeamRequest {
    name: string;
    description?: string;
}

export interface UpdateTeamRequest {
    name?: string;
    description?: string;
}

export interface AddMemberRequest {
    userId: number;
}

// Project Types
export interface Project {
    id: number;
    name: string;
    description?: string;
    createdAt?: string;
    updatedAt?: string;
    createdBy?: User;
    members?: ProjectMember[];
    memberCount?: number;
    taskCount?: number;
}

export interface ProjectMember {
    id: number;
    user: User;
    project: Project;
    role: ProjectRole;
    joinedAt?: string;
}

export enum ProjectRole {
    ADMIN = 'ADMIN',
    MEMBER = 'MEMBER',
    VIEWER = 'VIEWER'
}

export interface CreateProjectRequest {
    name: string;
    description?: string;
}

export interface UpdateProjectRequest {
    name?: string;
    description?: string;
}

export interface AddProjectMemberRequest {
    userId: number;
    role: ProjectRole;
}

// Task Types (for future use)
export interface Task {
    id: number;
    title: string;
    description?: string;
    status?: string;
    priority?: string;
    deadline?: string;
    createdAt?: string;
    assignedTo?: User;
}

// API Response Types
export interface ApiResponse<T> {
    success: boolean;
    message: string;
    data?: T;
    error?: string;
}

export interface PaginatedResponse<T> {
    success: boolean;
    message: string;
    data: T[];
    totalItems?: number;
    currentPage?: number;
    totalPages?: number;
}