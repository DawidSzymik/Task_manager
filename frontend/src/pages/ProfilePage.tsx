// src/pages/ProfilePage.tsx
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import MainLayout from '../components/MainLayout';
import { useAuth } from '../context/AuthContext';

const ProfilePage: React.FC = () => {
    const { user, refreshUser } = useAuth();
    const navigate = useNavigate();
    const [isEditing, setIsEditing] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);

    const [formData, setFormData] = useState({
        fullName: user?.fullName || '',
        email: user?.email || '',
    });

    const [passwordData, setPasswordData] = useState({
        currentPassword: '',
        newPassword: '',
        confirmPassword: '',
    });

    const [showPasswordChange, setShowPasswordChange] = useState(false);

    const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setFormData({
            ...formData,
            [e.target.name]: e.target.value,
        });
    };

    const handlePasswordChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setPasswordData({
            ...passwordData,
            [e.target.name]: e.target.value,
        });
    };

    const handleUpdateProfile = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError(null);
        setSuccess(null);

        try {
            // TODO: Implement update profile API call
            // await userService.updateProfile(formData);

            // Symulacja dla demonstracji
            await new Promise(resolve => setTimeout(resolve, 1000));

            await refreshUser();
            setSuccess('Profil został zaktualizowany!');
            setIsEditing(false);
        } catch (err: any) {
            setError(err.message || 'Nie udało się zaktualizować profilu');
        } finally {
            setLoading(false);
        }
    };

    const handleChangePassword = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError(null);
        setSuccess(null);

        if (passwordData.newPassword !== passwordData.confirmPassword) {
            setError('Nowe hasła nie są identyczne');
            setLoading(false);
            return;
        }

        if (passwordData.newPassword.length < 6) {
            setError('Nowe hasło musi mieć co najmniej 6 znaków');
            setLoading(false);
            return;
        }

        try {
            // TODO: Implement change password API call
            // await authService.changePassword(passwordData);

            // Symulacja dla demonstracji
            await new Promise(resolve => setTimeout(resolve, 1000));

            setSuccess('Hasło zostało zmienione!');
            setPasswordData({
                currentPassword: '',
                newPassword: '',
                confirmPassword: '',
            });
            setShowPasswordChange(false);
        } catch (err: any) {
            setError(err.message || 'Nie udało się zmienić hasła');
        } finally {
            setLoading(false);
        }
    };

    const getRoleBadgeColor = (role?: string) => {
        if (role === 'SUPER_ADMIN') return 'bg-red-500';
        return 'bg-blue-500';
    };

    const getRoleLabel = (role?: string) => {
        if (role === 'SUPER_ADMIN') return '👑 Super Administrator';
        return 'Użytkownik';
    };

    const getInitials = (username?: string) => {
        if (!username) return 'U';
        return username.charAt(0).toUpperCase();
    };

    const formatDate = (dateString?: string) => {
        if (!dateString) return 'Brak danych';
        return new Date(dateString).toLocaleString('pl-PL', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
        });
    };

    return (
        <MainLayout>
            <div className="max-w-4xl mx-auto">
                {/* Header */}
                <div className="mb-6">
                    <h1 className="text-3xl font-bold text-white">👤 Mój Profil</h1>
                    <p className="text-gray-400 mt-1">Zarządzaj swoim kontem i ustawieniami</p>
                </div>

                {/* Success/Error Messages */}
                {success && (
                    <div className="mb-6 p-4 bg-green-500 bg-opacity-20 border border-green-500 rounded-lg text-green-400">
                        ✅ {success}
                    </div>
                )}
                {error && (
                    <div className="mb-6 p-4 bg-red-500 bg-opacity-20 border border-red-500 rounded-lg text-red-400">
                        ❌ {error}
                    </div>
                )}

                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                    {/* Profile Card */}
                    <div className="lg:col-span-1">
                        <div className="bg-gray-900 rounded-lg p-6 border border-gray-800">
                            {/* Avatar */}
                            <div className="flex flex-col items-center">
                                <div className={`w-24 h-24 ${getRoleBadgeColor(user?.systemRole)} rounded-full flex items-center justify-center mb-4`}>
                                    <span className="text-white font-bold text-3xl">
                                        {getInitials(user?.username)}
                                    </span>
                                </div>
                                <h2 className="text-xl font-bold text-white mb-1">{user?.username}</h2>
                                <span className={`px-3 py-1 rounded-full text-xs font-semibold text-white ${getRoleBadgeColor(user?.systemRole)}`}>
                                    {getRoleLabel(user?.systemRole)}
                                </span>
                            </div>

                            {/* Stats */}
                            <div className="mt-6 pt-6 border-t border-gray-800 space-y-3">
                                <div className="flex items-center justify-between text-sm">
                                    <span className="text-gray-400">Status:</span>
                                    <span className={`font-semibold ${user?.active ? 'text-green-400' : 'text-red-400'}`}>
                                        {user?.active ? '✅ Aktywny' : '❌ Nieaktywny'}
                                    </span>
                                </div>
                                <div className="flex items-center justify-between text-sm">
                                    <span className="text-gray-400">Data utworzenia:</span>
                                    <span className="text-white font-semibold">
                                        {user?.createdAt ? new Date(user.createdAt).toLocaleDateString('pl-PL') : 'Brak danych'}
                                    </span>
                                </div>
                                <div className="flex items-center justify-between text-sm">
                                    <span className="text-gray-400">Ostatnie logowanie:</span>
                                    <span className="text-white font-semibold">
                                        {user?.lastLogin ? new Date(user.lastLogin).toLocaleDateString('pl-PL') : 'Brak danych'}
                                    </span>
                                </div>
                            </div>

                            {/* Admin Panel Button */}
                            {user?.systemRole === 'SUPER_ADMIN' && (
                                <div className="mt-6">
                                    <button
                                        onClick={() => navigate('/admin')}
                                        className="w-full px-4 py-3 bg-red-500 hover:bg-red-600 text-white rounded-lg font-semibold transition-colors flex items-center justify-center gap-2"
                                    >
                                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4" />
                                        </svg>
                                        Panel Admina
                                    </button>
                                </div>
                            )}
                        </div>
                    </div>

                    {/* Profile Details & Settings */}
                    <div className="lg:col-span-2 space-y-6">
                        {/* Profile Information */}
                        <div className="bg-gray-900 rounded-lg p-6 border border-gray-800">
                            <div className="flex items-center justify-between mb-4">
                                <h3 className="text-xl font-bold text-white">Informacje Profilu</h3>
                                {!isEditing && (
                                    <button
                                        onClick={() => setIsEditing(true)}
                                        className="px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-lg transition-colors flex items-center gap-2"
                                    >
                                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                                        </svg>
                                        Edytuj
                                    </button>
                                )}
                            </div>

                            {isEditing ? (
                                <form onSubmit={handleUpdateProfile} className="space-y-4">
                                    <div>
                                        <label className="block text-sm font-medium text-gray-400 mb-2">
                                            Nazwa użytkownika
                                        </label>
                                        <input
                                            type="text"
                                            value={user?.username}
                                            disabled
                                            className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-gray-500 cursor-not-allowed"
                                        />
                                        <p className="text-xs text-gray-500 mt-1">Nazwa użytkownika nie może być zmieniona</p>
                                    </div>

                                    <div>
                                        <label className="block text-sm font-medium text-gray-400 mb-2">
                                            Imię i nazwisko
                                        </label>
                                        <input
                                            type="text"
                                            name="fullName"
                                            value={formData.fullName}
                                            onChange={handleInputChange}
                                            className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:ring-2 focus:ring-emerald-500 focus:border-transparent"
                                            placeholder="Podaj swoje imię i nazwisko"
                                        />
                                    </div>

                                    <div>
                                        <label className="block text-sm font-medium text-gray-400 mb-2">
                                            Email
                                        </label>
                                        <input
                                            type="email"
                                            name="email"
                                            value={formData.email}
                                            onChange={handleInputChange}
                                            className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:ring-2 focus:ring-emerald-500 focus:border-transparent"
                                            placeholder="twoj@email.com"
                                        />
                                    </div>

                                    <div className="flex gap-3">
                                        <button
                                            type="submit"
                                            disabled={loading}
                                            className="flex-1 px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                                        >
                                            {loading ? 'Zapisywanie...' : 'Zapisz zmiany'}
                                        </button>
                                        <button
                                            type="button"
                                            onClick={() => {
                                                setIsEditing(false);
                                                setFormData({
                                                    fullName: user?.fullName || '',
                                                    email: user?.email || '',
                                                });
                                            }}
                                            className="px-4 py-2 bg-gray-700 hover:bg-gray-600 text-white rounded-lg transition-colors"
                                        >
                                            Anuluj
                                        </button>
                                    </div>
                                </form>
                            ) : (
                                <div className="space-y-4">
                                    <div>
                                        <label className="block text-sm font-medium text-gray-400 mb-1">
                                            Nazwa użytkownika
                                        </label>
                                        <p className="text-white">{user?.username || 'Brak danych'}</p>
                                    </div>

                                    <div>
                                        <label className="block text-sm font-medium text-gray-400 mb-1">
                                            Imię i nazwisko
                                        </label>
                                        <p className="text-white">{user?.fullName || 'Nie podano'}</p>
                                    </div>

                                    <div>
                                        <label className="block text-sm font-medium text-gray-400 mb-1">
                                            Email
                                        </label>
                                        <p className="text-white">{user?.email || 'Nie podano'}</p>
                                    </div>
                                </div>
                            )}
                        </div>

                        {/* Change Password */}
                        <div className="bg-gray-900 rounded-lg p-6 border border-gray-800">
                            <div className="flex items-center justify-between mb-4">
                                <h3 className="text-xl font-bold text-white">Zmiana Hasła</h3>
                                {!showPasswordChange && (
                                    <button
                                        onClick={() => setShowPasswordChange(true)}
                                        className="px-4 py-2 bg-gray-700 hover:bg-gray-600 text-white rounded-lg transition-colors flex items-center gap-2"
                                    >
                                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z" />
                                        </svg>
                                        Zmień hasło
                                    </button>
                                )}
                            </div>

                            {showPasswordChange ? (
                                <form onSubmit={handleChangePassword} className="space-y-4">
                                    <div>
                                        <label className="block text-sm font-medium text-gray-400 mb-2">
                                            Obecne hasło
                                        </label>
                                        <input
                                            type="password"
                                            name="currentPassword"
                                            value={passwordData.currentPassword}
                                            onChange={handlePasswordChange}
                                            required
                                            className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:ring-2 focus:ring-emerald-500 focus:border-transparent"
                                            placeholder="Wpisz obecne hasło"
                                        />
                                    </div>

                                    <div>
                                        <label className="block text-sm font-medium text-gray-400 mb-2">
                                            Nowe hasło
                                        </label>
                                        <input
                                            type="password"
                                            name="newPassword"
                                            value={passwordData.newPassword}
                                            onChange={handlePasswordChange}
                                            required
                                            className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:ring-2 focus:ring-emerald-500 focus:border-transparent"
                                            placeholder="Wpisz nowe hasło"
                                        />
                                    </div>

                                    <div>
                                        <label className="block text-sm font-medium text-gray-400 mb-2">
                                            Potwierdź nowe hasło
                                        </label>
                                        <input
                                            type="password"
                                            name="confirmPassword"
                                            value={passwordData.confirmPassword}
                                            onChange={handlePasswordChange}
                                            required
                                            className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:ring-2 focus:ring-emerald-500 focus:border-transparent"
                                            placeholder="Powtórz nowe hasło"
                                        />
                                    </div>

                                    <div className="flex gap-3">
                                        <button
                                            type="submit"
                                            disabled={loading}
                                            className="flex-1 px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                                        >
                                            {loading ? 'Zmienianie...' : 'Zmień hasło'}
                                        </button>
                                        <button
                                            type="button"
                                            onClick={() => {
                                                setShowPasswordChange(false);
                                                setPasswordData({
                                                    currentPassword: '',
                                                    newPassword: '',
                                                    confirmPassword: '',
                                                });
                                            }}
                                            className="px-4 py-2 bg-gray-700 hover:bg-gray-600 text-white rounded-lg transition-colors"
                                        >
                                            Anuluj
                                        </button>
                                    </div>
                                </form>
                            ) : (
                                <p className="text-gray-400">
                                    Kliknij "Zmień hasło" aby zaktualizować swoje hasło
                                </p>
                            )}
                        </div>

                        {/* Account Info */}
                        <div className="bg-gray-900 rounded-lg p-6 border border-gray-800">
                            <h3 className="text-xl font-bold text-white mb-4">Informacje o Koncie</h3>
                            <div className="space-y-3 text-sm">
                                <div className="flex items-center justify-between py-2 border-b border-gray-800">
                                    <span className="text-gray-400">Data utworzenia:</span>
                                    <span className="text-white">{formatDate(user?.createdAt)}</span>
                                </div>
                                <div className="flex items-center justify-between py-2 border-b border-gray-800">
                                    <span className="text-gray-400">Ostatnie logowanie:</span>
                                    <span className="text-white">{formatDate(user?.lastLogin)}</span>
                                </div>
                                <div className="flex items-center justify-between py-2">
                                    <span className="text-gray-400">ID Użytkownika:</span>
                                    <span className="text-white font-mono">#{user?.id}</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </MainLayout>
    );
};

export default ProfilePage;