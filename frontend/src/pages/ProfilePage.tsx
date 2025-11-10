// frontend/src/pages/ProfilePage.tsx - KOMPLETNA DZIAŁAJĄCA WERSJA ✅
import React, { useState } from 'react';
import MainLayout from '../components/MainLayout';
import UserAvatar from '../components/UserAvatar';
import { useAuth } from '../context/AuthContext';

const ProfilePage: React.FC = () => {
    // ✅ Używamy AuthContext
    const { user, refreshUser } = useAuth();

    const [uploadingAvatar, setUploadingAvatar] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);

    // ✅ HANDLER UPLOADU AVATARA
    const handleAvatarUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;

        // Walidacja rozmiaru (max 5MB)
        if (file.size > 5 * 1024 * 1024) {
            setError('Plik jest za duży! Maksymalny rozmiar to 5MB.');
            return;
        }

        // Walidacja typu
        if (!file.type.startsWith('image/')) {
            setError('Można uploadować tylko obrazy (JPG, PNG, GIF)');
            return;
        }

        try {
            setUploadingAvatar(true);
            setError(null);
            setSuccess(null);

            console.log('📤 Uploading avatar...', file.name);

            const formData = new FormData();
            formData.append('file', file);

            const response = await fetch('/api/v1/users/avatar', {
                method: 'POST',
                credentials: 'include',
                body: formData,
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Upload failed');
            }

            console.log('✅ Avatar uploaded successfully!');

            // ✅ KLUCZOWE: Odśwież globalny AuthContext!
            await refreshUser();

            console.log('✅ AuthContext refreshed!');

            setSuccess('✅ Avatar został zaktualizowany!');

            // Clear success message po 3 sekundach
            setTimeout(() => setSuccess(null), 3000);

            // Clear file input
            e.target.value = '';
        } catch (err: any) {
            console.error('❌ Avatar upload error:', err);
            setError(err.message || 'Nie udało się zaktualizować avatara');
        } finally {
            setUploadingAvatar(false);
        }
    };

    if (!user) {
        return (
            <MainLayout>
                <div className="flex items-center justify-center h-96">
                    <div className="text-gray-400">Ładowanie profilu...</div>
                </div>
            </MainLayout>
        );
    }

    return (
        <MainLayout>
            <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                {/* Header */}
                <div className="mb-8">
                    <h1 className="text-3xl font-bold text-white">👤 Twój profil</h1>
                    <p className="text-gray-400 mt-1">Zarządzaj swoimi ustawieniami</p>
                </div>

                {/* Success Message */}
                {success && (
                    <div className="mb-6 p-4 bg-green-500 bg-opacity-20 border border-green-500 rounded-lg text-green-400 flex items-center gap-3">
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                        </svg>
                        {success}
                    </div>
                )}

                {/* Error Message */}
                {error && (
                    <div className="mb-6 p-4 bg-red-500 bg-opacity-20 border border-red-500 rounded-lg text-red-400 flex items-center gap-3">
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                        {error}
                    </div>
                )}

                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                    {/* ✅ Sekcja Avatara */}
                    <div className="lg:col-span-1">
                        <div className="bg-gray-900 border border-gray-800 rounded-lg p-6">
                            <div className="flex flex-col items-center">
                                {/* Avatar z przyciskiem zmiany */}
                                <div className="relative mb-4 group">
                                    <UserAvatar user={user} size="xl" />

                                    {/* Overlay z ikoną kamery przy hover */}
                                    <div className="absolute inset-0 bg-black bg-opacity-50 rounded-full opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                                        <svg className="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" />
                                        </svg>
                                    </div>

                                    {/* Przycisk do zmiany avatara */}
                                    <label
                                        htmlFor="avatar-upload"
                                        className="absolute bottom-0 right-0 bg-primary-500 hover:bg-primary-600 text-white rounded-full p-2 cursor-pointer transition-colors shadow-lg"
                                        title="Zmień avatar"
                                    >
                                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                                        </svg>
                                    </label>
                                    <input
                                        id="avatar-upload"
                                        type="file"
                                        accept="image/*"
                                        onChange={handleAvatarUpload}
                                        className="hidden"
                                        disabled={uploadingAvatar}
                                    />
                                </div>

                                {/* Loading indicator */}
                                {uploadingAvatar && (
                                    <div className="flex items-center gap-2 text-sm text-primary-400 mb-2">
                                        <svg className="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
                                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                        </svg>
                                        Uploading...
                                    </div>
                                )}

                                {/* User info */}
                                <h2 className="text-xl font-bold text-white mb-1">{user.username}</h2>
                                <span className={`px-3 py-1 rounded-full text-xs font-semibold text-white ${
                                    user.systemRole === 'SUPER_ADMIN' ? 'bg-red-500' : 'bg-blue-500'
                                }`}>
                                    {user.systemRole === 'SUPER_ADMIN' ? '👑 Super Administrator' : 'Użytkownik'}
                                </span>

                                {/* Stats */}
                                <div className="mt-6 pt-6 border-t border-gray-800 space-y-3 w-full">
                                    <div className="flex items-center justify-between text-sm">
                                        <span className="text-gray-400">Status:</span>
                                        <span className={`font-semibold ${user.active ? 'text-primary-400' : 'text-red-400'}`}>
                                            {user.active ? 'Aktywny' : 'Nieaktywny'}
                                        </span>
                                    </div>
                                    <div className="flex items-center justify-between text-sm">
                                        <span className="text-gray-400">Email:</span>
                                        <span className="text-white truncate ml-2">{user.email || 'Brak'}</span>
                                    </div>
                                </div>

                                {/* Upload hint */}

                            </div>
                        </div>
                    </div>

                    {/* ✅ Informacje podstawowe */}
                    <div className="lg:col-span-2">
                        <div className="bg-gray-900 border border-gray-800 rounded-lg p-6">
                            <h3 className="text-lg font-semibold text-white mb-6">Informacje podstawowe</h3>

                            <div className="space-y-4">
                                <div>
                                    <label className="block text-sm text-gray-400 mb-2">Nazwa użytkownika</label>
                                    <div className="flex items-center gap-3 p-3 bg-gray-800 rounded-lg">
                                        <UserAvatar user={user} size="sm" />
                                        <span className="text-white font-medium">{user.username}</span>
                                    </div>
                                </div>

                                {user.fullName && (
                                    <div>
                                        <label className="block text-sm text-gray-400 mb-2">Pełna nazwa</label>
                                        <div className="p-3 bg-gray-800 rounded-lg text-white">
                                            {user.fullName}
                                        </div>
                                    </div>
                                )}

                                {user.email && (
                                    <div>
                                        <label className="block text-sm text-gray-400 mb-2">Email</label>
                                        <div className="p-3 bg-gray-800 rounded-lg text-white">
                                            {user.email}
                                        </div>
                                    </div>
                                )}

                                <div>
                                    <label className="block text-sm text-gray-400 mb-2">Rola systemowa</label>
                                    <div className="p-3 bg-gray-800 rounded-lg">
                                        <span className={`px-3 py-1 rounded-full text-xs font-semibold text-white ${
                                            user.systemRole === 'SUPER_ADMIN' ? 'bg-red-500' : 'bg-blue-500'
                                        }`}>
                                            {user.systemRole === 'SUPER_ADMIN' ? '👑 SUPER ADMINISTRATOR' : 'USER'}
                                        </span>
                                    </div>
                                </div>

                                {/* Account dates */}
                                <div className="pt-4 border-t border-gray-800 grid grid-cols-2 gap-4">
                                    {user.createdAt && (
                                        <div>
                                            <label className="block text-sm text-gray-400 mb-1">Dołączył</label>
                                            <div className="text-white text-sm">
                                                {new Date(user.createdAt).toLocaleDateString('pl-PL')}
                                            </div>
                                        </div>
                                    )}
                                    {user.lastLogin && (
                                        <div>
                                            <label className="block text-sm text-gray-400 mb-1">Ostatnie logowanie</label>
                                            <div className="text-white text-sm">
                                                {new Date(user.lastLogin).toLocaleDateString('pl-PL')}
                                            </div>
                                        </div>
                                    )}
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