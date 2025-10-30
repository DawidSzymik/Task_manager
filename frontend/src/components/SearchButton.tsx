// src/components/SearchButton.tsx
import React, { useState, useEffect } from 'react';
import GlobalSearch from './GlobalSearch';

const SearchButton: React.FC = () => {
    const [isSearchOpen, setIsSearchOpen] = useState(false);

    // Listen for Ctrl+K (or Cmd+K on Mac)
    useEffect(() => {
        const handleKeyDown = (e: KeyboardEvent) => {
            if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
                e.preventDefault();
                setIsSearchOpen(true);
            }
        };

        document.addEventListener('keydown', handleKeyDown);
        return () => document.removeEventListener('keydown', handleKeyDown);
    }, []);

    return (
        <>
            <button
                onClick={() => setIsSearchOpen(true)}
                className="flex items-center gap-2 px-3 py-2 bg-gray-800 hover:bg-gray-700 text-gray-400 hover:text-white rounded-lg transition-all duration-200 border border-gray-700 hover:border-gray-600"
            >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
                <span className="hidden sm:inline text-sm">Wyszukaj...</span>
                <kbd className="hidden sm:inline-block px-2 py-0.5 text-xs bg-gray-900 rounded border border-gray-700">
                    ⌘K
                </kbd>
            </button>

            <GlobalSearch isOpen={isSearchOpen} onClose={() => setIsSearchOpen(false)} />
        </>
    );
};

export default SearchButton;