// frontend/src/pages/Dashboard.tsx - NAPRAWIONA WERSJA (z key props)
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import MainLayout from '../components/MainLayout';

interface Stats {
    userProjects: number;
    userTasks: number;
    userTeams: number;
    completedTasks: number;
    taskCompletionRate: number;
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

                const assigned = statsData.data.userTasksAssigned || 0;
                const rate = statsData.data.taskCompletionRate || 0;
                const completed = Math.round((assigned * rate) / 100);

                setStats({
                    userProjects: statsData.data.userProjects || 0,
                    userTasks: statsData.data.userTasksAssigned || 0,
                    userTeams: statsData.data.userTeams || 0,
                    completedTasks: completed,
                    taskCompletionRate: statsData.data.taskCompletionRate || 0,
                });
            }

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
                    <div className="w-10 h-10 bg-emerald-500 rounded-full flex items-center justify-center flex-shrink-0">
                        <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                        </svg>
                    </div>
                );
            case 'COMMENT_ADDED':
                return (
                    <div className="w-10 h-10 bg-blue-500 rounded-full flex items-center justify-center flex-shrink-0">
                        <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 8h10M7 12h4m1 8l-4-4H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-3l-4 4z" />
                        </svg>
                    </div>
                );
            default:
                return (
                    <div className="w-10 h-10 bg-gray-500 rounded-full flex items-center justify-center flex-shrink-0">
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

    // ✅ FIX: Dane statystyk jako tablica z key
    const statsCards = stats ? [
        {
            key: 'projects',
            title: 'Moje Projekty',
            value: stats.userProjects,
            subtitle: 'Projekty z zadaniami',
            color: 'blue',
            icon: (
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
                </svg>
            )
        },
        {
            key: 'tasks',
            title: 'Moje Zadania',
            value: stats.userTasks,
            subtitle: 'Przypisane zadania',
            color: 'emerald',
            icon: (
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                </svg>
            )
        },
        {
            key: 'teams',
            title: 'Zespoły',
            value: stats.userTeams,
            subtitle: 'Moje zespoły',
            color: 'purple',
            icon: (
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                </svg>
            )
        },
        {
            key: 'completed',
            title: 'Ukończone',
            value: stats.completedTasks,
            subtitle: `${stats.taskCompletionRate.toFixed(0)}% zadań`,
            color: 'green',
            showProgress: true,
            progressValue: stats.taskCompletionRate,
            icon: (
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
            )
        }
    ] : [];

    // ✅ FIX: Szybkie akcje jako tablica z key
    const quickActions = [
        {
            key: 'projects',
            title: 'Projekty',
            subtitle: 'Przeglądaj projekty',
            color: 'emerald',
            onClick: () => navigate('/projects'),
            icon: (
                <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
                </svg>
            )
        },
        {
            key: 'tasks',
            title: 'Zadania',
            subtitle: 'Zobacz zadania',
            color: 'blue',
            onClick: () => navigate('/tasks'),
            icon: (
                <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                </svg>
            )
        },
        {
            key: 'teams',
            title: 'Zespoły',
            subtitle: 'Zobacz zespoły',
            color: 'purple',
            onClick: () => navigate('/teams'),
            icon: (
                <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                </svg>
            )
        }
    ];

    if (loading) {
        return (
            <MainLayout>
                <div className="flex items-center justify-center h-96">
                    <div className="text-gray-400">Ładowanie dashboardu...</div>
                </div>
            </MainLayout>
        );
    }

    return (
        <MainLayout>
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
                {/* Header */}
                <div className="mb-8">
                    <h1 className="text-3xl font-bold text-white">
                        Witaj, {user?.username || 'Użytkowniku'}! 👋
                    </h1>
                    <p className="text-gray-400">Oto przegląd Twoich projektów i zadań</p>
                </div>

                {/* Error Message */}
                {error && (
                    <div className="bg-red-900 border border-red-700 text-red-100 px-4 py-3 rounded-lg">
                        {error}
                    </div>
                )}

                {/* Stats Cards - ✅ Z KEY PROP */}
                {stats && statsCards.length > 0 && (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                        {statsCards.map((card) => (
                            <div key={card.key} className="bg-gray-900 border border-gray-800 rounded-lg p-6">
                                <div className="flex items-center justify-between mb-4">
                                    <h3 className="text-gray-400 text-sm font-medium">{card.title}</h3>
                                    <div className={`w-10 h-10 bg-${card.color}-500/10 rounded-lg flex items-center justify-center`}>
                                        <div className={`text-${card.color}-500`}>
                                            {card.icon}
                                        </div>
                                    </div>
                                </div>
                                <h3 className="text-3xl font-bold text-white">{card.value}</h3>
                                <p className="text-gray-400 text-sm">{card.subtitle}</p>

                                {/* Progress Bar (tylko dla ukończonych zadań) */}
                                {card.showProgress && (
                                    <div className="mt-2">
                                        <div className="w-full bg-gray-700 rounded-full h-2">
                                            <div
                                                className="bg-green-500 h-2 rounded-full transition-all"
                                                style={{ width: `${card.progressValue}%` }}
                                            ></div>
                                        </div>
                                    </div>
                                )}
                            </div>
                        ))}
                    </div>
                )}

                {/* Quick Actions - ✅ Z KEY PROP */}
                <div className="bg-gray-900 border border-gray-800 rounded-lg p-6">
                    <h2 className="text-xl font-bold text-white mb-4">Szybkie akcje</h2>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                        {quickActions.map((action) => (
                            <button
                                key={action.key}
                                onClick={action.onClick}
                                className="flex items-center gap-3 p-4 bg-gray-800 hover:bg-gray-750 border border-gray-700 hover:border-emerald-500 rounded-lg transition-all"
                            >
                                <div className={`w-10 h-10 bg-${action.color}-500 rounded-lg flex items-center justify-center`}>
                                    {action.icon}
                                </div>
                                <div className="text-left">
                                    <h3 className="text-white font-semibold">{action.title}</h3>
                                    <p className="text-gray-400 text-sm">{action.subtitle}</p>
                                </div>
                            </button>
                        ))}
                    </div>
                </div>

                {/* Recent Activity - ✅ JUŻ MA KEY PROP */}
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