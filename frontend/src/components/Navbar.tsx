// src/components/Navbar.tsx - Z NOWYM LOGO
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import NotificationDropdown from './NotificationDropdown';
import GlobalSearch from './GlobalSearch';
import UserAvatar from './UserAvatar';

const Navbar: React.FC = () => {
    const navigate = useNavigate();
    const { user, logout } = useAuth();
    const [showDropdown, setShowDropdown] = useState(false);
    const [showSearch, setShowSearch] = useState(false);

    const handleLogout = async () => {
        try {
            await logout();
            navigate('/login');
        } catch (error) {
            console.error('Logout failed:', error);
        }
    };

    return (
        <>
            <nav className="bg-gray-900 border-b border-gray-800 sticky top-0 z-50">
                <div className="max-w-full mx-auto px-4 sm:px-6 lg:px-8">
                    <div className="flex items-center justify-between h-16">
                        {/* Logo - NOWY DESIGN */}
                        <div className="flex items-center gap-4">
                            <div
                                className="flex-shrink-0 cursor-pointer"
                                onClick={() => navigate('/dashboard')}
                            >
                                <div className="flex items-center">
                                    {/* TASK - białe na czarnym tle */}
                                    <div className="bg-black px-4 py-2 rounded-l">
                                        <span className="text-white text-2xl font-bold tracking-wide">
                                            TASK
                                        </span>
                                    </div>
                                    {/* MANAGER - cyan/turkusowe */}
                                    <div className="bg-gray-800 px-4 py-2 rounded-r">
                                        <span className="text-cyan-400 text-2xl font-bold tracking-wide">
                                            MANAGER
                                        </span>
                                    </div>
                                </div>
                            </div>

                            {/* Search Button - 50% szerszy */}
                            <button
                                onClick={() => setShowSearch(true)}
                                className="flex items-center gap-2 px-4 py-2 bg-gray-800 hover:bg-gray-700 rounded-lg text-gray-400 hover:text-white transition min-w-[240px]"
                            >
                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                                </svg>
                                <span className="text-sm">Szukaj...</span>
                            </button>
                        </div>

                        {/* Right side - User menu */}
                        <div className="flex items-center gap-4">
                            {/* Notifications */}
                            <NotificationDropdown />

                            {/* User profile dropdown z UserAvatar */}
                            <div className="relative">
                                <button
                                    onClick={() => setShowDropdown(!showDropdown)}
                                    className="flex items-center gap-3 p-2 rounded-lg hover:bg-gray-800 transition"
                                >
                                    <UserAvatar
                                        user={user || { id: 0, username: 'User' }}
                                        size="sm"
                                    />
                                    <div className="text-left hidden sm:block">
                                        <p className="text-sm font-medium text-white">{user?.username || 'User'}</p>
                                        <p className="text-xs text-gray-400">{user?.systemRole || 'USER'}</p>
                                    </div>
                                    <svg
                                        className={`w-4 h-4 text-gray-400 transition-transform ${showDropdown ? 'rotate-180' : ''}`}
                                        fill="none"
                                        stroke="currentColor"
                                        viewBox="0 0 24 24"
                                    >
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                                    </svg>
                                </button>

                                {/* Dropdown Menu */}
                                {showDropdown && (
                                    <div className="absolute right-0 mt-2 w-56 bg-gray-800 border border-gray-700 rounded-lg shadow-xl py-1 z-50">
                                        <div className="px-4 py-3 border-b border-gray-700">
                                            <p className="text-sm font-medium text-white">{user?.fullName || user?.username}</p>
                                            <p className="text-xs text-gray-400 truncate">{user?.email}</p>
                                        </div>

                                        <button
                                            onClick={() => {
                                                navigate('/profile');
                                                setShowDropdown(false);
                                            }}
                                            className="w-full text-left px-4 py-2 text-sm text-gray-300 hover:bg-gray-700 hover:text-white flex items-center gap-2"
                                        >
                                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                                            </svg>
                                            Mój Profil
                                        </button>

                                        {user?.systemRole === 'SUPER_ADMIN' && (
                                            <button
                                                onClick={() => {
                                                    navigate('/admin');
                                                    setShowDropdown(false);
                                                }}
                                                className="w-full text-left px-4 py-2 text-sm text-red-400 hover:bg-gray-700 hover:text-red-300 flex items-center gap-2"
                                            >
                                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4" />
                                                </svg>
                                                Panel Admina
                                            </button>
                                        )}

                                        <div className="border-t border-gray-700">
                                            <button
                                                onClick={() => {
                                                    handleLogout();
                                                    setShowDropdown(false);
                                                }}
                                                className="w-full text-left px-4 py-2 text-sm text-gray-300 hover:bg-red-900 hover:bg-opacity-20 hover:text-red-400 flex items-center gap-2"
                                            >
                                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                                                </svg>
                                                Wyloguj
                                            </button>
                                        </div>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                </div>
            </nav>

            {/* Global Search Modal */}
            <GlobalSearch isOpen={showSearch} onClose={() => setShowSearch(false)} />
        </>
    );
};

export default Navbar;