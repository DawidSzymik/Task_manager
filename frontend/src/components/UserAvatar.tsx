// frontend/src/components/UserAvatar.tsx
import React from 'react';
import type { User } from '../types';

interface UserAvatarProps {
    user: User;
    size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl';
    className?: string;
}

/**
 * Komponent wyświetlający avatar użytkownika lub inicjały
 * - Jeśli użytkownik ma avatar → wyświetla zdjęcie
 * - Jeśli nie ma avatara → wyświetla kolorowe kółko z inicjałami
 */
const UserAvatar: React.FC<UserAvatarProps> = ({ user, size = 'md', className = '' }) => {

    // Rozmiary w pikselach
    const sizes = {
        xs: 'w-6 h-6 text-xs',
        sm: 'w-8 h-8 text-sm',
        md: 'w-10 h-10 text-base',
        lg: 'w-12 h-12 text-lg',
        xl: 'w-16 h-16 text-xl',
    };

    // Pobierz inicjały użytkownika
    const getInitials = (): string => {
        if (user.fullName) {
            const names = user.fullName.trim().split(' ');
            if (names.length >= 2) {
                return (names[0][0] + names[names.length - 1][0]).toUpperCase();
            }
            return names[0].substring(0, 2).toUpperCase();
        }
        if (user.username) {
            return user.username.substring(0, 2).toUpperCase();
        }
        return '??';
    };

    // Generuj kolor na podstawie username (zawsze ten sam dla użytkownika)
    const getColorFromUsername = (): string => {
        const colors = [
            'bg-red-500',
            'bg-blue-500',
            'bg-green-500',
            'bg-yellow-500',
            'bg-purple-500',
            'bg-pink-500',
            'bg-indigo-500',
            'bg-teal-500',
            'bg-orange-500',
            'bg-cyan-500',
        ];

        // Hash username do liczby
        let hash = 0;
        const str = user.username || user.id.toString();
        for (let i = 0; i < str.length; i++) {
            hash = str.charCodeAt(i) + ((hash << 5) - hash);
        }

        const index = Math.abs(hash) % colors.length;
        return colors[index];
    };

    // Jeśli użytkownik ma avatar
    if (user.hasAvatar && user.avatarUrl) {
        return (
            <div className={`${sizes[size]} ${className} rounded-full overflow-hidden flex-shrink-0 ring-2 ring-gray-700`}>
                <img
                    src={user.avatarUrl}
                    alt={user.username}
                    className="w-full h-full object-cover"
                    onError={(e) => {
                        // Jeśli zdjęcie się nie załaduje, ukryj je
                        e.currentTarget.style.display = 'none';
                    }}
                />
            </div>
        );
    }

    // Jeśli nie ma avatara - wyświetl inicjały
    const bgColor = getColorFromUsername();
    const initials = getInitials();

    return (
        <div
            className={`${sizes[size]} ${bgColor} ${className} rounded-full flex items-center justify-center flex-shrink-0 font-semibold text-white ring-2 ring-gray-700`}
            title={user.fullName || user.username}
        >
            {initials}
        </div>
    );
};

export default UserAvatar;