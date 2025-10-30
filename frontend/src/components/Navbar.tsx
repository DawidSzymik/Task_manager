// src/components/Navbar.tsx
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import NotificationDropdown from './NotificationDropdown';
import SearchButton from './SearchButton';

const Navbar: React.FC = () => {
    const navigate = useNavigate();
    const { user, logout } = useAuth();
    const [showDropdown, setShowDropdown] = useState(false);

    const handleLogout = async () => {
        try {
            await logout();
            navigate('/login');
        } catch (error) {
            console.error('Logout failed:', error);
        }
    };

    return (
        <nav className="bg-gray-900 border-b border-gray-800 sticky top-0 z-50">
            <div className="max-w-full mx-auto px-4 sm:px-6 lg:px-8">
                <div className="flex items-center justify-between h-16">
                    {/* Logo */}
                    <div className="flex items-center gap-4">
                        <div className="flex-shrink-0">
                            <h1 className="text-2xl font-bold cursor-pointer" onClick={() => navigate('/dashboard')}>
                                <span className="text-emerald-500">TASK</span>
                                <span className="text-white">MANAGER</span>
                            </h1>
                        </div>

                        {/* Search Button - Visible on larger screens */}
                        <div className="hidden md:block">
                            <SearchButton />
                        </div>
                    </div>

                    {/* Right side - User menu */}
                    <div className="flex items-center gap-4">
                        {/* Search Button - Mobile */}
                        <div className="md:hidden">
                            <SearchButton />
                        </div>

                        {/* Notifications */}
                        <NotificationDropdown />

                        {/* User profile dropdown */}
                        <div className="relative">
                            <button
                                onClick={() => setShowDropdown(!showDropdown)}
                                className="flex items-center gap-3 p-2 rounded-lg hover:bg-gray-800 transition"
                            >
                                <div className="w-8 h-8 bg-emerald-500 rounded-full flex items-center justify-center">
                                    <span className="text-white font-semibold text-sm">
                                        {user?.username?.charAt(0).toUpperCase() || 'U'}
                                    </span>
                                </div>
                                <div className="text-left hidden sm:block">
                                    <p className="text-sm font-medium text-white">{user?.username || 'User'}</p>
                                    <p className="text-xs text-gray-400">{user?.systemRole || 'USER'}</p>
                                </div>
                                <svg className={`w-4 h-4 text-gray-400 transition-transform ${showDropdown ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                                </svg>
                            </button>

                            {/* Dropdown Menu */}
                            {showDropdown && (
                                <div className="absolute right-0 mt-2 w-48 bg-gray-800 rounded-lg shadow-lg border border-gray-700 py-2">
                                    <button
                                        onClick={() => {
                                            navigate('/profile');
                                            setShowDropdown(false);
                                        }}
                                        className="w-full text-left px-4 py-2 text-sm text-gray-300 hover:bg-gray-700 hover:text-white transition"
                                    >
                                        Profil
                                    </button>
                                    <button
                                        onClick={() => {
                                            navigate('/calendar');
                                            setShowDropdown(false);
                                        }}
                                        className="w-full text-left px-4 py-2 text-sm text-gray-300 hover:bg-gray-700 hover:text-white transition"
                                    >
                                        Kalendarz
                                    </button>
                                    {user?.systemRole === 'SUPER_ADMIN' && (
                                        <button
                                            onClick={() => {
                                                navigate('/admin');
                                                setShowDropdown(false);
                                            }}
                                            className="w-full text-left px-4 py-2 text-sm text-purple-400 hover:bg-gray-700 hover:text-purple-300 transition"
                                        >
                                            Panel Admina
                                        </button>
                                    )}
                                    <hr className="my-2 border-gray-700" />
                                    <button
                                        onClick={handleLogout}
                                        className="w-full text-left px-4 py-2 text-sm text-red-400 hover:bg-gray-700 hover:text-red-300 transition"
                                    >
                                        Wyloguj się
                                    </button>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            </div>
        </nav>
    );
};

export default Navbar;