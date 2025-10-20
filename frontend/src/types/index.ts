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

// Project Role Type
export type ProjectRole = 'ADMIN' | 'MEMBER' | 'VIEWER';

// Task Status Type
export type TaskStatus = 'NEW' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

// Task Priority Type
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

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
    project?: Project;
    role: ProjectRole;
    joinedAt?: string;
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

// Task Types
export interface Task {
    id: number;
    title: string;
    description?: string;
    status: TaskStatus;
    priority: TaskPriority;
    deadline?: string;
    createdAt?: string;
    completedAt?: string;
    assignedTo?: User;
    assignedUsers?: User[];
    project?: Project;
    createdBy?: User;
    commentCount?: number;
    fileCount?: number;
    hasDeadlinePassed?: boolean;
    daysUntilDeadline?: number;
}

export interface CreateTaskRequest {
    title: string;
    description?: string;
    priority?: TaskPriority;
    deadline?: string;
    projectId: number;
    assignedToId?: number;
    assignedUserIds?: number[];
}

export interface UpdateTaskRequest {
    title?: string;
    description?: string;
    status?: TaskStatus;
    priority?: TaskPriority;
    deadline?: string;
    assignedToId?: number;
}

// Comment Types
export interface Comment {
    id: number;
    text: string;
    createdAt: string;
    updatedAt?: string;
    author: User;
    taskId?: number;
    taskTitle?: string;
    canEdit?: boolean;
    canDelete?: boolean;
}

export interface CreateCommentRequest {
    text: string;
}

export interface UpdateCommentRequest {
    text: string;
}

// File Types
export interface UploadedFile {
    id: number;
    originalName: string;
    contentType: string;
    fileSize: number;
    fileSizeFormatted?: string;
    uploadedAt: string;
    uploadedBy: User;
    taskId?: number;
    taskTitle?: string;
    downloadUrl?: string;
    isImage?: boolean;
    isDocument?: boolean;
    canDelete?: boolean;
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