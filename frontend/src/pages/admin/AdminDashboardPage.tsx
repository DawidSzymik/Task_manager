// frontend/src/pages/admin/AdminDashboardPage.tsx
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import MainLayout from '../../components/MainLayout';
import adminService, {type UserStats } from '../../services/adminService';
import { useAuth } from '../../context/AuthContext';

const AdminDashboardPage: React.FC = () => {
    const navigate = useNavigate();
    const { user } = useAuth();
    const [stats, setStats] = useState<UserStats | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        // Check if user is super admin
        if (user?.systemRole !== 'SUPER_ADMIN') {
            navigate('/dashboard');
            return;
        }
        loadStats();
    }, [user, navigate]);

    const loadStats = async () => {
        try {
            setLoading(true);
            setError(null);
            const data = await adminService.getUserStats();
            setStats(data);
        } catch (err: any) {
            setError(err.message || 'Failed to load statistics');
        } finally {
            setLoading(false);
        }
    };

    if (loading) {
        return (
            <MainLayout>
                <div className="flex items-center justify-center h-64">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-purple-600"></div>
                </div>
            </MainLayout>
        );
    }

    return (
        <MainLayout>
            <div className="space-y-6">
                {/* Header */}
                <div className="bg-gradient-to-r from-purple-900 to-purple-700 rounded-lg p-6 shadow-lg border border-purple-600">
                    <h1 className="text-3xl font-bold text-white flex items-center gap-3">
                        <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                        </svg>
                        Admin Dashboard
                    </h1>
                    <p className="text-purple-200 mt-2">System overview and statistics</p>
                </div>

                {/* Error Alert */}
                {error && (
                    <div className="bg-red-900/50 border border-red-600 text-red-200 px-4 py-3 rounded-lg">
                        {error}
                    </div>
                )}

                {/* Statistics Cards */}
                {stats && (
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                        {/* Total Users */}
                        <div className="bg-gradient-to-br from-gray-800 to-gray-900 rounded-lg shadow-xl p-6 border border-gray-700 hover:border-blue-500 transition-all">
                            <div className="flex items-center justify-between">
                                <div>
                                    <p className="text-sm font-medium text-gray-400">Total Users</p>
                                    <p className="text-3xl font-bold text-white mt-2">{stats.total}</p>
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
                        <div className="bg-gradient-to-br from-gray-800 to-gray-900 rounded-lg shadow-xl p-6 border border-gray-700 hover:border-emerald-500 transition-all">
                            <div className="flex items-center justify-between">
                                <div>
                                    <p className="text-sm font-medium text-gray-400">Active Users</p>
                                    <p className="text-3xl font-bold text-white mt-2">{stats.active}</p>
                                </div>
                                <div className="bg-emerald-900/50 rounded-full p-3 border border-emerald-600">
                                    <svg
                                        className="w-8 h-8 text-emerald-400"
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
                                            className="bg-emerald-500 h-2 rounded-full transition-all duration-300"
                                            style={{ width: `${stats.activePercentage}%` }}
                                        ></div>
                                    </div>
                                    <span className="ml-2 text-xs text-emerald-400 font-semibold">
                                        {stats.activePercentage.toFixed(1)}%
                                    </span>
                                </div>
                                <p className="text-xs text-gray-500 mt-2">
                                    Status: {stats.activePercentage > 75
                                    ? 'Excellent'
                                    : stats.activePercentage > 50
                                        ? 'Good'
                                        : 'Needs attention'}
                                </p>
                            </div>
                        </div>

                        {/* Inactive Users */}
                        <div className="bg-gradient-to-br from-gray-800 to-gray-900 rounded-lg shadow-xl p-6 border border-gray-700 hover:border-red-500 transition-all">
                            <div className="flex items-center justify-between">
                                <div>
                                    <p className="text-sm font-medium text-gray-400">Inactive Users</p>
                                    <p className="text-3xl font-bold text-white mt-2">{stats.inactive}</p>
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
                                            style={{ width: `${100 - stats.activePercentage}%` }}
                                        ></div>
                                    </div>
                                    <span className="ml-2 text-xs text-red-400 font-semibold">
                                        {(100 - stats.activePercentage).toFixed(1)}%
                                    </span>
                                </div>
                            </div>
                        </div>
                    </div>
                )}

                {/* Quick Actions */}
                <div className="bg-gradient-to-br from-gray-800 to-gray-900 rounded-lg shadow-xl p-6 border border-gray-700">
                    <h2 className="text-xl font-bold text-white mb-6 flex items-center gap-2">
                        <svg className="w-6 h-6 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                        </svg>
                        Quick Actions
                    </h2>
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
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
                                <p className="font-semibold text-white">Manage Users</p>
                                <p className="text-sm text-gray-400">View and edit all users</p>
                            </div>
                        </button>

                        <button
                            onClick={() => navigate('/admin/projects')}
                            className="flex items-center gap-3 p-4 bg-gray-900 border-2 border-gray-700 rounded-lg hover:border-emerald-500 hover:bg-gray-800 transition-all group"
                        >
                            <div className="bg-emerald-900/50 rounded-full p-2 group-hover:bg-emerald-800 transition-colors border border-emerald-600">
                                <svg
                                    className="w-6 h-6 text-emerald-400"
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
                                <p className="font-semibold text-white">Manage Projects</p>
                                <p className="text-sm text-gray-400">View and delete projects</p>
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
                                        d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"
                                    />
                                </svg>
                            </div>
                            <div className="text-left">
                                <p className="font-semibold text-white">Manage Tasks</p>
                                <p className="text-sm text-gray-400">View and delete tasks</p>
                            </div>
                        </button>

                        <button
                            onClick={() => navigate('/dashboard')}
                            className="flex items-center gap-3 p-4 bg-gray-900 border-2 border-gray-700 rounded-lg hover:border-purple-500 hover:bg-gray-800 transition-all group"
                        >
                            <div className="bg-purple-900/50 rounded-full p-2 group-hover:bg-purple-800 transition-colors border border-purple-600">
                                <svg
                                    className="w-6 h-6 text-purple-400"
                                    fill="none"
                                    stroke="currentColor"
                                    viewBox="0 0 24 24"
                                >
                                    <path
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        strokeWidth={2}
                                        d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"
                                    />
                                </svg>
                            </div>
                            <div className="text-left">
                                <p className="font-semibold text-white">User Dashboard</p>
                                <p className="text-sm text-gray-400">Return to main view</p>
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
                        System Information
                    </h2>
                    <div className="space-y-3">
                        <div className="flex justify-between items-center py-3 border-b border-gray-700">
                            <span className="text-gray-400">Current User</span>
                            <span className="font-semibold text-white">{user?.username}</span>
                        </div>
                        <div className="flex justify-between items-center py-3 border-b border-gray-700">
                            <span className="text-gray-400">Role</span>
                            <span className="px-3 py-1 bg-purple-900/50 text-purple-300 text-sm font-semibold rounded border border-purple-600">
                                {user?.systemRole}
                            </span>
                        </div>
                        <div className="flex justify-between items-center py-3 border-b border-gray-700">
                            <span className="text-gray-400">Total Registered Users</span>
                            <span className="font-semibold text-white">{stats?.total || 0}</span>
                        </div>
                        <div className="flex justify-between items-center py-3">
                            <span className="text-gray-400">Last Updated</span>
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