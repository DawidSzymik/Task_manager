// frontend/src/pages/NotificationsPage.tsx
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import MainLayout from '../components/MainLayout';
import notificationService, {type Notification } from '../services/notificationService';

const NotificationsPage: React.FC = () => {
    const navigate = useNavigate();
    const [notifications, setNotifications] = useState<Notification[]>([]);
    const [loading, setLoading] = useState(true);
    const [unreadCount, setUnreadCount] = useState(0);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        loadNotifications();
        const interval = setInterval(loadUnreadCount, 30000);
        return () => clearInterval(interval);
    }, []);

    const loadNotifications = async () => {
        try {
            setLoading(true);
            setError(null);
            const response = await notificationService.getAllNotifications(false);
            setNotifications(response.data);
            setUnreadCount(response.unreadCount);
        } catch (err: any) {
            console.error('Failed to load notifications:', err);
            setError(err.message || 'Nie udało się załadować powiadomień');
        } finally {
            setLoading(false);
        }
    };

    const loadUnreadCount = async () => {
        try {
            const count = await notificationService.getUnreadCount();
            setUnreadCount(count);
        } catch (error) {
            console.error('Failed to load unread count:', error);
        }
    };

    const handleMarkAsRead = async (id: number, e: React.MouseEvent) => {
        e.stopPropagation();
        try {
            await notificationService.markAsRead(id);
            await loadNotifications();
        } catch (error) {
            console.error('Failed to mark as read:', error);
        }
    };

    const handleMarkAllAsRead = async () => {
        if (unreadCount === 0) return;

        try {
            await notificationService.markAllAsRead();
            await loadNotifications();
        } catch (error) {
            console.error('Failed to mark all as read:', error);
        }
    };

    const handleDelete = async (id: number, e: React.MouseEvent) => {
        e.stopPropagation();

        if (!window.confirm('Czy na pewno chcesz usunąć to powiadomienie?')) {
            return;
        }

        try {
            await notificationService.deleteNotification(id);
            await loadNotifications();
        } catch (error) {
            console.error('Failed to delete notification:', error);
        }
    };

    const handleDeleteAll = async () => {
        if (!window.confirm('Czy na pewno chcesz usunąć wszystkie powiadomienia?')) {
            return;
        }

        try {
            await notificationService.deleteAllNotifications(false);
            await loadNotifications();
        } catch (error) {
            console.error('Failed to delete all notifications:', error);
        }
    };

    const handleNotificationClick = (notification: Notification) => {
        if (!notification.isRead) {
            handleMarkAsRead(notification.id, new Event('click') as any);
        }

        if (notification.actionUrl) {
            navigate(notification.actionUrl);
        }
    };

    const getIcon = (type: string): string => {
        const icons: Record<string, string> = {
            TASK_ASSIGNED: '📋',
            TASK_COMPLETED: '✅',
            TASK_DUE_SOON: '⏰',
            TASK_OVERDUE: '⚠️',
            COMMENT_ADDED: '💬',
            STATUS_CHANGED: '🔄',
            PRIORITY_CHANGED: '🎯',
            TEAM_MENTION: '👥',
        };
        return icons[type] || '📬';
    };

    const getBorderColor = (type: string): string => {
        const colors: Record<string, string> = {
            TASK_ASSIGNED: 'border-blue-500',
            TASK_COMPLETED: 'border-green-500',
            TASK_DUE_SOON: 'border-yellow-500',
            TASK_OVERDUE: 'border-red-500',
            COMMENT_ADDED: 'border-purple-500',
            STATUS_CHANGED: 'border-emerald-500',
            PRIORITY_CHANGED: 'border-orange-500',
            TEAM_MENTION: 'border-pink-500',
        };
        return colors[type] || 'border-gray-500';
    };

    const formatDate = (dateString: string): string => {
        const date = new Date(dateString);
        const now = new Date();
        const diffInMs = now.getTime() - date.getTime();
        const diffInMinutes = Math.floor(diffInMs / (1000 * 60));
        const diffInHours = Math.floor(diffInMs / (1000 * 60 * 60));
        const diffInDays = Math.floor(diffInMs / (1000 * 60 * 60 * 24));

        if (diffInMinutes < 1) return 'Przed chwilą';
        if (diffInMinutes < 60) return `${diffInMinutes} min temu`;
        if (diffInHours < 24) return `${diffInHours} godz. temu`;
        if (diffInDays < 7) return `${diffInDays} dni temu`;

        return date.toLocaleDateString('pl-PL', {
            day: 'numeric',
            month: 'long',
            year: date.getFullYear() !== now.getFullYear() ? 'numeric' : undefined
        });
    };

    return (
        <MainLayout>
            <div className="max-w-6xl mx-auto space-y-6">
                {/* Header */}
                <div className="bg-gradient-to-r from-gray-900 via-gray-800 to-gray-900 rounded-xl p-8 shadow-2xl border border-gray-700">
                    <div className="flex items-center justify-between">
                        <div>
                            <h1 className="text-4xl font-bold text-white mb-2 flex items-center gap-3">
                                🔔 Powiadomienia
                            </h1>
                            <p className="text-gray-400">
                                {unreadCount > 0
                                    ? `Masz ${unreadCount} nieprzeczytanych powiadomień`
                                    : 'Wszystkie powiadomienia są przeczytane'}
                            </p>
                        </div>

                        {/* Mark All as Read Button */}
                        {unreadCount > 0 && (
                            <button
                                onClick={handleMarkAllAsRead}
                                className="px-4 py-2 bg-gray-700 hover:bg-gray-600 text-white rounded-lg font-medium transition-all duration-300 flex items-center gap-2"
                            >
                                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                </svg>
                                Oznacz wszystkie
                            </button>
                        )}
                    </div>
                </div>

                {/* Error Display */}
                {error && (
                    <div className="bg-red-500/10 border border-red-500 rounded-xl p-4 text-red-400">
                        <div className="flex items-center gap-2">
                            <svg className="w-5 h-5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
                            </svg>
                            <span>{error}</span>
                        </div>
                    </div>
                )}

                {/* Loading State */}
                {loading ? (
                    <div className="text-center py-16 bg-gray-800/30 backdrop-blur-sm rounded-xl border border-gray-700">
                        <div className="animate-spin h-12 w-12 border-4 border-emerald-500 border-t-transparent rounded-full mx-auto mb-4"></div>
                        <p className="text-xl text-gray-400">Ładowanie powiadomień...</p>
                    </div>
                ) : (
                    <>
                        {/* Notifications List */}
                        <div className="space-y-4">
                            {notifications.length === 0 ? (
                                <div className="text-center py-16 bg-gray-800/30 backdrop-blur-sm rounded-xl border border-gray-700">
                                    <div className="text-6xl mb-4">🔕</div>
                                    <p className="text-xl text-gray-400">Nie masz żadnych powiadomień</p>
                                </div>
                            ) : (
                                notifications.map((notification) => (
                                    <div
                                        key={notification.id}
                                        onClick={() => handleNotificationClick(notification)}
                                        className={`
                                            bg-gray-800/50 backdrop-blur-sm rounded-xl p-6 border-l-4 
                                            ${getBorderColor(notification.type)}
                                            ${!notification.isRead ? 'border border-gray-600 shadow-lg' : 'border border-gray-700'}
                                            hover:bg-gray-800 transition-all duration-300 cursor-pointer
                                            hover:scale-[1.02] hover:shadow-xl
                                            relative group
                                        `}
                                    >
                                        {/* Unread Indicator */}
                                        {!notification.isRead && (
                                            <div className="absolute top-4 right-4">
                                                <span className="flex h-3 w-3">
                                                    <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-red-400 opacity-75"></span>
                                                    <span className="relative inline-flex rounded-full h-3 w-3 bg-red-500"></span>
                                                </span>
                                            </div>
                                        )}

                                        {/* Header */}
                                        <div className="flex items-start gap-4 mb-3">
                                            <div className="text-3xl flex-shrink-0">
                                                {getIcon(notification.type)}
                                            </div>
                                            <div className="flex-1 min-w-0">
                                                <div className="flex items-start justify-between gap-4">
                                                    <h3 className={`text-lg font-semibold ${!notification.isRead ? 'text-white' : 'text-gray-300'}`}>
                                                        {notification.title}
                                                    </h3>
                                                    <span className="text-sm text-gray-500 flex-shrink-0">
                                                        {formatDate(notification.createdAt)}
                                                    </span>
                                                </div>
                                                <p className="text-gray-400 mt-1 break-words">
                                                    {notification.message}
                                                </p>
                                            </div>
                                        </div>

                                        {/* Actions */}
                                        <div className="flex gap-2 mt-4 opacity-0 group-hover:opacity-100 transition-opacity">
                                            {!notification.isRead && (
                                                <button
                                                    onClick={(e) => handleMarkAsRead(notification.id, e)}
                                                    className="px-3 py-1.5 bg-emerald-500/20 hover:bg-emerald-500/30 text-emerald-400 rounded-lg text-sm font-medium transition-colors flex items-center gap-1"
                                                >
                                                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                                    </svg>
                                                    Oznacz jako przeczytane
                                                </button>
                                            )}
                                            <button
                                                onClick={(e) => handleDelete(notification.id, e)}
                                                className="px-3 py-1.5 bg-red-500/20 hover:bg-red-500/30 text-red-400 rounded-lg text-sm font-medium transition-colors flex items-center gap-1"
                                            >
                                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                                </svg>
                                                Usuń
                                            </button>
                                        </div>
                                    </div>
                                ))
                            )}
                        </div>

                        {/* Delete All Button */}
                        {notifications.length > 0 && (
                            <div className="flex justify-center pt-6">
                                <button
                                    onClick={handleDeleteAll}
                                    className="px-6 py-3 bg-red-500/20 hover:bg-red-500/30 text-red-400 rounded-lg font-medium transition-all duration-300 flex items-center gap-2 border border-red-500/30"
                                >
                                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                    </svg>
                                    Usuń wszystkie powiadomienia
                                </button>
                            </div>
                        )}
                    </>
                )}
            </div>
        </MainLayout>
    );
};

export default NotificationsPage;