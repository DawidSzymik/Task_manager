// frontend/src/services/adminService.ts

const API_BASE_URL = '/api/v1/admin';
const PROJECT_API_URL = '/api/v1/projects';
const TASK_API_URL = '/api/v1/tasks';

export interface UserDto {
    id: number;
    username: string;
    email?: string;
    fullName?: string;
    systemRole?: 'SUPER_ADMIN' | 'USER';
    active?: boolean;
    lastLogin?: string;
    createdAt?: string;
}

export interface CreateUserRequest {
    username: string;
    password: string;
    email?: string;
    fullName?: string;
    systemRole: 'SUPER_ADMIN' | 'USER';
}

export interface UpdateUserRequest {
    email?: string;
    fullName?: string;
    systemRole?: 'SUPER_ADMIN' | 'USER';
    active?: boolean;
}

export interface UserStats {
    total: number;
    active: number;
    inactive: number;
    activePercentage: number;
}

// Project types
export interface ProjectDto {
    id: number;
    name: string;
    description?: string;
    createdBy?: UserDto;
    createdAt?: string;
    memberCount?: number;
    taskCount?: number;
}

// Task types
export interface TaskDto {
    id: number;
    title: string;
    description?: string;
    status: string;
    priority: string;
    deadline?: string;
    project?: {
        id: number;
        name: string;
    };
    assignedTo?: UserDto;
    createdBy?: UserDto;
    createdAt?: string;
}

export interface ApiResponse<T> {
    success: boolean;
    message: string;
    data?: T;
    error?: string;
}

const handleResponse = async <T,>(response: Response): Promise<T> => {
    if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.error || errorData.message || 'Request failed');
    }
    const data = await response.json();
    return data.data || data;
};

const adminService = {
    // ========== USER MANAGEMENT ==========

    getAllUsers: async (search?: string, active?: boolean): Promise<UserDto[]> => {
        try {
            const params = new URLSearchParams();
            if (search) params.append('search', search);
            if (active !== undefined) params.append('active', active.toString());

            const url = params.toString() ? `${API_BASE_URL}/users?${params}` : `${API_BASE_URL}/users`;

            const response = await fetch(url, {
                method: 'GET',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
            });

            return await handleResponse<UserDto[]>(response);
        } catch (error) {
            console.error('Get all users error:', error);
            throw error;
        }
    },

    getUserById: async (id: number): Promise<UserDto> => {
        try {
            const response = await fetch(`${API_BASE_URL}/users/${id}`, {
                method: 'GET',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
            });

            return await handleResponse<UserDto>(response);
        } catch (error) {
            console.error('Get user by ID error:', error);
            throw error;
        }
    },

    createUser: async (userData: CreateUserRequest): Promise<UserDto> => {
        try {
            const response = await fetch(`${API_BASE_URL}/users`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify(userData),
            });

            return await handleResponse<UserDto>(response);
        } catch (error) {
            console.error('Create user error:', error);
            throw error;
        }
    },

    updateUser: async (id: number, userData: UpdateUserRequest): Promise<UserDto> => {
        try {
            const response = await fetch(`${API_BASE_URL}/users/${id}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify(userData),
            });

            return await handleResponse<UserDto>(response);
        } catch (error) {
            console.error('Update user error:', error);
            throw error;
        }
    },

    deleteUser: async (id: number): Promise<void> => {
        try {
            const response = await fetch(`${API_BASE_URL}/users/${id}`, {
                method: 'DELETE',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.error || errorData.message || 'Failed to delete user');
            }
        } catch (error) {
            console.error('Delete user error:', error);
            throw error;
        }
    },

    getUserStats: async (): Promise<UserStats> => {
        try {
            const response = await fetch(`${API_BASE_URL}/users/stats`, {
                method: 'GET',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
            });

            return await handleResponse<UserStats>(response);
        } catch (error) {
            console.error('Get user stats error:', error);
            throw error;
        }
    },

    // ========== PROJECT MANAGEMENT ==========

    getAllProjects: async (): Promise<ProjectDto[]> => {
        try {
            const response = await fetch(`${PROJECT_API_URL}`, {
                method: 'GET',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
            });

            return await handleResponse<ProjectDto[]>(response);
        } catch (error) {
            console.error('Get all projects error:', error);
            throw error;
        }
    },

    getProjectById: async (id: number): Promise<ProjectDto> => {
        try {
            const response = await fetch(`${PROJECT_API_URL}/${id}`, {
                method: 'GET',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
            });

            return await handleResponse<ProjectDto>(response);
        } catch (error) {
            console.error('Get project by ID error:', error);
            throw error;
        }
    },

    deleteProject: async (id: number): Promise<void> => {
        try {
            const response = await fetch(`${PROJECT_API_URL}/${id}`, {
                method: 'DELETE',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.error || errorData.message || 'Failed to delete project');
            }
        } catch (error) {
            console.error('Delete project error:', error);
            throw error;
        }
    },

    // ========== TASK MANAGEMENT ==========

    // ✅ POPRAWIONA METODA - przekazuje assignedToMe=false dla admina, żeby widział WSZYSTKIE zadania
    getAllTasks: async (): Promise<TaskDto[]> => {
        try {
            // KLUCZOWA ZMIANA: dodajemy parametr assignedToMe=false
            // To sprawi, że SUPER_ADMIN zobaczy wszystkie zadania z całego systemu
            const response = await fetch(`${TASK_API_URL}?assignedToMe=false`, {
                method: 'GET',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
            });

            return await handleResponse<TaskDto[]>(response);
        } catch (error) {
            console.error('Get all tasks error:', error);
            throw error;
        }
    },

    getTaskById: async (id: number): Promise<TaskDto> => {
        try {
            const response = await fetch(`${TASK_API_URL}/${id}`, {
                method: 'GET',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
            });

            return await handleResponse<TaskDto>(response);
        } catch (error) {
            console.error('Get task by ID error:', error);
            throw error;
        }
    },

    deleteTask: async (id: number): Promise<void> => {
        try {
            const response = await fetch(`${TASK_API_URL}/${id}`, {
                method: 'DELETE',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.error || errorData.message || 'Failed to delete task');
            }
        } catch (error) {
            console.error('Delete task error:', error);
            throw error;
        }
    },
};

export default adminService;