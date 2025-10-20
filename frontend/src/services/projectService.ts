// src/services/projectService.ts
import type { Project, CreateProjectRequest, UpdateProjectRequest, ProjectMember, ProjectRole, ApiResponse } from '../types';

const API_BASE_URL = '/api/v1/projects';

const handleResponse = async <T>(response: Response): Promise<ApiResponse<T>> => {
    const data = await response.json();

    if (!response.ok || !data.success) {
        throw {
            message: data.error || data.message || 'Wystąpił błąd',
            status: response.status,
        };
    }

    return data;
};

export const projectService = {
    // Get all projects
    getAllProjects: async (includeAll: boolean = false): Promise<Project[]> => {
        try {
            const url = includeAll ? `${API_BASE_URL}?includeAll=true` : API_BASE_URL;
            const response = await fetch(url, {
                method: 'GET',
                credentials: 'include',
            });

            const data = await handleResponse<Project[]>(response);
            return data.data || [];
        } catch (error) {
            console.error('Get projects error:', error);
            throw error;
        }
    },

    // Get project by ID
    getProjectById: async (id: number): Promise<Project> => {
        try {
            const response = await fetch(`${API_BASE_URL}/${id}`, {
                method: 'GET',
                credentials: 'include',
            });

            const data = await handleResponse<Project>(response);
            if (!data.data) throw new Error('Project not found');
            return data.data;
        } catch (error) {
            console.error('Get project error:', error);
            throw error;
        }
    },

    // Create new project
    createProject: async (projectData: CreateProjectRequest): Promise<Project> => {
        try {
            const response = await fetch(API_BASE_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify(projectData),
            });

            const data = await handleResponse<Project>(response);
            if (!data.data) throw new Error('Failed to create project');
            return data.data;
        } catch (error) {
            console.error('Create project error:', error);
            throw error;
        }
    },

    // Update project
    updateProject: async (id: number, projectData: UpdateProjectRequest): Promise<Project> => {
        try {
            const response = await fetch(`${API_BASE_URL}/${id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify(projectData),
            });

            const data = await handleResponse<Project>(response);
            if (!data.data) throw new Error('Failed to update project');
            return data.data;
        } catch (error) {
            console.error('Update project error:', error);
            throw error;
        }
    },

    // Delete project
    deleteProject: async (id: number): Promise<void> => {
        try {
            const response = await fetch(`${API_BASE_URL}/${id}`, {
                method: 'DELETE',
                credentials: 'include',
            });

            await handleResponse<void>(response);
        } catch (error) {
            console.error('Delete project error:', error);
            throw error;
        }
    },

    // Get project members
    getProjectMembers: async (id: number): Promise<ProjectMember[]> => {
        try {
            const response = await fetch(`${API_BASE_URL}/${id}/members`, {
                method: 'GET',
                credentials: 'include',
            });

            const data = await handleResponse<ProjectMember[]>(response);
            return data.data || [];
        } catch (error) {
            console.error('Get project members error:', error);
            throw error;
        }
    },

    // Add member to project
    addMemberToProject: async (projectId: number, userId: number, role: ProjectRole): Promise<void> => {
        try {
            const response = await fetch(`${API_BASE_URL}/${projectId}/members`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify({ userId, role }),
            });

            await handleResponse<void>(response);
        } catch (error) {
            console.error('Add member error:', error);
            throw error;
        }
    },

    // Remove member from project
    removeMemberFromProject: async (projectId: number, userId: number): Promise<void> => {
        try {
            const response = await fetch(`${API_BASE_URL}/${projectId}/members/${userId}`, {
                method: 'DELETE',
                credentials: 'include',
            });

            await handleResponse<void>(response);
        } catch (error) {
            console.error('Remove member error:', error);
            throw error;
        }
    },

    // Change member role
    changeMemberRole: async (projectId: number, userId: number, role: ProjectRole): Promise<void> => {
        try {
            const response = await fetch(`${API_BASE_URL}/${projectId}/members/${userId}/role`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify({ role }),
            });

            await handleResponse<void>(response);
        } catch (error) {
            console.error('Change member role error:', error);
            throw error;
        }
    },
};

export default projectService;