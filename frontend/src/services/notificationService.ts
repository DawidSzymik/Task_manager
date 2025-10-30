// src/services/notificationService.ts
const API_BASE_URL = '/api/v1/notifications';

export type NotificationType =
    | 'TASK_ASSIGNED'
    | 'TASK_STATUS_CHANGED'
    | 'TASK_PROPOSAL_PENDING'
    | 'TASK_PROPOSAL_APPROVED'
    | 'TASK_PROPOSAL_REJECTED'
    | 'STATUS_CHANGE_PENDING'
    | 'STATUS_CHANGE_APPROVED'
    | 'STATUS_CHANGE_REJECTED'
    | 'NEW_ACTIVITY'
    | 'ROLE_CHANGED'
    | 'NEW_MESSAGE'
    | 'PROJECT_MEMBER_ADDED'
    | 'TASK_COMMENT_ADDED'
    | 'TASK_FILE_UPLOADED'
    | 'OTHER';

export interface Notification {
    id: number;
    title: string;
    message: string;
    type: NotificationType;
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
    testUser?: string;
}

const handleResponse = async <T>(response: Response): Promise<T> => {
    const data = await response.json();
    if (!response.ok || !data.success) {
        throw new Error(data.error || data.message || 'Request failed');
    }
    return data as T;
};

const notificationService = {
    /**
     * Get all notifications for current user
     * @param unreadOnly - If true, return only unread notifications
     */
    getAllNotifications: async (unreadOnly: boolean = false): Promise<NotificationsResponse> => {
        const response = await fetch(`${API_BASE_URL}?unreadOnly=${unreadOnly}`, {
            credentials: 'include',
        });
        return handleResponse<NotificationsResponse>(response);
    },

    /**
     * Get unread notifications count
     */
    getUnreadCount: async (): Promise<number> => {
        const response = await fetch(`${API_BASE_URL}/unread-count`, {
            credentials: 'include',
        });
        const data = await handleResponse<{ success: boolean; unreadCount: number }>(response);
        return data.unreadCount;
    },

    /**
     * Mark a specific notification as read
     * @param id - Notification ID
     */
    markAsRead: async (id: number): Promise<void> => {
        const response = await fetch(`${API_BASE_URL}/${id}/mark-read`, {
            method: 'PUT',
            credentials: 'include',
        });
        await handleResponse(response);
    },

    /**
     * Mark all notifications as read
     */
    markAllAsRead: async (): Promise<void> => {
        const response = await fetch(`${API_BASE_URL}/mark-all-read`, {
            method: 'PUT',
            credentials: 'include',
        });
        await handleResponse(response);
    },

    /**
     * Delete a specific notification
     * @param id - Notification ID
     */
    deleteNotification: async (id: number): Promise<void> => {
        const response = await fetch(`${API_BASE_URL}/${id}`, {
            method: 'DELETE',
            credentials: 'include',
        });
        await handleResponse(response);
    },

    /**
     * Delete all notifications (optionally only read ones)
     * @param readOnly - If true, delete only read notifications
     */
    deleteAllNotifications: async (readOnly: boolean = false): Promise<void> => {
        const response = await fetch(`${API_BASE_URL}?readOnly=${readOnly}`, {
            method: 'DELETE',
            credentials: 'include',
        });
        await handleResponse(response);
    },

    /**
     * Get a specific notification by ID
     * @param id - Notification ID
     */
    getNotification: async (id: number): Promise<Notification> => {
        const response = await fetch(`${API_BASE_URL}/${id}`, {
            credentials: 'include',
        });
        const data = await handleResponse<{ success: boolean; message: string; data: Notification }>(response);
        return data.data;
    },
};

export default notificationService;