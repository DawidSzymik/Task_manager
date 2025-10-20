// src/services/authService.ts

const API_BASE_URL = '/api/v1/auth';

// Types based on your backend
export interface LoginRequest {
    username: string;
    password: string;
}

export interface RegisterRequest {
    username: string;
    email: string;
    password: string;
    confirmPassword: string; // Backend requires this!
    fullName?: string;
}

export interface UserDto {
    id: number;
    username: string;
    email?: string;
    fullName?: string;
    systemRole?: string;
    active?: boolean;
    lastLogin?: string;
}

export interface AuthResponse {
    success: boolean;
    message: string;
    data?: {
        user: UserDto;
        sessionId?: string;
    };
    error?: string;
    sessionId?: string;
}

export interface ApiError {
    message: string;
    status: number;
}

// Helper function for handling responses
const handleResponse = async <T>(response: Response): Promise<T> => {
    const data = await response.json();

    if (!response.ok || !data.success) {
        const error: ApiError = {
            message: data.error || data.message || 'Wystąpił błąd',
            status: response.status,
        };
        throw error;
    }

    return data as T;
};

// Auth Service
export const authService = {
    // Login
    login: async (credentials: LoginRequest): Promise<AuthResponse> => {
        try {
            const response = await fetch(`${API_BASE_URL}/login`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify(credentials),
            });

            const data = await handleResponse<AuthResponse>(response);

            if (data.data?.user) {
                localStorage.setItem('user', JSON.stringify(data.data.user));
                localStorage.setItem('username', data.data.user.username);
                if (data.data.sessionId) {
                    localStorage.setItem('sessionId', data.data.sessionId);
                }
            }

            return data;
        } catch (error) {
            console.error('Login error:', error);
            throw error;
        }
    },

    // Register
    register: async (userData: RegisterRequest): Promise<AuthResponse> => {
        try {
            const response = await fetch(`${API_BASE_URL}/register`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify(userData),
            });

            const data = await handleResponse<AuthResponse>(response);

            if (data.data?.user) {
                localStorage.setItem('user', JSON.stringify(data.data.user));
                localStorage.setItem('username', data.data.user.username);
            }

            return data;
        } catch (error) {
            console.error('Register error:', error);
            throw error;
        }
    },

    // Logout
    logout: async (): Promise<void> => {
        try {
            await fetch(`${API_BASE_URL}/logout`, {
                method: 'POST',
                credentials: 'include',
            });
        } catch (error) {
            console.error('Logout error:', error);
        } finally {
            localStorage.removeItem('user');
            localStorage.removeItem('username');
            localStorage.removeItem('sessionId');
        }
    },

    // Check if user is authenticated
    isAuthenticated: (): boolean => {
        return !!localStorage.getItem('user');
    },

    // Get current user
    getCurrentUser: (): UserDto | null => {
        const userStr = localStorage.getItem('user');
        if (!userStr) return null;
        try {
            return JSON.parse(userStr);
        } catch {
            return null;
        }
    },

    // Get username
    getUsername: (): string | null => {
        return localStorage.getItem('username');
    },

    // Get user profile from API
    getProfile: async (): Promise<UserDto> => {
        try {
            const response = await fetch(`${API_BASE_URL}/me`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
            });

            const data = await handleResponse<{ success: boolean; data: UserDto }>(response);

            if (data.data) {
                localStorage.setItem('user', JSON.stringify(data.data));
            }

            return data.data;
        } catch (error) {
            console.error('Get profile error:', error);
            throw error;
        }
    },

    // Check auth status
    checkAuthStatus: async (): Promise<boolean> => {
        try {
            const response = await fetch(`${API_BASE_URL}/status`, {
                method: 'GET',
                credentials: 'include',
            });

            const data = await response.json();
            return data.authenticated || false;
        } catch (error) {
            console.error('Check auth status error:', error);
            return false;
        }
    },
};

export default authService;