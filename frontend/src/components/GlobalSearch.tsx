// src/components/GlobalSearch.tsx
import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import searchService, {type SearchResponse } from '../services/searchService';

interface GlobalSearchProps {
    isOpen: boolean;
    onClose: () => void;
}

const GlobalSearch: React.FC<GlobalSearchProps> = ({ isOpen, onClose }) => {
    const navigate = useNavigate();
    const [query, setQuery] = useState('');
    const [results, setResults] = useState<SearchResponse | null>(null);
    const [loading, setLoading] = useState(false);
    const [selectedIndex, setSelectedIndex] = useState(0);
    const inputRef = useRef<HTMLInputElement>(null);
    const modalRef = useRef<HTMLDivElement>(null);

    // Focus input when modal opens
    useEffect(() => {
        if (isOpen && inputRef.current) {
            inputRef.current.focus();
        }
    }, [isOpen]);

    // Debounced search
    useEffect(() => {
        if (!query.trim()) {
            setResults(null);
            return;
        }

        const timeoutId = setTimeout(async () => {
            try {
                setLoading(true);
                const data = await searchService.globalSearch(query, 5);
                setResults(data);
                setSelectedIndex(0);
            } catch (error) {
                console.error('Search failed:', error);
            } finally {
                setLoading(false);
            }
        }, 300); // 300ms debounce

        return () => clearTimeout(timeoutId);
    }, [query]);

    // Close on Escape
    useEffect(() => {
        const handleEscape = (e: KeyboardEvent) => {
            if (e.key === 'Escape') {
                onClose();
            }
        };

        if (isOpen) {
            document.addEventListener('keydown', handleEscape);
        }

        return () => document.removeEventListener('keydown', handleEscape);
    }, [isOpen, onClose]);

    // Click outside to close
    useEffect(() => {
        const handleClickOutside = (e: MouseEvent) => {
            if (modalRef.current && !modalRef.current.contains(e.target as Node)) {
                onClose();
            }
        };

        if (isOpen) {
            document.addEventListener('mousedown', handleClickOutside);
        }

        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, [isOpen, onClose]);

    // Get all results as flat array for keyboard navigation
    const getAllResults = () => {
        if (!results) return [];

        const allResults: Array<{ type: string; item: any }> = [];

        results.users.forEach(user => allResults.push({ type: 'user', item: user }));
        results.tasks.forEach(task => allResults.push({ type: 'task', item: task }));
        results.projects.forEach(project => allResults.push({ type: 'project', item: project }));

        return allResults;
    };

    // Handle keyboard navigation
    const handleKeyDown = (e: React.KeyboardEvent) => {
        const allResults = getAllResults();

        if (e.key === 'ArrowDown') {
            e.preventDefault();
            setSelectedIndex(prev => (prev + 1) % allResults.length);
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            setSelectedIndex(prev => (prev - 1 + allResults.length) % allResults.length);
        } else if (e.key === 'Enter' && allResults.length > 0) {
            e.preventDefault();
            const selected = allResults[selectedIndex];
            if (selected) {
                navigate(selected.item.url);
                onClose();
            }
        }
    };

    const handleResultClick = (url: string) => {
        navigate(url);
        onClose();
    };

    const getIcon = (type: string) => {
        switch (type) {
            case 'user':
                return (
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                    </svg>
                );
            case 'task':
                return (
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                    </svg>
                );
            case 'project':
                return (
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
                    </svg>
                );
            default:
                return null;
        }
    };

    const getPriorityColor = (priority: string) => {
        switch (priority) {
            case 'HIGH': return 'text-red-400';
            case 'MEDIUM': return 'text-yellow-400';
            case 'LOW': return 'text-green-400';
            default: return 'text-gray-400';
        }
    };

    const getStatusColor = (status: string) => {
        switch (status) {
            case 'COMPLETED': return 'text-green-400';
            case 'IN_PROGRESS': return 'text-blue-400';
            case 'TODO': return 'text-gray-400';
            default: return 'text-gray-400';
        }
    };

    if (!isOpen) return null;
    getAllResults();
    let currentIndex = 0;

    return (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-start justify-center pt-20">
            <div
                ref={modalRef}
                className="bg-gray-800 rounded-xl shadow-2xl border border-gray-700 w-full max-w-2xl max-h-[600px] flex flex-col"
            >
                {/* Search Input */}
                <div className="p-4 border-b border-gray-700">
                    <div className="relative">
                        <svg
                            className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400"
                            fill="none"
                            stroke="currentColor"
                            viewBox="0 0 24 24"
                        >
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                        </svg>
                        <input
                            ref={inputRef}
                            type="text"
                            value={query}
                            onChange={(e) => setQuery(e.target.value)}
                            onKeyDown={handleKeyDown}
                            placeholder="Wyszukaj użytkowników, zadania, projekty..."
                            className="w-full pl-10 pr-4 py-3 bg-gray-900 text-white rounded-lg border border-gray-700 focus:border-emerald-500 focus:outline-none placeholder-gray-400"
                        />
                        {loading && (
                            <div className="absolute right-3 top-1/2 transform -translate-y-1/2">
                                <div className="animate-spin h-5 w-5 border-2 border-emerald-500 border-t-transparent rounded-full"></div>
                            </div>
                        )}
                    </div>
                    <div className="flex items-center gap-2 mt-2 text-xs text-gray-400">
                        <kbd className="px-2 py-1 bg-gray-900 rounded border border-gray-700">↑↓</kbd>
                        <span>nawiguj</span>
                        <kbd className="px-2 py-1 bg-gray-900 rounded border border-gray-700">Enter</kbd>
                        <span>wybierz</span>
                        <kbd className="px-2 py-1 bg-gray-900 rounded border border-gray-700">Esc</kbd>
                        <span>zamknij</span>
                    </div>
                </div>

                {/* Results */}
                <div className="overflow-y-auto flex-1">
                    {!query.trim() ? (
                        <div className="p-8 text-center text-gray-400">
                            <svg className="w-16 h-16 mx-auto mb-4 opacity-50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                            </svg>
                            <p className="text-lg">Zacznij wpisywać aby wyszukać...</p>
                            <p className="text-sm mt-2">Wyszukuj użytkowników, zadania i projekty</p>
                        </div>
                    ) : results && results.totalResults === 0 ? (
                        <div className="p-8 text-center text-gray-400">
                            <svg className="w-16 h-16 mx-auto mb-4 opacity-50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                            <p className="text-lg">Brak wyników dla "{query}"</p>
                            <p className="text-sm mt-2">Spróbuj innego zapytania</p>
                        </div>
                    ) : results ? (
                        <div className="divide-y divide-gray-700">
                            {/* Users */}
                            {results.users.length > 0 && (
                                <div className="p-4">
                                    <h3 className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2">
                                        Użytkownicy ({results.users.length})
                                    </h3>
                                    <div className="space-y-1">
                                        {results.users.map((user) => {
                                            const itemIndex = currentIndex++;
                                            return (
                                                <button
                                                    key={user.id}
                                                    onClick={() => handleResultClick(user.url)}
                                                    className={`w-full text-left px-3 py-2 rounded-lg transition-colors flex items-center gap-3 ${
                                                        selectedIndex === itemIndex
                                                            ? 'bg-emerald-500/20 border border-emerald-500'
                                                            : 'hover:bg-gray-700/50'
                                                    }`}
                                                >
                                                    <div className="text-blue-400">
                                                        {getIcon('user')}
                                                    </div>
                                                    <div className="flex-1 min-w-0">
                                                        <p className="text-white font-medium">{user.username}</p>
                                                        {user.email && (
                                                            <p className="text-xs text-gray-400 truncate">{user.email}</p>
                                                        )}
                                                    </div>
                                                    <span className="text-xs text-gray-500">{user.systemRole}</span>
                                                </button>
                                            );
                                        })}
                                    </div>
                                </div>
                            )}

                            {/* Tasks */}
                            {results.tasks.length > 0 && (
                                <div className="p-4">
                                    <h3 className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2">
                                        Zadania ({results.tasks.length})
                                    </h3>
                                    <div className="space-y-1">
                                        {results.tasks.map((task) => {
                                            const itemIndex = currentIndex++;
                                            return (
                                                <button
                                                    key={task.id}
                                                    onClick={() => handleResultClick(task.url)}
                                                    className={`w-full text-left px-3 py-2 rounded-lg transition-colors flex items-center gap-3 ${
                                                        selectedIndex === itemIndex
                                                            ? 'bg-emerald-500/20 border border-emerald-500'
                                                            : 'hover:bg-gray-700/50'
                                                    }`}
                                                >
                                                    <div className="text-purple-400">
                                                        {getIcon('task')}
                                                    </div>
                                                    <div className="flex-1 min-w-0">
                                                        <p className="text-white font-medium">{task.title}</p>
                                                        <div className="flex items-center gap-2 text-xs mt-1">
                                                            <span className="text-gray-500">{task.projectName}</span>
                                                            <span className={getStatusColor(task.status)}>
                                                                {task.status}
                                                            </span>
                                                            <span className={getPriorityColor(task.priority)}>
                                                                {task.priority}
                                                            </span>
                                                        </div>
                                                    </div>
                                                </button>
                                            );
                                        })}
                                    </div>
                                </div>
                            )}

                            {/* Projects */}
                            {results.projects.length > 0 && (
                                <div className="p-4">
                                    <h3 className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2">
                                        Projekty ({results.projects.length})
                                    </h3>
                                    <div className="space-y-1">
                                        {results.projects.map((project) => {
                                            const itemIndex = currentIndex++;
                                            return (
                                                <button
                                                    key={project.id}
                                                    onClick={() => handleResultClick(project.url)}
                                                    className={`w-full text-left px-3 py-2 rounded-lg transition-colors flex items-center gap-3 ${
                                                        selectedIndex === itemIndex
                                                            ? 'bg-emerald-500/20 border border-emerald-500'
                                                            : 'hover:bg-gray-700/50'
                                                    }`}
                                                >
                                                    <div className="text-emerald-400">
                                                        {getIcon('project')}
                                                    </div>
                                                    <div className="flex-1 min-w-0">
                                                        <p className="text-white font-medium">{project.name}</p>
                                                        {project.description && (
                                                            <p className="text-xs text-gray-400 truncate">{project.description}</p>
                                                        )}
                                                        <div className="flex items-center gap-3 text-xs text-gray-500 mt-1">
                                                            <span>👥 {project.memberCount} członków</span>
                                                            <span>📋 {project.taskCount} zadań</span>
                                                        </div>
                                                    </div>
                                                </button>
                                            );
                                        })}
                                    </div>
                                </div>
                            )}
                        </div>
                    ) : null}
                </div>
            </div>
        </div>
    );
};

export default GlobalSearch;