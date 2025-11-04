// src/pages/Dashboard.tsx - ZAKTUALIZOWANA WERSJA
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import MainLayout from '../components/MainLayout';

interface Stats {
    userProjects: number;          // Projekty z przypisanymi zadaniami
    userTasks: number;             // Zadania przypisane
    userTeams: number;             // Zespoły
    completedTasks: number;        // Ukończone zadania
    taskCompletionRate: number;    // % ukończenia
}

interface Activity {
    id: number;
    type: string;
    description: string;
    timestamp: string;
    user: {
        username: string;
    };
}

const Dashboard: React.FC = () => {
    const navigate = useNavigate();
    const { user } = useAuth();
    const [stats, setStats] = useState<Stats | null>(null);
    const [activities, setActivities] = useState<Activity[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        loadDashboardData();
    }, []);

    const loadDashboardData = async () => {
        try {
            setLoading(true);
            setError(null);

            // Załaduj statystyki użytkownika z API
            const statsResponse = await fetch('/api/v1/dashboard/user-stats', {
                method: 'GET',
                credentials: 'include',
            });

            if (!statsResponse.ok) {
                throw new Error('Nie udało się załadować statystyk');
            }

            const statsData = await statsResponse.json();

            if (statsData.success && statsData.data) {
                console.log('📊 Dashboard stats:', statsData.data);

                // Mapuj dane z API na stan komponentu
                setStats({
                    userProjects: statsData.data.userProjects || 0,
                    userTasks: statsData.data.userTasksAssigned || 0,
                    userTeams: statsData.data.userTeams || 0,
                    completedTasks: 0, // Oblicz z completion rate
                    taskCompletionRate: statsData.data.taskCompletionRate || 0,
                });

                // Oblicz liczbę ukończonych zadań
                const assigned = statsData.data.userTasksAssigned || 0;
                const rate = statsData.data.taskCompletionRate || 0;
                const completed = Math.round((assigned * rate) / 100);

                setStats(prev => prev ? {...prev, completedTasks: completed} : null);
            }

            // Załaduj ostatnią aktywność
            const activityResponse = await fetch('/api/v1/dashboard/my-activity?limit=10', {
                method: 'GET',
                credentials: 'include',
            });

            if (activityResponse.ok) {
                const activityData = await activityResponse.json();
                if (activityData.success && activityData.data) {
                    setActivities(activityData.data);
                }
            }

        } catch (error: any) {
            console.error('Failed to load dashboard data:', error);
            setError(error.message || 'Nie udało się załadować danych dashboardu');
        } finally {
            setLoading(false);
        }
    };

    const getActivityIcon = (type: string) => {
        switch (type) {
            case 'TASK_CREATED':
                return (
                    <div className="w-10 h-10 bg-emerald-500 rounded-full flex items-center justify-center">
                        <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                        </svg>
                    </div>
                );
            case 'COMMENT_ADDED':
                return (
                    <div className="w-10 h-10 bg-blue-500 rounded-full flex items-center justify-center">
                        <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 8h10M7 12h4m1 8l-4-4H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-3l-4 4z" />
                        </svg>
                    </div>
                );
            default:
                return (
                    <div className="w-10 h-10 bg-gray-500 rounded-full flex items-center justify-center">
                        <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                    </div>
                );
        }
    };

    const formatTimestamp = (timestamp: string): string => {
        const date = new Date(timestamp);
        const now = new Date();
        const diff = now.getTime() - date.getTime();
        const hours = Math.floor(diff / (1000 * 60 * 60));

        if (hours < 1) return 'przed chwilą';
        if (hours < 24) return `${hours} ${hours === 1 ? 'godzinę' : 'godzin'} temu`;

        const days = Math.floor(hours / 24);
        return `${days} ${days === 1 ? 'dzień' : 'dni'} temu`;
    };

    if (loading) {
        return (
            <MainLayout>
                <div className="flex items-center justify-center h-screen">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-emerald-500"></div>
                </div>
            </MainLayout>
        );
    }

    if (error) {
        return (
            <MainLayout>
                <div className="bg-red-500/10 border border-red-500 text-red-500 px-4 py-3 rounded-lg">
                    {error}
                </div>
            </MainLayout>
        );
    }

    return (
        <MainLayout>
            <div className="space-y-8">
                {/* Welcome Header */}
                <div>
                    <h1 className="text-4xl font-bold text-white mb-2">
                        Witaj, {user?.username}! 👋
                    </h1>
                    <p className="text-gray-400">Oto przegląd Twoich projektów i zadań</p>
                </div>

                {/* Stats Cards */}
                {stats && (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                        {/* Projekty */}
                        <div className="bg-gray-900 border border-gray-800 rounded-lg p-6">
                            <div className="flex items-center justify-between mb-4">
                                <h3 className="text-gray-400 text-sm font-medium">Moje Projekty</h3>
                                <div className="w-10 h-10 bg-blue-500/10 rounded-lg flex items-center justify-center">
                                    <svg className="w-5 h-5 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
                                    </svg>
                                </div>
                            </div>
                            <h3 className="text-3xl font-bold text-white">{stats.userProjects}</h3>
                            <p className="text-gray-400 text-sm">Projekty z zadaniami</p>
                        </div>

                        {/* Zadania */}
                        <div className="bg-gray-900 border border-gray-800 rounded-lg p-6">
                            <div className="flex items-center justify-between mb-4">
                                <h3 className="text-gray-400 text-sm font-medium">Moje Zadania</h3>
                                <div className="w-10 h-10 bg-emerald-500/10 rounded-lg flex items-center justify-center">
                                    <svg className="w-5 h-5 text-emerald-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                                    </svg>
                                </div>
                            </div>
                            <h3 className="text-3xl font-bold text-white">{stats.userTasks}</h3>
                            <p className="text-gray-400 text-sm">Przypisane zadania</p>
                        </div>

                        {/* Zespoły */}
                        <div className="bg-gray-900 border border-gray-800 rounded-lg p-6">
                            <div className="flex items-center justify-between mb-4">
                                <h3 className="text-gray-400 text-sm font-medium">Zespoły</h3>
                                <div className="w-10 h-10 bg-purple-500/10 rounded-lg flex items-center justify-center">
                                    <svg className="w-5 h-5 text-purple-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                                    </svg>
                                </div>
                            </div>
                            <h3 className="text-3xl font-bold text-white">{stats.userTeams}</h3>
                            <p className="text-gray-400 text-sm">Moje zespoły</p>
                        </div>

                        {/* Ukończone */}
                        <div className="bg-gray-900 border border-gray-800 rounded-lg p-6">
                            <div className="flex items-center justify-between mb-4">
                                <h3 className="text-gray-400 text-sm font-medium">Ukończone</h3>
                                <div className="w-10 h-10 bg-green-500/10 rounded-lg flex items-center justify-center">
                                    <svg className="w-5 h-5 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                                    </svg>
                                </div>
                            </div>
                            <h3 className="text-3xl font-bold text-white">{stats.completedTasks}</h3>
                            <p className="text-gray-400 text-sm">{stats.taskCompletionRate.toFixed(0)}% zadań</p>
                            <div className="mt-2">
                                <div className="w-full bg-gray-700 rounded-full h-2">
                                    <div
                                        className="bg-green-500 h-2 rounded-full transition-all"
                                        style={{ width: `${stats.taskCompletionRate}%` }}
                                    ></div>
                                </div>
                            </div>
                        </div>
                    </div>
                )}

                {/* Quick Actions */}
                <div className="bg-gray-900 border border-gray-800 rounded-lg p-6 mb-8">
                    <h2 className="text-xl font-bold text-white mb-4">Szybkie akcje</h2>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                        <button
                            onClick={() => navigate('/projects')}
                            className="flex items-center gap-3 p-4 bg-gray-800 hover:bg-gray-750 border border-gray-700 hover:border-emerald-500 rounded-lg transition-all"
                        >
                            <div className="w-10 h-10 bg-emerald-500 rounded-lg flex items-center justify-center">
                                <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
                                </svg>
                            </div>
                            <div className="text-left">
                                <h3 className="text-white font-semibold">Projekty</h3>
                                <p className="text-gray-400 text-sm">Przejdź do projektów</p>
                            </div>
                        </button>

                        <button
                            onClick={() => navigate('/tasks')}
                            className="flex items-center gap-3 p-4 bg-gray-800 hover:bg-gray-750 border border-gray-700 hover:border-blue-500 rounded-lg transition-all"
                        >
                            <div className="w-10 h-10 bg-blue-500 rounded-lg flex items-center justify-center">
                                <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                                </svg>
                            </div>
                            <div className="text-left">
                                <h3 className="text-white font-semibold">Zadania</h3>
                                <p className="text-gray-400 text-sm">Zobacz zadania</p>
                            </div>
                        </button>

                        <button
                            onClick={() => navigate('/teams')}
                            className="flex items-center gap-3 p-4 bg-gray-800 hover:bg-gray-750 border border-gray-700 hover:border-purple-500 rounded-lg transition-all"
                        >
                            <div className="w-10 h-10 bg-purple-500 rounded-lg flex items-center justify-center">
                                <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                                </svg>
                            </div>
                            <div className="text-left">
                                <h3 className="text-white font-semibold">Zespoły</h3>
                                <p className="text-gray-400 text-sm">Zobacz zespoły</p>
                            </div>
                        </button>
                    </div>
                </div>

                {/* Recent Activity */}
                {activities.length > 0 && (
                    <div className="bg-gray-900 border border-gray-800 rounded-lg p-6">
                        <h2 className="text-xl font-bold text-white mb-4">Ostatnia aktywność</h2>
                        <div className="space-y-4">
                            {activities.map((activity) => (
                                <div key={activity.id} className="flex items-start gap-4 p-4 bg-gray-800 rounded-lg">
                                    {getActivityIcon(activity.type)}
                                    <div className="flex-1">
                                        <p className="text-white font-medium">{activity.description}</p>
                                        <p className="text-gray-400 text-sm mt-1">
                                            {activity.user.username} • {formatTimestamp(activity.timestamp)}
                                        </p>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                )}
            </div>
        </MainLayout>
    );
};

export default Dashboard;