// src/pages/Dashboard.tsx
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import MainLayout from '../components/MainLayout';

interface Stats {
    totalProjects: number;
    totalTasks: number;
    totalTeams: number;
    completedTasks: number;
}

interface Activity {
    id: number;
    type: 'task' | 'project' | 'comment';
    title: string;
    description: string;
    time: string;
    user: string;
}

const Dashboard: React.FC = () => {
    const navigate = useNavigate();
    const { user } = useAuth();
    const [stats, setStats] = useState<Stats>({
        totalProjects: 0,
        totalTasks: 0,
        totalTeams: 0,
        completedTasks: 0,
    });
    const [activities, setActivities] = useState<Activity[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        loadDashboardData();
    }, []);

    const loadDashboardData = async () => {
        try {
            // TODO: Replace with actual API calls when endpoints are ready
            // Symulacja danych - będziemy to podłączać do API
            setTimeout(() => {
                setStats({
                    totalProjects: 5,
                    totalTasks: 23,
                    totalTeams: 3,
                    completedTasks: 12,
                });

                setActivities([
                    {
                        id: 1,
                        type: 'task',
                        title: 'Zadanie zakończone',
                        description: 'Frontend Login Page została ukończona',
                        time: '5 minut temu',
                        user: user?.username || 'User',
                    },
                    {
                        id: 2,
                        type: 'project',
                        title: 'Nowy projekt',
                        description: 'Task Manager został utworzony',
                        time: '1 godzinę temu',
                        user: user?.username || 'User',
                    },
                    {
                        id: 3,
                        type: 'comment',
                        title: 'Nowy komentarz',
                        description: 'Jan Kowalski dodał komentarz do zadania',
                        time: '2 godziny temu',
                        user: 'Jan Kowalski',
                    },
                ]);

                setLoading(false);
            }, 500);
        } catch (error) {
            console.error('Failed to load dashboard data:', error);
            setLoading(false);
        }
    };

    const getActivityIcon = (type: Activity['type']) => {
        switch (type) {
            case 'task':
                return (
                    <div className="w-10 h-10 bg-emerald-500 rounded-full flex items-center justify-center">
                        <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                        </svg>
                    </div>
                );
            case 'project':
                return (
                    <div className="w-10 h-10 bg-blue-500 rounded-full flex items-center justify-center">
                        <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
                        </svg>
                    </div>
                );
            case 'comment':
                return (
                    <div className="w-10 h-10 bg-purple-500 rounded-full flex items-center justify-center">
                        <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                        </svg>
                    </div>
                );
        }
    };

    if (loading) {
        return (
            <MainLayout>
                <div className="flex items-center justify-center h-96">
                    <div className="flex flex-col items-center">
                        <svg className="animate-spin h-12 w-12 text-emerald-500 mb-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                        </svg>
                        <p className="text-gray-400">Ładowanie danych...</p>
                    </div>
                </div>
            </MainLayout>
        );
    }

    return (
        <MainLayout>
            <div className="max-w-7xl mx-auto">
                {/* Welcome section */}
                <div className="mb-8">
                    <h1 className="text-3xl font-bold text-white mb-2">
                        Witaj, {user?.username || 'User'}! 👋
                    </h1>
                    <p className="text-gray-400">Oto przegląd Twoich projektów i zadań</p>
                </div>

                {/* Stats Cards */}
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
                    <div className="bg-gray-900 border border-gray-800 rounded-lg p-6 hover:border-emerald-500 transition-colors cursor-pointer"
                         onClick={() => navigate('/projects')}>
                        <div className="flex items-center justify-between mb-4">
                            <div className="w-12 h-12 bg-emerald-500 bg-opacity-20 rounded-lg flex items-center justify-center">
                                <svg className="w-6 h-6 text-emerald-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
                                </svg>
                            </div>
                        </div>
                        <h3 className="text-3xl font-bold text-white mb-1">{stats.totalProjects}</h3>
                        <p className="text-gray-400 text-sm">Projekty</p>
                    </div>

                    <div className="bg-gray-900 border border-gray-800 rounded-lg p-6 hover:border-blue-500 transition-colors cursor-pointer"
                         onClick={() => navigate('/tasks')}>
                        <div className="flex items-center justify-between mb-4">
                            <div className="w-12 h-12 bg-blue-500 bg-opacity-20 rounded-lg flex items-center justify-center">
                                <svg className="w-6 h-6 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                                </svg>
                            </div>
                        </div>
                        <h3 className="text-3xl font-bold text-white mb-1">{stats.totalTasks}</h3>
                        <p className="text-gray-400 text-sm">Wszystkie zadania</p>
                    </div>

                    <div className="bg-gray-900 border border-gray-800 rounded-lg p-6 hover:border-purple-500 transition-colors cursor-pointer"
                         onClick={() => navigate('/teams')}>
                        <div className="flex items-center justify-between mb-4">
                            <div className="w-12 h-12 bg-purple-500 bg-opacity-20 rounded-lg flex items-center justify-center">
                                <svg className="w-6 h-6 text-purple-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                                </svg>
                            </div>
                        </div>
                        <h3 className="text-3xl font-bold text-white mb-1">{stats.totalTeams}</h3>
                        <p className="text-gray-400 text-sm">Zespoły</p>
                    </div>

                    <div className="bg-gray-900 border border-gray-800 rounded-lg p-6 hover:border-green-500 transition-colors">
                        <div className="flex items-center justify-between mb-4">
                            <div className="w-12 h-12 bg-green-500 bg-opacity-20 rounded-lg flex items-center justify-center">
                                <svg className="w-6 h-6 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                </svg>
                            </div>
                        </div>
                        <h3 className="text-3xl font-bold text-white mb-1">{stats.completedTasks}</h3>
                        <p className="text-gray-400 text-sm">Ukończone zadania</p>
                        <div className="mt-2">
                            <div className="w-full bg-gray-700 rounded-full h-2">
                                <div
                                    className="bg-green-500 h-2 rounded-full transition-all"
                                    style={{ width: `${(stats.completedTasks / stats.totalTasks) * 100}%` }}
                                ></div>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Quick Actions */}
                <div className="bg-gray-900 border border-gray-800 rounded-lg p-6 mb-8">
                    <h2 className="text-xl font-bold text-white mb-4">Szybkie akcje</h2>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                        <button
                            onClick={() => navigate('/projects/new')}
                            className="flex items-center gap-3 p-4 bg-gray-800 hover:bg-gray-750 border border-gray-700 hover:border-emerald-500 rounded-lg transition-all"
                        >
                            <div className="w-10 h-10 bg-emerald-500 rounded-lg flex items-center justify-center">
                                <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                                </svg>
                            </div>
                            <div className="text-left">
                                <h3 className="text-white font-semibold">Nowy projekt</h3>
                                <p className="text-gray-400 text-sm">Utwórz nowy projekt</p>
                            </div>
                        </button>

                        <button
                            onClick={() => navigate('/tasks/new')}
                            className="flex items-center gap-3 p-4 bg-gray-800 hover:bg-gray-750 border border-gray-700 hover:border-blue-500 rounded-lg transition-all"
                        >
                            <div className="w-10 h-10 bg-blue-500 rounded-lg flex items-center justify-center">
                                <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                                </svg>
                            </div>
                            <div className="text-left">
                                <h3 className="text-white font-semibold">Nowe zadanie</h3>
                                <p className="text-gray-400 text-sm">Dodaj zadanie do projektu</p>
                            </div>
                        </button>

                        <button
                            onClick={() => navigate('/teams/new')}
                            className="flex items-center gap-3 p-4 bg-gray-800 hover:bg-gray-750 border border-gray-700 hover:border-purple-500 rounded-lg transition-all"
                        >
                            <div className="w-10 h-10 bg-purple-500 rounded-lg flex items-center justify-center">
                                <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                                </svg>
                            </div>
                            <div className="text-left">
                                <h3 className="text-white font-semibold">Nowy zespół</h3>
                                <p className="text-gray-400 text-sm">Stwórz zespół</p>
                            </div>
                        </button>
                    </div>
                </div>

                {/* Recent Activity */}
                <div className="bg-gray-900 border border-gray-800 rounded-lg p-6">
                    <h2 className="text-xl font-bold text-white mb-4">Ostatnia aktywność</h2>
                    <div className="space-y-4">
                        {activities.length === 0 ? (
                            <div className="text-center py-8">
                                <svg className="w-16 h-16 text-gray-600 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
                                </svg>
                                <p className="text-gray-400">Brak ostatniej aktywności</p>
                            </div>
                        ) : (
                            activities.map((activity) => (
                                <div key={activity.id} className="flex items-start gap-4 p-4 bg-gray-800 rounded-lg hover:bg-gray-750 transition-colors">
                                    {getActivityIcon(activity.type)}
                                    <div className="flex-1">
                                        <h4 className="text-white font-semibold mb-1">{activity.title}</h4>
                                        <p className="text-gray-400 text-sm mb-2">{activity.description}</p>
                                        <div className="flex items-center gap-2 text-xs text-gray-500">
                                            <span>{activity.user}</span>
                                            <span>•</span>
                                            <span>{activity.time}</span>
                                        </div>
                                    </div>
                                </div>
                            ))
                        )}
                    </div>
                </div>
            </div>
        </MainLayout>
    );
};

export default Dashboard;