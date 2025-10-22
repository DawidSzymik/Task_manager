// frontend/src/services/statusRequestService.ts
import type { StatusChangeRequest, CreateStatusChangeRequest, ApiResponse } from '../types';

const API_BASE_URL = '/api/v1/status-requests';

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

const statusRequestService = {
    // Get requests for task
    getTaskRequests: async (taskId: number): Promise<StatusChangeRequest[]> => {
        try {
            const response = await fetch(`${API_BASE_URL}/task/${taskId}`, {
                method: 'GET',
                credentials: 'include',
            });

            const data = await handleResponse<StatusChangeRequest[]>(response);
            return data.data || [];
        } catch (error) {
            console.error('Get task requests error:', error);
            throw error;
        }
    },

    // Create status change request
    createRequest: async (requestData: CreateStatusChangeRequest): Promise<void> => {
        try {
            const response = await fetch(API_BASE_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify(requestData),
            });

            await handleResponse<void>(response);
        } catch (error) {
            console.error('Create status request error:', error);
            throw error;
        }
    },

    // Approve request (admin only)
    approveRequest: async (requestId: number): Promise<void> => {
        try {
            const response = await fetch(`${API_BASE_URL}/${requestId}/approve`, {
                method: 'POST',
                credentials: 'include',
            });

            await handleResponse<void>(response);
        } catch (error) {
            console.error('Approve request error:', error);
            throw error;
        }
    },

    // Reject request (admin only)
    rejectRequest: async (requestId: number, reason: string): Promise<void> => {
        try {
            const response = await fetch(`${API_BASE_URL}/${requestId}/reject`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify({ reason }),
            });

            await handleResponse<void>(response);
        } catch (error) {
            console.error('Reject request error:', error);
            throw error;
        }
    },
};

export default statusRequestService;