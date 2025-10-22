// src/services/notificationService.ts

const API_BASE_URL = '/api/v1/notifications';

export interface Notification {
    id: number;
    title: string;
    message: string;
    type: 'TASK_PROPOSAL_PENDING' | 'TASK_PROPOSAL_APPROVED' | 'TASK_PROPOSAL_REJECTED' |
        'STATUS_CHANGE_PENDING' | 'STATUS_CHANGE_APPROVED' | 'STATUS_CHANGE_REJECTED' | 'OTHER';
    isRead: boolean;
    createdAt: string;
    relatedId?: number;
    actionUrl?: string;
}

export interface NotificationsResponse {
    success: boolean;
    message: string;
    data: Notification[];
    unreadCount: number;
    totalCount: number;
}

const handleResponse = async <T>(response: Response): Promise<T> => {
    const data = await response.json();
    if (!response.ok || !data.success) {
        throw new Error(data.error || data.message || 'Request failed');
    }
    return data as T;
};

const notificationService = {
    // Get all notifications
    getAllNotifications: async (unreadOnly: boolean = false): Promise<NotificationsResponse> => {
        const response = await fetch(`${API_BASE_URL}?unreadOnly=${unreadOnly}`, {
            credentials: 'include',
        });
        return handleResponse<NotificationsResponse>(response);
    },

    // Get unread count
    getUnreadCount: async (): Promise<number> => {
        const response = await fetch(`${API_BASE_URL}/unread-count`, {
            credentials: 'include',
        });
        const data = await handleResponse<{ success: boolean; unreadCount: number }>(response);
        return data.unreadCount;
    },

    // Mark notification as read
    markAsRead: async (id: number): Promise<void> => {
        const response = await fetch(`${API_BASE_URL}/${id}/mark-read`, {
            method: 'PUT',
            credentials: 'include',
        });
        await handleResponse(response);
    },

    // Mark all as read
    markAllAsRead: async (): Promise<void> => {
        const response = await fetch(`${API_BASE_URL}/mark-all-read`, {
            method: 'PUT',
            credentials: 'include',
        });
        await handleResponse(response);
    },

    // Delete notification
    deleteNotification: async (id: number): Promise<void> => {
        const response = await fetch(`${API_BASE_URL}/${id}`, {
            method: 'DELETE',
            credentials: 'include',
        });
        await handleResponse(response);
    },

    // Delete all notifications
    deleteAllNotifications: async (readOnly: boolean = false): Promise<void> => {
        const response = await fetch(`${API_BASE_URL}?readOnly=${readOnly}`, {
            method: 'DELETE',
            credentials: 'include',
        });
        await handleResponse(response);
    },
};

export default notificationService;