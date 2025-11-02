// src/pages/NotificationsPage.tsx
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import MainLayout from '../components/MainLayout';
import notificationService, {type Notification } from '../services/notificationService';

type FilterType = 'all' | 'unread' | 'read';

const NotificationsPage: React.FC = () => {
    const navigate = useNavigate();
    const [notifications, setNotifications] = useState<Notification[]>([]);
    const [filteredNotifications, setFilteredNotifications] = useState<Notification[]>([]);
    const [loading, setLoading] = useState(true);
    const [filter, setFilter] = useState<FilterType>('all');
    const [unreadCount, setUnreadCount] = useState(0);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        loadNotifications();
        const interval = setInterval(loadUnreadCount, 30000); // Auto-refresh every 30s
        return () => clearInterval(interval);
    }, []);

    useEffect(() => {
        applyFilter();
    }, [notifications, filter]);

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

    const applyFilter = () => {
        let filtered = [...notifications];

        if (filter === 'unread') {
            filtered = filtered.filter(n => !n.isRead);
        } else if (filter === 'read') {
            filtered = filtered.filter(n => n.isRead);
        }

        setFilteredNotifications(filtered);
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

    // Konwersja URL z formatu Spring do React Router
    const convertSpringUrlToReact = (url: string): string => {
        if (!url) return '/dashboard';

        // /tasks/view/123 -> /tasks/123
        const taskViewMatch = url.match(/\/tasks\/view\/(\d+)/);
        if (taskViewMatch) {
            return `/tasks/${taskViewMatch[1]}`;
        }

        // /projects/view/123 -> /projects/123
        const projectViewMatch = url.match(/\/projects\/view\/(\d+)/);
        if (projectViewMatch) {
            return `/projects/${projectViewMatch[1]}`;
        }

        // /teams/view/123 -> /teams/123
        const teamViewMatch = url.match(/\/teams\/view\/(\d+)/);
        if (teamViewMatch) {
            return `/teams/${teamViewMatch[1]}`;
        }

        // Jeśli URL już jest w formacie React, zwróć go bez zmian
        return url;
    };

    const handleNotificationClick = async (notification: Notification) => {
        if (!notification.isRead) {
            await notificationService.markAsRead(notification.id);
            await loadNotifications();
        }

        if (notification.actionUrl) {
            const reactUrl = convertSpringUrlToReact(notification.actionUrl);
            navigate(reactUrl);
        }
    };

    const getIcon = (type: string) => {
        switch (type) {
            case 'TASK_PROPOSAL_PENDING': return '💡';
            case 'TASK_PROPOSAL_APPROVED': return '✅';
            case 'TASK_PROPOSAL_REJECTED': return '❌';
            case 'STATUS_CHANGE_PENDING': return '🔄';
            case 'STATUS_CHANGE_APPROVED': return '✅';
            case 'STATUS_CHANGE_REJECTED': return '❌';
            case 'TASK_ASSIGNED': return '👤';
            case 'TASK_COMMENT_ADDED': return '💬';
            case 'TASK_FILE_UPLOADED': return '📎';
            case 'PROJECT_MEMBER_ADDED': return '🎯';
            case 'NEW_MESSAGE': return '📩';
            case 'TASK_STATUS_CHANGED': return '📊';
            case 'NEW_ACTIVITY': return '⚡';
            case 'ROLE_CHANGED': return '🔑';
            default: return '📢';
        }
    };

    const getBorderColor = (type: string) => {
        switch (type) {
            case 'TASK_PROPOSAL_PENDING': return 'border-l-yellow-500';
            case 'TASK_PROPOSAL_APPROVED': return 'border-l-green-500';
            case 'TASK_PROPOSAL_REJECTED': return 'border-l-red-500';
            case 'STATUS_CHANGE_PENDING': return 'border-l-blue-500';
            case 'STATUS_CHANGE_APPROVED': return 'border-l-green-500';
            case 'STATUS_CHANGE_REJECTED': return 'border-l-red-500';
            case 'TASK_ASSIGNED': return 'border-l-purple-500';
            case 'TASK_COMMENT_ADDED': return 'border-l-teal-500';
            case 'TASK_FILE_UPLOADED': return 'border-l-orange-500';
            case 'PROJECT_MEMBER_ADDED': return 'border-l-indigo-500';
            case 'NEW_MESSAGE': return 'border-l-pink-500';
            default: return 'border-l-emerald-500';
        }
    };

    const getTimeAgo = (dateString: string) => {
        const date = new Date(dateString);
        const now = new Date();
        const seconds = Math.floor((now.getTime() - date.getTime()) / 1000);

        if (seconds < 60) return 'przed chwilą';
        const minutes = Math.floor(seconds / 60);
        if (minutes < 60) return `${minutes} min temu`;
        const hours = Math.floor(minutes / 60);
        if (hours < 24) return `${hours} godz. temu`;
        const days = Math.floor(hours / 24);
        if (days < 7) return `${days} dni temu`;
        return date.toLocaleDateString('pl-PL', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    if (loading) {
        return (
            <MainLayout>
                <div className="flex items-center justify-center min-h-screen">
                    <div className="text-center">
                        <div className="animate-spin rounded-full h-16 w-16 border-t-4 border-b-4 border-emerald-500 mx-auto"></div>
                        <p className="mt-4 text-gray-400">Ładowanie powiadomień...</p>
                    </div>
                </div>
            </MainLayout>
        );
    }

    return (
        <MainLayout>
            <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                {/* Header */}
                <div className="text-center mb-8">
                    <h1 className="text-4xl font-bold bg-gradient-to-r from-emerald-400 to-emerald-600 bg-clip-text text-transparent mb-2">
                        🔔 Powiadomienia
                    </h1>
                    <p className="text-gray-400">Zarządzaj swoimi powiadomieniami i bądź na bieżąco</p>
                </div>

                {/* Error Message */}
                {error && (
                    <div className="mb-6 bg-red-900/20 border border-red-500 text-red-400 px-4 py-3 rounded-lg">
                        {error}
                    </div>
                )}

                {/* Stats Cards */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
                    <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-6 border border-gray-700 hover:border-emerald-500 transition-all duration-300 hover:scale-105">
                        <div className="text-3xl font-bold text-emerald-500 mb-1">
                            {notifications.length}
                        </div>
                        <div className="text-sm text-gray-400">Wszystkie</div>
                    </div>
                    <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-6 border border-gray-700 hover:border-red-500 transition-all duration-300 hover:scale-105">
                        <div className="text-3xl font-bold text-red-500 mb-1">
                            {unreadCount}
                        </div>
                        <div className="text-sm text-gray-400">Nieprzeczytane</div>
                    </div>
                    <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-6 border border-gray-700 hover:border-green-500 transition-all duration-300 hover:scale-105">
                        <div className="text-3xl font-bold text-green-500 mb-1">
                            {notifications.length - unreadCount}
                        </div>
                        <div className="text-sm text-gray-400">Przeczytane</div>
                    </div>
                </div>

                {/* Action Bar */}
                <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-4 mb-6 border border-gray-700">
                    <div className="flex flex-col sm:flex-row justify-between items-center gap-4">
                        {/* Filter Buttons */}
                        <div className="flex gap-2 flex-wrap justify-center sm:justify-start">
                            <button
                                onClick={() => setFilter('all')}
                                className={`px-4 py-2 rounded-lg font-medium transition-all duration-300 ${
                                    filter === 'all'
                                        ? 'bg-emerald-500 text-white shadow-lg shadow-emerald-500/50'
                                        : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
                                }`}
                            >
                                📋 Wszystkie ({notifications.length})
                            </button>
                            <button
                                onClick={() => setFilter('unread')}
                                className={`px-4 py-2 rounded-lg font-medium transition-all duration-300 ${
                                    filter === 'unread'
                                        ? 'bg-red-500 text-white shadow-lg shadow-red-500/50'
                                        : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
                                }`}
                            >
                                🔴 Nieprzeczytane ({unreadCount})
                            </button>
                            <button
                                onClick={() => setFilter('read')}
                                className={`px-4 py-2 rounded-lg font-medium transition-all duration-300 ${
                                    filter === 'read'
                                        ? 'bg-green-500 text-white shadow-lg shadow-green-500/50'
                                        : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
                                }`}
                            >
                                ✅ Przeczytane ({notifications.length - unreadCount})
                            </button>
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

                {/* Notifications List */}
                <div className="space-y-4">
                    {filteredNotifications.length === 0 ? (
                        <div className="text-center py-16 bg-gray-800/30 backdrop-blur-sm rounded-xl border border-gray-700">
                            <div className="text-6xl mb-4">🔕</div>
                            <p className="text-xl text-gray-400">
                                {filter === 'unread' && 'Brak nieprzeczytanych powiadomień'}
                                {filter === 'read' && 'Brak przeczytanych powiadomień'}
                                {filter === 'all' && 'Nie masz żadnych powiadomień'}
                            </p>
                        </div>
                    ) : (
                        filteredNotifications.map((notification) => (
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
                                            <span className="text-xs text-gray-500 whitespace-nowrap">
                                                {getTimeAgo(notification.createdAt)}
                                            </span>
                                        </div>
                                        <p className="text-sm text-gray-400 mt-2 leading-relaxed">
                                            {notification.message}
                                        </p>
                                    </div>
                                </div>

                                {/* Actions */}
                                <div className="flex items-center gap-2 mt-4 pt-4 border-t border-gray-700">
                                    {notification.actionUrl && (
                                        <button
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                handleNotificationClick(notification);
                                            }}
                                            className="px-3 py-1.5 bg-emerald-500 hover:bg-emerald-600 text-white text-sm rounded-lg font-medium transition-all duration-300 flex items-center gap-2"
                                        >
                                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7l5 5m0 0l-5 5m5-5H6" />
                                            </svg>
                                            Przejdź
                                        </button>
                                    )}

                                    {!notification.isRead && (
                                        <button
                                            onClick={(e) => handleMarkAsRead(notification.id, e)}
                                            className="px-3 py-1.5 bg-gray-700 hover:bg-gray-600 text-white text-sm rounded-lg font-medium transition-all duration-300 flex items-center gap-2"
                                        >
                                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                            </svg>
                                            Oznacz jako przeczytane
                                        </button>
                                    )}

                                    <button
                                        onClick={(e) => handleDelete(notification.id, e)}
                                        className="px-3 py-1.5 bg-red-900/30 hover:bg-red-900/50 text-red-400 text-sm rounded-lg font-medium transition-all duration-300 flex items-center gap-2 ml-auto"
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

                {/* Back Link */}
                <div className="mt-8">
                    <button
                        onClick={() => navigate('/dashboard')}
                        className="text-emerald-500 hover:text-emerald-400 font-medium flex items-center gap-2 transition-all duration-300 hover:gap-3"
                    >
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
                        </svg>
                        Powrót do pulpitu
                    </button>
                </div>
            </div>
        </MainLayout>
    );
};

export default NotificationsPage;