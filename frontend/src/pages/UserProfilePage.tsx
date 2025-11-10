// frontend/src/pages/UserProfilePage.tsx
import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import MainLayout from '../components/MainLayout';
import userService from '../services/userService';
import type { User } from '../types';

const UserProfilePage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const [user, setUser] = useState<User | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (id) {
            loadUserProfile();
        }
    }, [id]);

    const loadUserProfile = async () => {
        try {
            setLoading(true);
            setError(null);

            const userData = await userService.getUserById(parseInt(id!));
            setUser(userData);
        } catch (err: any) {
            setError(err.message || 'Wystąpił błąd podczas ładowania profilu');
        } finally {
            setLoading(false);
        }
    };

    if (loading) {
        return (
            <MainLayout>
                <div className="flex items-center justify-center h-96">
                    <div className="flex flex-col items-center">
                        <svg className="animate-spin h-12 w-12 text-primary-500 mb-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                        </svg>
                        <p className="text-gray-400">Ładowanie profilu...</p>
                    </div>
                </div>
            </MainLayout>
        );
    }

    if (error || !user) {
        return (
            <MainLayout>
                <div className="max-w-4xl mx-auto">
                    <div className="bg-red-900/20 border border-red-500/50 rounded-lg p-6 mb-6">
                        <div className="flex items-center gap-3">
                            <svg className="w-6 h-6 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                            <div>
                                <h3 className="text-red-500 font-semibold">Błąd</h3>
                                <p className="text-red-400">{error || 'Nie znaleziono użytkownika'}</p>
                            </div>
                        </div>
                    </div>
                    <button
                        onClick={() => navigate(-1)}
                        className="px-4 py-2 bg-gray-800 hover:bg-gray-700 text-white rounded-lg transition"
                    >
                        ← Powrót
                    </button>
                </div>
            </MainLayout>
        );
    }

    return (
        <MainLayout>
            <div className="max-w-4xl mx-auto">
                {/* Header */}
                <div className="mb-6">
                    <button
                        onClick={() => navigate(-1)}
                        className="flex items-center gap-2 text-gray-400 hover:text-white transition mb-4"
                    >
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
                        </svg>
                        Powrót
                    </button>
                    <h1 className="text-3xl font-bold text-white">Profil Użytkownika</h1>
                </div>

                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                    {/* User Card */}
                    <div className="lg:col-span-1">
                        <div className="bg-gray-900 rounded-lg p-6 border border-gray-800">
                            {/* Avatar */}
                            <div className="flex flex-col items-center mb-6">
                                <div className="w-32 h-32 bg-primary-500 rounded-full flex items-center justify-center mb-4">
                                    <span className="text-white text-5xl font-bold">
                                        {user.username.charAt(0).toUpperCase()}
                                    </span>
                                </div>
                                <h2 className="text-2xl font-bold text-white text-center">{user.username}</h2>
                                <p className="text-gray-400 text-sm mt-1">{user.email}</p>
                                <span className={`mt-3 px-3 py-1 rounded-full text-xs font-semibold ${
                                    user.systemRole === 'SUPER_ADMIN'
                                        ? 'bg-red-500/20 text-red-400'
                                        : 'bg-blue-500/20 text-blue-400'
                                }`}>
                                    {user.systemRole === 'SUPER_ADMIN' ? 'Administrator' : 'Użytkownik'}
                                </span>
                                <span className={`mt-2 px-3 py-1 rounded-full text-xs font-semibold ${
                                    user.active
                                        ? 'bg-primary-500/20 text-primary-400'
                                        : 'bg-gray-500/20 text-gray-400'
                                }`}>
                                    {user.active ? 'Aktywny' : 'Nieaktywny'}
                                </span>
                            </div>

                            {/* User Stats */}
                            <div className="space-y-3 pt-6 border-t border-gray-800">
                                <div className="flex items-center justify-between text-sm">
                                    <span className="text-gray-400">Dołączył:</span>
                                    <span className="text-white font-semibold">
                                        {user.createdAt ? new Date(user.createdAt).toLocaleDateString('pl-PL') : 'Brak danych'}
                                    </span>
                                </div>
                                {user.fullName && (
                                    <div className="flex items-center justify-between text-sm">
                                        <span className="text-gray-400">Imię i nazwisko:</span>
                                        <span className="text-white font-semibold">{user.fullName}</span>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>

                    {/* User Information */}
                    <div className="lg:col-span-2">
                        <div className="bg-gray-900 rounded-lg p-6 border border-gray-800">
                            <h3 className="text-xl font-bold text-white mb-4">Informacje</h3>

                            <div className="space-y-4">
                                <div className="border-b border-gray-800 pb-4">
                                    <label className="text-sm text-gray-400 block mb-1">Nazwa użytkownika</label>
                                    <p className="text-white font-medium">{user.username}</p>
                                </div>

                                <div className="border-b border-gray-800 pb-4">
                                    <label className="text-sm text-gray-400 block mb-1">Email</label>
                                    <p className="text-white font-medium">{user.email}</p>
                                </div>

                                {user.fullName && (
                                    <div className="border-b border-gray-800 pb-4">
                                        <label className="text-sm text-gray-400 block mb-1">Imię i nazwisko</label>
                                        <p className="text-white font-medium">{user.fullName}</p>
                                    </div>
                                )}

                                <div className="border-b border-gray-800 pb-4">
                                    <label className="text-sm text-gray-400 block mb-1">Rola systemowa</label>
                                    <p className="text-white font-medium">
                                        {user.systemRole === 'SUPER_ADMIN' ? 'Administrator' : 'Użytkownik'}
                                    </p>
                                </div>

                                <div className="border-b border-gray-800 pb-4">
                                    <label className="text-sm text-gray-400 block mb-1">Status konta</label>
                                    <p className={`font-medium ${user.active ? 'text-primary-400' : 'text-gray-400'}`}>
                                        {user.active ? 'Aktywne' : 'Nieaktywne'}
                                    </p>
                                </div>

                                <div>
                                    <label className="text-sm text-gray-400 block mb-1">Data dołączenia</label>
                                    <p className="text-white font-medium">
                                        {user.createdAt ? new Date(user.createdAt).toLocaleString('pl-PL') : 'Brak danych'}
                                    </p>
                                </div>
                            </div>
                        </div>

                        {/* Additional info about viewing other profiles */}
                        <div className="mt-6 bg-blue-900/20 border border-blue-500/50 rounded-lg p-4">
                            <div className="flex items-start gap-3">
                                <svg className="w-5 h-5 text-blue-400 mt-0.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                                </svg>
                                <div>
                                    <p className="text-blue-300 text-sm">
                                        Przeglądasz profil innego użytkownika. Aby zobaczyć i edytować swój profil, przejdź do zakładki "Profil" w menu.
                                    </p>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </MainLayout>
    );
};

export default UserProfilePage;