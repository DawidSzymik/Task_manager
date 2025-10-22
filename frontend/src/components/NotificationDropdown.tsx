// src/components/NotificationDropdown.tsx
import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import notificationService, {type Notification } from '../services/notificationService';

const NotificationDropdown: React.FC = () => {
    const navigate = useNavigate();
    const [showDropdown, setShowDropdown] = useState(false);
    const [notifications, setNotifications] = useState<Notification[]>([]);
    const [unreadCount, setUnreadCount] = useState(0);
    const [loading, setLoading] = useState(false);
    const dropdownRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        loadUnreadCount();
        const interval = setInterval(loadUnreadCount, 30000); // Refresh every 30s
        return () => clearInterval(interval);
    }, []);

    useEffect(() => {
        if (showDropdown) {
            loadNotifications();
        }
    }, [showDropdown]);

    // Close dropdown when clicking outside
    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
                setShowDropdown(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const loadUnreadCount = async () => {
        try {
            const count = await notificationService.getUnreadCount();
            setUnreadCount(count);
        } catch (error) {
            console.error('Failed to load unread count:', error);
        }
    };

    const loadNotifications = async () => {
        try {
            setLoading(true);
            const response = await notificationService.getAllNotifications(false);
            setNotifications(response.data.slice(0, 10)); // Show only 10 most recent
            setUnreadCount(response.unreadCount);
        } catch (error) {
            console.error('Failed to load notifications:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleMarkAsRead = async (id: number, e: React.MouseEvent) => {
        e.stopPropagation();
        try {
            await notificationService.markAsRead(id);
            loadNotifications();
            loadUnreadCount();
        } catch (error) {
            console.error('Failed to mark as read:', error);
        }
    };

    const handleMarkAllAsRead = async () => {
        try {
            await notificationService.markAllAsRead();
            loadNotifications();
            loadUnreadCount();
        } catch (error) {
            console.error('Failed to mark all as read:', error);
        }
    };

    const handleNotificationClick = async (notification: Notification) => {
        if (!notification.isRead) {
            await notificationService.markAsRead(notification.id);
            loadUnreadCount();
        }
        setShowDropdown(false);
        if (notification.actionUrl) {
            navigate(notification.actionUrl);
        }
    };

    // W NotificationDropdown.tsx - funkcja getIcon:
    const getIcon = (type: string) => {
        switch (type) {
            case 'TASK_PROPOSAL_PENDING':
                return '💡';
            case 'TASK_PROPOSAL_APPROVED':
                return '✅';
            case 'TASK_PROPOSAL_REJECTED':
                return '❌';
            case 'STATUS_CHANGE_PENDING':
                return '🔄';
            case 'STATUS_CHANGE_APPROVED':
                return '✅';
            case 'STATUS_CHANGE_REJECTED':
                return '❌';
            case 'TASK_COMMENT_ADDED':
                return '💬';
            case 'TASK_FILE_UPLOADED':
                return '📎';
            case 'TASK_ASSIGNED':
                return '👤';
            case 'PROJECT_MEMBER_ADDED':
                return '🎯';
            default:
                return '📢';
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
        return date.toLocaleDateString('pl-PL');
    };

    return (
        <div className="relative" ref={dropdownRef}>
            <button
                onClick={() => setShowDropdown(!showDropdown)}
                className="p-2 text-gray-400 hover:text-white hover:bg-gray-800 rounded-lg transition relative"
            >
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                </svg>
                {unreadCount > 0 && (
                    <span className="absolute -top-1 -right-1 bg-red-500 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center font-bold">
                        {unreadCount > 9 ? '9+' : unreadCount}
                    </span>
                )}
            </button>

            {showDropdown && (
                <div className="absolute right-0 mt-2 w-96 bg-gray-800 rounded-lg shadow-lg border border-gray-700 z-50">
                    {/* Header */}
                    <div className="p-4 border-b border-gray-700 flex items-center justify-between">
                        <h3 className="text-white font-semibold">Powiadomienia</h3>
                        <button
                            onClick={() => navigate('/notifications')}
                            className="text-emerald-500 hover:text-emerald-400 text-sm"
                        >
                            Zobacz wszystkie
                        </button>
                    </div>

                    {/* Mark all as read */}
                    {unreadCount > 0 && (
                        <div className="p-2 border-b border-gray-700">
                            <button
                                onClick={handleMarkAllAsRead}
                                className="w-full text-left px-3 py-2 text-sm text-gray-400 hover:text-white hover:bg-gray-700 rounded transition"
                            >
                                Oznacz wszystkie jako przeczytane
                            </button>
                        </div>
                    )}

                    {/* Notifications list */}
                    <div className="max-h-96 overflow-y-auto">
                        {loading ? (
                            <div className="p-8 text-center text-gray-400">
                                <div className="animate-spin h-8 w-8 border-4 border-emerald-500 border-t-transparent rounded-full mx-auto"></div>
                                <p className="mt-2">Ładowanie...</p>
                            </div>
                        ) : notifications.length === 0 ? (
                            <div className="p-8 text-center text-gray-400">
                                <svg className="w-12 h-12 mx-auto mb-2 opacity-50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
                                </svg>
                                <p>Brak powiadomień</p>
                            </div>
                        ) : (
                            notifications.map((notification) => (
                                <div
                                    key={notification.id}
                                    onClick={() => handleNotificationClick(notification)}
                                    className={`p-4 border-b border-gray-700 hover:bg-gray-750 cursor-pointer transition ${
                                        !notification.isRead ? 'bg-gray-750' : ''
                                    }`}
                                >
                                    <div className="flex items-start gap-3">
                                        <span className="text-2xl flex-shrink-0">{getIcon(notification.type)}</span>
                                        <div className="flex-1 min-w-0">
                                            <div className="flex items-start justify-between gap-2">
                                                <h4 className={`text-sm font-medium ${!notification.isRead ? 'text-white' : 'text-gray-300'}`}>
                                                    {notification.title}
                                                </h4>
                                                {!notification.isRead && (
                                                    <span className="w-2 h-2 bg-emerald-500 rounded-full flex-shrink-0 mt-1"></span>
                                                )}
                                            </div>
                                            <p className="text-xs text-gray-400 mt-1 line-clamp-2">
                                                {notification.message}
                                            </p>
                                            <div className="flex items-center justify-between mt-2">
                                                <span className="text-xs text-gray-500">
                                                    {getTimeAgo(notification.createdAt)}
                                                </span>
                                                {!notification.isRead && (
                                                    <button
                                                        onClick={(e) => handleMarkAsRead(notification.id, e)}
                                                        className="text-xs text-emerald-500 hover:text-emerald-400"
                                                    >
                                                        Oznacz jako przeczytane
                                                    </button>
                                                )}
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            ))
                        )}
                    </div>

                    {/* Footer */}
                    {notifications.length > 0 && (
                        <div className="p-3 border-t border-gray-700 text-center">
                            <button
                                onClick={() => {
                                    setShowDropdown(false);
                                    navigate('/notifications');
                                }}
                                className="text-sm text-emerald-500 hover:text-emerald-400"
                            >
                                Zobacz wszystkie powiadomienia
                            </button>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
};

export default NotificationDropdown;