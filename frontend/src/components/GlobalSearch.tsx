// frontend/src/components/GlobalSearch.tsx
import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';

interface SearchResult {
    id: number;
    type: 'user' | 'task' | 'project';
    url: string;
    // User fields
    username?: string;
    email?: string;
    // Task fields
    title?: string;
    description?: string;
    status?: string;
    priority?: string;
    projectName?: string;
    projectId?: number;
    // Project fields
    name?: string;
    memberCount?: number;
    taskCount?: number;
}

interface SearchResponse {
    success: boolean;
    query: string;
    users: SearchResult[];
    tasks: SearchResult[];
    projects: SearchResult[];
    totalResults: number;
}

interface GlobalSearchProps {
    isOpen: boolean;
    onClose: () => void;
}

const GlobalSearch: React.FC<GlobalSearchProps> = ({ isOpen, onClose }) => {
    const [query, setQuery] = useState('');
    const [results, setResults] = useState<SearchResponse | null>(null);
    const [loading, setLoading] = useState(false);
    const inputRef = useRef<HTMLInputElement>(null);
    const navigate = useNavigate();

    // Focus input when modal opens
    useEffect(() => {
        if (isOpen && inputRef.current) {
            inputRef.current.focus();
        }
    }, [isOpen]);

    // Handle ESC key
    useEffect(() => {
        const handleEsc = (e: KeyboardEvent) => {
            if (e.key === 'Escape' && isOpen) {
                onClose();
            }
        };
        window.addEventListener('keydown', handleEsc);
        return () => window.removeEventListener('keydown', handleEsc);
    }, [isOpen, onClose]);

    // Search function with debounce
    useEffect(() => {
        if (!query.trim()) {
            setResults(null);
            return;
        }

        const timeoutId = setTimeout(async () => {
            try {
                setLoading(true);
                const params = new URLSearchParams({ q: query, limit: '5' });
                const response = await fetch(`/api/v1/search?${params}`, {
                    method: 'GET',
                    credentials: 'include',
                });

                if (!response.ok) {
                    throw new Error('Search failed');
                }

                const data: SearchResponse = await response.json();
                setResults(data);
            } catch (error) {
                console.error('Search failed:', error);
                setResults(null);
            } finally {
                setLoading(false);
            }
        }, 300); // Debounce 300ms

        return () => clearTimeout(timeoutId);
    }, [query]);

    const handleResultClick = (result: SearchResult) => {
        navigate(result.url);
        onClose();
        setQuery('');
        setResults(null);
    };

    const handleClose = () => {
        onClose();
        setQuery('');
        setResults(null);
    };

    if (!isOpen) return null;

    return (
        <>
            {/* Backdrop */}
            <div
                className="fixed inset-0 bg-black bg-opacity-50 z-40"
                onClick={handleClose}
            />

            {/* Modal */}
            <div className="fixed top-20 left-1/2 transform -translate-x-1/2 w-full max-w-2xl z-50">
                <div className="bg-gray-900 border border-gray-800 rounded-lg shadow-2xl">
                    {/* Header with close button */}
                    <div className="flex items-center gap-3 p-4 border-b border-gray-800">
                        <svg className="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                        </svg>
                        <input
                            ref={inputRef}
                            type="text"
                            placeholder="Szukaj użytkowników, zadań, projektów..."
                            value={query}
                            onChange={(e) => setQuery(e.target.value)}
                            className="flex-1 bg-transparent text-white outline-none placeholder-gray-500"
                        />

                        {/* Przycisk X do zamykania */}
                        <button
                            onClick={handleClose}
                            className="p-2 text-gray-400 hover:text-white hover:bg-gray-800 rounded-lg transition"
                            title="Zamknij (ESC)"
                        >
                            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                            </svg>
                        </button>
                    </div>

                    {/* Results */}
                    <div className="max-h-96 overflow-y-auto p-2">
                        {loading && (
                            <div className="flex items-center justify-center py-8">
                                <svg className="animate-spin h-6 w-6 text-emerald-500" fill="none" viewBox="0 0 24 24">
                                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                </svg>
                            </div>
                        )}

                        {results && results.totalResults === 0 && (
                            <div className="text-center py-8 text-gray-400">
                                Brak wyników dla "{query}"
                            </div>
                        )}

                        {results && results.users && results.users.length > 0 && (
                            <div className="mb-4">
                                <div className="px-3 py-2 text-xs font-semibold text-gray-500 uppercase">
                                    Użytkownicy
                                </div>
                                {results.users.map((user) => (
                                    <button
                                        key={`user-${user.id}`}
                                        onClick={() => handleResultClick(user)}
                                        className="w-full flex items-center gap-3 px-3 py-2 hover:bg-gray-800 rounded-lg transition text-left"
                                    >
                                        <div className="w-8 h-8 bg-emerald-500 rounded-full flex items-center justify-center">
                                            <span className="text-white text-sm font-semibold">
                                                {user.username?.charAt(0).toUpperCase()}
                                            </span>
                                        </div>
                                        <div>
                                            <div className="text-white font-medium">{user.username}</div>
                                            <div className="text-sm text-gray-400">{user.email}</div>
                                        </div>
                                    </button>
                                ))}
                            </div>
                        )}

                        {results && results.tasks && results.tasks.length > 0 && (
                            <div className="mb-4">
                                <div className="px-3 py-2 text-xs font-semibold text-gray-500 uppercase">
                                    Zadania
                                </div>
                                {results.tasks.map((task) => (
                                    <button
                                        key={`task-${task.id}`}
                                        onClick={() => handleResultClick(task)}
                                        className="w-full flex items-start gap-3 px-3 py-2 hover:bg-gray-800 rounded-lg transition text-left"
                                    >
                                        <svg className="w-5 h-5 text-emerald-500 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                                        </svg>
                                        <div className="flex-1">
                                            <div className="text-white font-medium">{task.title}</div>
                                            <div className="text-sm text-gray-400">
                                                {task.projectName} • {task.status} • {task.priority}
                                            </div>
                                        </div>
                                    </button>
                                ))}
                            </div>
                        )}

                        {results && results.projects && results.projects.length > 0 && (
                            <div>
                                <div className="px-3 py-2 text-xs font-semibold text-gray-500 uppercase">
                                    Projekty
                                </div>
                                {results.projects.map((project) => (
                                    <button
                                        key={`project-${project.id}`}
                                        onClick={() => handleResultClick(project)}
                                        className="w-full flex items-start gap-3 px-3 py-2 hover:bg-gray-800 rounded-lg transition text-left"
                                    >
                                        <svg className="w-5 h-5 text-emerald-500 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
                                        </svg>
                                        <div className="flex-1">
                                            <div className="text-white font-medium">{project.name}</div>
                                            <div className="text-sm text-gray-400">
                                                {project.memberCount} członków • {project.taskCount} zadań
                                            </div>
                                        </div>
                                    </button>
                                ))}
                            </div>
                        )}
                    </div>

                    {/* Footer - hint */}
                    <div className="border-t border-gray-800 px-4 py-3 flex items-center justify-between text-xs text-gray-500">
                        <span>Użyj strzałek ↑↓ aby nawigować</span>
                        <span>ESC lub X aby zamknąć</span>
                    </div>
                </div>
            </div>
        </>
    );
};

export default GlobalSearch;