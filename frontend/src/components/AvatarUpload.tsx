// frontend/src/components/AvatarUpload.tsx
import React, { useState, useRef } from 'react';
import UserAvatar from './UserAvatar';
import type { User } from '../types';

interface AvatarUploadProps {
    user: User;
    onAvatarUpdated: () => void; // Callback po aktualizacji avatara
}

/**
 * Komponent do uploadu i zarządzania avatarem użytkownika
 * Wyświetla obecny avatar oraz przyciski do zmiany/usunięcia
 */
const AvatarUpload: React.FC<AvatarUploadProps> = ({ user, onAvatarUpdated }) => {
    const [uploading, setUploading] = useState(false);
    const [deleting, setDeleting] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const fileInputRef = useRef<HTMLInputElement>(null);

    const handleFileSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;

        // Walidacja rozmiaru (5MB max)
        if (file.size > 5 * 1024 * 1024) {
            setError('Plik jest za duży. Maksymalny rozmiar: 5MB');
            return;
        }

        // Walidacja typu
        if (!file.type.startsWith('image/')) {
            setError('Nieprawidłowy typ pliku. Wybierz obraz (JPG, PNG, GIF, WEBP)');
            return;
        }

        try {
            setUploading(true);
            setError(null);

            const formData = new FormData();
            formData.append('file', file);

            const response = await fetch('/api/v1/users/profile/avatar', {
                method: 'POST',
                credentials: 'include',
                body: formData,
            });

            const data = await response.json();

            if (!response.ok || !data.success) {
                throw new Error(data.error || 'Nie udało się zapisać avatara');
            }

            // Odśwież dane użytkownika
            onAvatarUpdated();

        } catch (error: any) {
            console.error('Upload avatar error:', error);
            setError(error.message || 'Nie udało się zapisać avatara');
        } finally {
            setUploading(false);
            // Reset input
            if (fileInputRef.current) {
                fileInputRef.current.value = '';
            }
        }
    };

    const handleDeleteAvatar = async () => {
        if (!window.confirm('Czy na pewno chcesz usunąć swój avatar?')) {
            return;
        }

        try {
            setDeleting(true);
            setError(null);

            const response = await fetch('/api/v1/users/profile/avatar', {
                method: 'DELETE',
                credentials: 'include',
            });

            const data = await response.json();

            if (!response.ok || !data.success) {
                throw new Error(data.error || 'Nie udało się usunąć avatara');
            }

            // Odśwież dane użytkownika
            onAvatarUpdated();

        } catch (error: any) {
            console.error('Delete avatar error:', error);
            setError(error.message || 'Nie udało się usunąć avatara');
        } finally {
            setDeleting(false);
        }
    };

    return (
        <div className="bg-gray-900 border border-gray-800 rounded-lg p-6">
            <h3 className="text-lg font-semibold text-white mb-4">Zdjęcie profilowe</h3>

            <div className="flex items-center gap-6">
                {/* Podgląd avatara */}
                <UserAvatar user={user} size="xl" />

                {/* Przyciski */}
                <div className="flex-1">
                    <div className="flex gap-3 mb-3">
                        {/* Przycisk upload */}
                        <button
                            onClick={() => fileInputRef.current?.click()}
                            disabled={uploading || deleting}
                            className="px-4 py-2 bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                        >
                            {uploading ? 'Wysyłanie...' : user.hasAvatar ? 'Zmień zdjęcie' : 'Dodaj zdjęcie'}
                        </button>

                        {/* Przycisk usuń (tylko jeśli ma avatar) */}
                        {user.hasAvatar && (
                            <button
                                onClick={handleDeleteAvatar}
                                disabled={uploading || deleting}
                                className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                            >
                                {deleting ? 'Usuwanie...' : 'Usuń zdjęcie'}
                            </button>
                        )}
                    </div>

                    {/* Info */}
                    <p className="text-xs text-gray-400">
                        Dozwolone formaty: JPG, PNG, GIF, WEBP. Maksymalny rozmiar: 5MB
                    </p>

                    {/* Error message */}
                    {error && (
                        <div className="mt-3 text-sm text-red-400">
                            {error}
                        </div>
                    )}
                </div>
            </div>

            {/* Hidden file input */}
            <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                onChange={handleFileSelect}
                className="hidden"
            />
        </div>
    );
};

export default AvatarUpload;