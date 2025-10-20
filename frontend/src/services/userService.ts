// src/services/userService.ts
import type { User, ApiResponse } from '../types';

const API_BASE_URL = '/api/v1/admin/users';

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

export const userService = {
    // Get all users
    getAllUsers: async (search?: string, active?: boolean): Promise<User[]> => {
        try {
            let url = API_BASE_URL;
            const params = new URLSearchParams();

            if (search) params.append('search', search);
            if (active !== undefined) params.append('active', active.toString());

            if (params.toString()) {
                url += `?${params.toString()}`;
            }

            const response = await fetch(url, {
                method: 'GET',
                credentials: 'include',
            });

            const data = await handleResponse<User[]>(response);
            return data.data || [];
        } catch (error) {
            console.error('Get users error:', error);
            throw error;
        }
    },

    // Get user by ID
    getUserById: async (id: number): Promise<User> => {
        try {
            const response = await fetch(`${API_BASE_URL}/${id}`, {
                method: 'GET',
                credentials: 'include',
            });

            const data = await handleResponse<User>(response);
            if (!data.data) throw new Error('User not found');
            return data.data;
        } catch (error) {
            console.error('Get user error:', error);
            throw error;
        }
    },
};

export default userService;