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
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
                </div>
            </MainLayout>
        );
    }

    return (
        <MainLayout>
            <div className="space-y-6">
                {/* Header */}
                <div>
                    <h1 className="text-3xl font-bold text-gray-900">Admin Dashboard</h1>
                    <p className="text-gray-600 mt-1">System overview and statistics</p>
                </div>

                {/* Error Alert */}
                {error && (
                    <div className="bg-red-50 border border-red-200 text-red-800 px-4 py-3 rounded-lg">
                        {error}
                    </div>
                )}

                {/* Statistics Cards */}
                {stats && (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                        {/* Total Users */}
                        <div className="bg-white rounded-lg shadow p-6">
                            <div className="flex items-center justify-between">
                                <div>
                                    <p className="text-sm font-medium text-gray-600">Total Users</p>
                                    <p className="text-3xl font-bold text-gray-900 mt-2">{stats.total}</p>
                                </div>
                                <div className="bg-blue-100 rounded-full p-3">
                                    <svg
                                        className="w-8 h-8 text-blue-600"
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
                                <button
                                    onClick={() => navigate('/admin/users')}
                                    className="text-blue-600 hover:text-blue-800 text-sm font-medium"
                                >
                                    View all users →
                                </button>
                            </div>
                        </div>

                        {/* Active Users */}
                        <div className="bg-white rounded-lg shadow p-6">
                            <div className="flex items-center justify-between">
                                <div>
                                    <p className="text-sm font-medium text-gray-600">Active Users</p>
                                    <p className="text-3xl font-bold text-green-600 mt-2">{stats.active}</p>
                                </div>
                                <div className="bg-green-100 rounded-full p-3">
                                    <svg
                                        className="w-8 h-8 text-green-600"
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
                                    <div className="flex-1 bg-gray-200 rounded-full h-2">
                                        <div
                                            className="bg-green-600 h-2 rounded-full transition-all duration-300"
                                            style={{ width: `${stats.activePercentage}%` }}
                                        ></div>
                                    </div>
                                    <span className="ml-2 text-sm text-gray-600">
                                        {stats.activePercentage.toFixed(1)}%
                                    </span>
                                </div>
                            </div>
                        </div>

                        {/* Inactive Users */}
                        <div className="bg-white rounded-lg shadow p-6">
                            <div className="flex items-center justify-between">
                                <div>
                                    <p className="text-sm font-medium text-gray-600">Inactive Users</p>
                                    <p className="text-3xl font-bold text-gray-600 mt-2">{stats.inactive}</p>
                                </div>
                                <div className="bg-gray-100 rounded-full p-3">
                                    <svg
                                        className="w-8 h-8 text-gray-600"
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
                                    <div className="flex-1 bg-gray-200 rounded-full h-2">
                                        <div
                                            className="bg-gray-600 h-2 rounded-full transition-all duration-300"
                                            style={{ width: `${100 - stats.activePercentage}%` }}
                                        ></div>
                                    </div>
                                    <span className="ml-2 text-sm text-gray-600">
                                        {(100 - stats.activePercentage).toFixed(1)}%
                                    </span>
                                </div>
                            </div>
                        </div>

                        {/* Activity Rate */}
                        <div className="bg-white rounded-lg shadow p-6">
                            <div className="flex items-center justify-between">
                                <div>
                                    <p className="text-sm font-medium text-gray-600">Activity Rate</p>
                                    <p className="text-3xl font-bold text-purple-600 mt-2">
                                        {stats.activePercentage.toFixed(0)}%
                                    </p>
                                </div>
                                <div className="bg-purple-100 rounded-full p-3">
                                    <svg
                                        className="w-8 h-8 text-purple-600"
                                        fill="none"
                                        stroke="currentColor"
                                        viewBox="0 0 24 24"
                                    >
                                        <path
                                            strokeLinecap="round"
                                            strokeLinejoin="round"
                                            strokeWidth={2}
                                            d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"
                                        />
                                    </svg>
                                </div>
                            </div>
                            <div className="mt-4">
                                <p className="text-sm text-gray-600">
                                    {stats.activePercentage > 75
                                        ? 'Excellent'
                                        : stats.activePercentage > 50
                                            ? 'Good'
                                            : 'Needs attention'}
                                </p>
                            </div>
                        </div>
                    </div>
                )}

                {/* Quick Actions */}
                <div className="bg-white rounded-lg shadow p-6">
                    <h2 className="text-xl font-bold text-gray-900 mb-4">Quick Actions</h2>
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                        <button
                            onClick={() => navigate('/admin/users')}
                            className="flex items-center gap-3 p-4 border-2 border-gray-200 rounded-lg hover:border-blue-500 hover:bg-blue-50 transition-colors"
                        >
                            <div className="bg-blue-100 rounded-full p-2">
                                <svg
                                    className="w-6 h-6 text-blue-600"
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
                                <p className="font-semibold text-gray-900">Manage Users</p>
                                <p className="text-sm text-gray-600">View and edit all users</p>
                            </div>
                        </button>

                        <button
                            onClick={() => navigate('/admin/projects')}
                            className="flex items-center gap-3 p-4 border-2 border-gray-200 rounded-lg hover:border-green-500 hover:bg-green-50 transition-colors"
                        >
                            <div className="bg-green-100 rounded-full p-2">
                                <svg
                                    className="w-6 h-6 text-green-600"
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
                                <p className="font-semibold text-gray-900">Manage Projects</p>
                                <p className="text-sm text-gray-600">View and delete projects</p>
                            </div>
                        </button>

                        <button
                            onClick={() => navigate('/admin/tasks')}
                            className="flex items-center gap-3 p-4 border-2 border-gray-200 rounded-lg hover:border-yellow-500 hover:bg-yellow-50 transition-colors"
                        >
                            <div className="bg-yellow-100 rounded-full p-2">
                                <svg
                                    className="w-6 h-6 text-yellow-600"
                                    fill="none"
                                    stroke="currentColor"
                                    viewBox="0 0 24 24"
                                >
                                    <path
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        strokeWidth={2}
                                        d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4"
                                    />
                                </svg>
                            </div>
                            <div className="text-left">
                                <p className="font-semibold text-gray-900">Manage Tasks</p>
                                <p className="text-sm text-gray-600">View and delete tasks</p>
                            </div>
                        </button>

                        <button
                            onClick={() => {
                                navigate('/admin/users');
                                // Trigger create modal after navigation
                                setTimeout(() => {
                                    const createBtn = document.querySelector(
                                        'button:has(svg path[d*="M12 4v16m8-8H4"])'
                                    ) as HTMLButtonElement;
                                    if (createBtn) createBtn.click();
                                }, 100);
                            }}
                            className="flex items-center gap-3 p-4 border-2 border-gray-200 rounded-lg hover:border-purple-500 hover:bg-purple-50 transition-colors"
                        >
                            <div className="bg-purple-100 rounded-full p-2">
                                <svg
                                    className="w-6 h-6 text-purple-600"
                                    fill="none"
                                    stroke="currentColor"
                                    viewBox="0 0 24 24"
                                >
                                    <path
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        strokeWidth={2}
                                        d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z"
                                    />
                                </svg>
                            </div>
                            <div className="text-left">
                                <p className="font-semibold text-gray-900">Create User</p>
                                <p className="text-sm text-gray-600">Add new user account</p>
                            </div>
                        </button>

                        <button
                            onClick={() => navigate('/dashboard')}
                            className="flex items-center gap-3 p-4 border-2 border-gray-200 rounded-lg hover:border-gray-500 hover:bg-gray-50 transition-colors"
                        >
                            <div className="bg-gray-100 rounded-full p-2">
                                <svg
                                    className="w-6 h-6 text-gray-600"
                                    fill="none"
                                    stroke="currentColor"
                                    viewBox="0 0 24 24"
                                >
                                    <path
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        strokeWidth={2}
                                        d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"
                                    />
                                </svg>
                            </div>
                            <div className="text-left">
                                <p className="font-semibold text-gray-900">User Dashboard</p>
                                <p className="text-sm text-gray-600">Return to main view</p>
                            </div>
                        </button>
                    </div>
                </div>

                {/* System Info */}
                <div className="bg-white rounded-lg shadow p-6">
                    <h2 className="text-xl font-bold text-gray-900 mb-4">System Information</h2>
                    <div className="space-y-3">
                        <div className="flex justify-between items-center py-2 border-b border-gray-200">
                            <span className="text-gray-600">Current User</span>
                            <span className="font-semibold text-gray-900">{user?.username}</span>
                        </div>
                        <div className="flex justify-between items-center py-2 border-b border-gray-200">
                            <span className="text-gray-600">Role</span>
                            <span className="px-2 py-1 bg-purple-100 text-purple-800 text-sm font-semibold rounded">
                                {user?.systemRole}
                            </span>
                        </div>
                        <div className="flex justify-between items-center py-2 border-b border-gray-200">
                            <span className="text-gray-600">Total Registered Users</span>
                            <span className="font-semibold text-gray-900">{stats?.total || 0}</span>
                        </div>
                        <div className="flex justify-between items-center py-2">
                            <span className="text-gray-600">Last Updated</span>
                            <span className="font-semibold text-gray-900">
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