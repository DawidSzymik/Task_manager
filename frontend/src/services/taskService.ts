// src/services/taskService.ts
import type {
    Task,
    CreateTaskRequest,
    UpdateTaskRequest,
    ApiResponse
} from '../types';

const API_BASE_URL = '/api/v1/tasks';

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

const taskService = {
    // Get all tasks
    getAllTasks: async (filters?: {
        projectId?: number;
        status?: string;
        priority?: string;
        assignedToMe?: boolean;
    }): Promise<Task[]> => {
        try {
            const params = new URLSearchParams();
            if (filters?.projectId) params.append('projectId', filters.projectId.toString());
            if (filters?.status) params.append('status', filters.status);
            if (filters?.priority) params.append('priority', filters.priority);
            if (filters?.assignedToMe) params.append('assignedToMe', 'true');

            const url = params.toString() ? `${API_BASE_URL}?${params}` : API_BASE_URL;
            const response = await fetch(url, {
                method: 'GET',
                credentials: 'include',
            });

            const data = await handleResponse<Task[]>(response);
            return data.data || [];
        } catch (error) {
            console.error('Get tasks error:', error);
            throw error;
        }
    },

    // Get task by ID
    getTaskById: async (id: number): Promise<Task> => {
        try {
            const response = await fetch(`${API_BASE_URL}/${id}`, {
                method: 'GET',
                credentials: 'include',
            });

            const data = await handleResponse<Task>(response);
            if (!data.data) throw new Error('Task not found');
            return data.data;
        } catch (error) {
            console.error('Get task error:', error);
            throw error;
        }
    },

    // Get tasks by project
    getTasksByProject: async (projectId: number, status?: string): Promise<Task[]> => {
        try {
            const url = status
                ? `${API_BASE_URL}/project/${projectId}?status=${status}`
                : `${API_BASE_URL}/project/${projectId}`;

            const response = await fetch(url, {
                method: 'GET',
                credentials: 'include',
            });

            const data = await handleResponse<Task[]>(response);
            return data.data || [];
        } catch (error) {
            console.error('Get project tasks error:', error);
            throw error;
        }
    },

    // Get my tasks
    getMyTasks: async (status?: string): Promise<Task[]> => {
        try {
            const url = status
                ? `${API_BASE_URL}/my?status=${status}`
                : `${API_BASE_URL}/my`;

            const response = await fetch(url, {
                method: 'GET',
                credentials: 'include',
            });

            const data = await handleResponse<Task[]>(response);
            return data.data || [];
        } catch (error) {
            console.error('Get my tasks error:', error);
            throw error;
        }
    },

    // Create new task
    createTask: async (taskData: CreateTaskRequest): Promise<Task> => {
        try {
            const response = await fetch(API_BASE_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify(taskData),
            });

            const data = await handleResponse<Task>(response);
            if (!data.data) throw new Error('Failed to create task');
            return data.data;
        } catch (error) {
            console.error('Create task error:', error);
            throw error;
        }
    },

    // Update task
    updateTask: async (id: number, taskData: UpdateTaskRequest): Promise<Task> => {
        try {
            const response = await fetch(`${API_BASE_URL}/${id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify(taskData),
            });

            const data = await handleResponse<Task>(response);
            if (!data.data) throw new Error('Failed to update task');
            return data.data;
        } catch (error) {
            console.error('Update task error:', error);
            throw error;
        }
    },

    // Delete task
    deleteTask: async (id: number): Promise<void> => {
        try {
            const response = await fetch(`${API_BASE_URL}/${id}`, {
                method: 'DELETE',
                credentials: 'include',
            });

            await handleResponse<void>(response);
        } catch (error) {
            console.error('Delete task error:', error);
            throw error;
        }
    },
};

export default taskService;