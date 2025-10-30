// src/types/notification.types.ts
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
}