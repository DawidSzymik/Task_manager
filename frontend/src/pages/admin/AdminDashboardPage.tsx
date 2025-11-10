// frontend/src/pages/admin/AdminDashboardPage.tsx
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import MainLayout from '../../components/MainLayout';
import adminService, { type UserStats } from '../../services/adminService';
import { useAuth } from '../../context/AuthContext';

const AdminDashboardPage: React.FC = () => {
    const navigate = useNavigate();
    const { user } = useAuth();
    const [stats, setStats] = useState<UserStats | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (user?.systemRole !== 'SUPER_ADMIN') {
            navigate('/dashboard');
            return;
        }
        loadStats();
    }, [user, navigate]);

    const loadStats = async () => {
        try {
            setLoading(true);
            const data = await adminService.getUserStats();
            setStats(data);
        } catch (error) {
            console.error('Nie udało się załadować statystyk:', error);
        } finally {
            setLoading(false);
        }
    };

    if (loading) {
        return (
            <MainLayout>
                <div className="flex items-center justify-center h-64">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
                </div>
            </MainLayout>
        );
    }

    return (
        <MainLayout>
            <div className="space-y-6">
                {/* Header */}
                <div className="bg-gradient-to-r from-gray-900 via-gray-800 to-gray-900 rounded-lg shadow-xl p-8 border border-gray-700">
                    <h1 className="text-4xl font-bold text-white mb-2">🔧 Panel Administracyjny</h1>
                    <p className="text-gray-400">Witaj, {user?.fullName || user?.username}! Zarządzaj swoim systemem.</p>
                </div>

                {/* Stats Cards */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    {/* Total Users */}
                    <div className="bg-gradient-to-br from-gray-800 to-gray-900 rounded-lg shadow-xl p-6 border border-gray-700 hover:border-blue-500 transition-all">
                        <div className="flex items-center justify-between">
                            <div>
                                <p className="text-sm font-medium text-gray-400">Wszyscy Użytkownicy</p>
                                <p className="text-3xl font-bold text-white mt-2">{stats?.total || 0}</p>
                            </div>
                            <div className="bg-blue-900/50 rounded-full p-3 border border-blue-600">
                                <svg
                                    className="w-8 h-8 text-blue-400"
                                    fill="none"
                                    stroke="currentColor"
                                    viewBox="0 0 24 24"
                                >
                                    <path
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        strokeWidth={2}
                                        d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"
                                    />
                                </svg>
                            </div>
                        </div>
                        <div className="mt-4">
                            <div className="flex items-center">
                                <div className="flex-1 bg-gray-700 rounded-full h-2">
                                    <div
                                        className="bg-blue-500 h-2 rounded-full transition-all duration-300"
                                        style={{ width: '100%' }}
                                    ></div>
                                </div>
                                <span className="ml-2 text-xs text-gray-400">100%</span>
                            </div>
                        </div>
                    </div>

                    {/* Active Users */}
                    <div className="bg-gradient-to-br from-gray-800 to-gray-900 rounded-lg shadow-xl p-6 border border-gray-700 hover:border-primary-500 transition-all">
                        <div className="flex items-center justify-between">
                            <div>
                                <p className="text-sm font-medium text-gray-400">Aktywni Użytkownicy</p>
                                <p className="text-3xl font-bold text-white mt-2">{stats?.active || 0}</p>
                            </div>
                            <div className="bg-primary-900/50 rounded-full p-3 border border-primary-600">
                                <svg
                                    className="w-8 h-8 text-primary-400"
                                    fill="none"
                                    stroke="currentColor"
                                    viewBox="0 0 24 24"
                                >
                                    <path
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        strokeWidth={2}
                                        d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
                                    />
                                </svg>
                            </div>
                        </div>
                        <div className="mt-4">
                            <div className="flex items-center">
                                <div className="flex-1 bg-gray-700 rounded-full h-2">
                                    <div
                                        className="bg-primary-500 h-2 rounded-full transition-all duration-300"
                                        style={{ width: `${stats?.activePercentage || 0}%` }}
                                    ></div>
                                </div>
                                <span className="ml-2 text-xs text-gray-400">
                                    {stats?.activePercentage || 0}%
                                </span>
                            </div>
                        </div>
                    </div>

                    {/* Inactive Users */}
                    <div className="bg-gradient-to-br from-gray-800 to-gray-900 rounded-lg shadow-xl p-6 border border-gray-700 hover:border-red-500 transition-all">
                        <div className="flex items-center justify-between">
                            <div>
                                <p className="text-sm font-medium text-gray-400">Nieaktywni Użytkownicy</p>
                                <p className="text-3xl font-bold text-white mt-2">{stats?.inactive || 0}</p>
                            </div>
                            <div className="bg-red-900/50 rounded-full p-3 border border-red-600">
                                <svg
                                    className="w-8 h-8 text-red-400"
                                    fill="none"
                                    stroke="currentColor"
                                    viewBox="0 0 24 24"
                                >
                                    <path
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        strokeWidth={2}
                                        d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636"
                                    />
                                </svg>
                            </div>
                        </div>
                        <div className="mt-4">
                            <div className="flex items-center">
                                <div className="flex-1 bg-gray-700 rounded-full h-2">
                                    <div
                                        className="bg-red-500 h-2 rounded-full transition-all duration-300"
                                        style={{ width: `${100 - (stats?.activePercentage || 0)}%` }}
                                    ></div>
                                </div>
                                <span className="ml-2 text-xs text-gray-400">
                                    {100 - (stats?.activePercentage || 0)}%
                                </span>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Quick Actions */}
                <div className="bg-gradient-to-br from-gray-800 to-gray-900 rounded-lg shadow-xl p-6 border border-gray-700">
                    <h2 className="text-xl font-bold text-white mb-4 flex items-center gap-2">
                        <svg className="w-6 h-6 text-primary-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                        </svg>
                        Szybkie Akcje
                    </h2>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                        <button
                            onClick={() => navigate('/admin/users')}
                            className="flex items-center gap-3 p-4 bg-gray-900 border-2 border-gray-700 rounded-lg hover:border-blue-500 hover:bg-gray-800 transition-all group"
                        >
                            <div className="bg-blue-900/50 rounded-full p-2 group-hover:bg-blue-800 transition-colors border border-blue-600">
                                <svg
                                    className="w-6 h-6 text-blue-400"
                                    fill="none"
                                    stroke="currentColor"
                                    viewBox="0 0 24 24"
                                >
                                    <path
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        strokeWidth={2}
                                        d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"
                                    />
                                </svg>
                            </div>
                            <div className="text-left">
                                <p className="font-semibold text-white">Zarządzaj Użytkownikami</p>
                                <p className="text-sm text-gray-400">Przeglądaj i edytuj wszystkich użytkowników</p>
                            </div>
                        </button>

                        <button
                            onClick={() => navigate('/admin/projects')}
                            className="flex items-center gap-3 p-4 bg-gray-900 border-2 border-gray-700 rounded-lg hover:border-primary-500 hover:bg-gray-800 transition-all group"
                        >
                            <div className="bg-primary-900/50 rounded-full p-2 group-hover:bg-primary-800 transition-colors border border-primary-600">
                                <svg
                                    className="w-6 h-6 text-primary-400"
                                    fill="none"
                                    stroke="currentColor"
                                    viewBox="0 0 24 24"
                                >
                                    <path
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        strokeWidth={2}
                                        d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"
                                    />
                                </svg>
                            </div>
                            <div className="text-left">
                                <p className="font-semibold text-white">Zarządzaj Projektami</p>
                                <p className="text-sm text-gray-400">Przeglądaj i usuwaj projekty</p>
                            </div>
                        </button>

                        <button
                            onClick={() => navigate('/admin/tasks')}
                            className="flex items-center gap-3 p-4 bg-gray-900 border-2 border-gray-700 rounded-lg hover:border-yellow-500 hover:bg-gray-800 transition-all group"
                        >
                            <div className="bg-yellow-900/50 rounded-full p-2 group-hover:bg-yellow-800 transition-colors border border-yellow-600">
                                <svg
                                    className="w-6 h-6 text-yellow-400"
                                    fill="none"
                                    stroke="currentColor"
                                    viewBox="0 0 24 24"
                                >
                                    <path
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        strokeWidth={2}
                                        d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01"
                                    />
                                </svg>
                            </div>
                            <div className="text-left">
                                <p className="font-semibold text-white">Zarządzaj Zadaniami</p>
                                <p className="text-sm text-gray-400">Przeglądaj i usuwaj zadania</p>
                            </div>
                        </button>
                    </div>
                </div>

                {/* System Info */}
                <div className="bg-gradient-to-br from-gray-800 to-gray-900 rounded-lg shadow-xl p-6 border border-gray-700">
                    <h2 className="text-xl font-bold text-white mb-4 flex items-center gap-2">
                        <svg className="w-6 h-6 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                        Informacje o Systemie
                    </h2>
                    <div className="space-y-3">
                        <div className="flex justify-between items-center py-3 border-b border-gray-700">
                            <span className="text-gray-400">Obecny Użytkownik</span>
                            <span className="font-semibold text-white">{user?.username}</span>
                        </div>
                        <div className="flex justify-between items-center py-3 border-b border-gray-700">
                            <span className="text-gray-400">Rola</span>
                            <span className="px-3 py-1 bg-purple-900/50 text-purple-300 text-sm font-semibold rounded border border-purple-600">
                                {user?.systemRole}
                            </span>
                        </div>
                        <div className="flex justify-between items-center py-3 border-b border-gray-700">
                            <span className="text-gray-400">Całkowita Liczba Zarejestrowanych Użytkowników</span>
                            <span className="font-semibold text-white">{stats?.total || 0}</span>
                        </div>
                        <div className="flex justify-between items-center py-3">
                            <span className="text-gray-400">Ostatnia Aktualizacja</span>
                            <span className="font-semibold text-white">
                                {new Date().toLocaleString('pl-PL')}
                            </span>
                        </div>
                    </div>
                </div>
            </div>
        </MainLayout>
    );
};

export default AdminDashboardPage;