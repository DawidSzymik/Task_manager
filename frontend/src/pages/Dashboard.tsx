import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import authService from '../services/authService';
import type { UserDto } from '../services/authService';

const Dashboard: React.FC = () => {
    const navigate = useNavigate();
    const [user, setUser] = useState<UserDto | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        loadUserData();
    }, []);

    const loadUserData = async () => {
        try {
            if (!authService.isAuthenticated()) {
                navigate('/login');
                return;
            }

            const localUser = authService.getCurrentUser();
            if (localUser) {
                setUser(localUser);
            }

            const freshUser = await authService.getProfile();
            setUser(freshUser);
        } catch (error) {
            console.error('Failed to load user data:', error);
            navigate('/login');
        } finally {
            setLoading(false);
        }
    };

    const handleLogout = async () => {
        try {
            await authService.logout();
            navigate('/login');
        } catch (error) {
            console.error('Logout error:', error);
            authService.logout();
            navigate('/login');
        }
    };

    if (loading) {
        return (
            <div className="min-h-screen bg-gray-800 flex items-center justify-center">
                <div className="flex flex-col items-center">
                    <svg className="animate-spin h-12 w-12 text-emerald-500 mb-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    <p className="text-gray-400">Ładowanie...</p>
                </div>
            </div>
        );
    }

    // Przykładowe dane zadań - później podłączymy do API
    const mockTasks = [
        { id: 1, title: 'Dokończyć frontend', status: 'IN_PROGRESS', project: 'Task Manager', deadline: '2025-10-15 18:00' },
        { id: 2, title: 'Przegląd kodu', status: 'TODO', project: 'Task Manager', deadline: '2025-10-20 12:00' },
        { id: 3, title: 'Testy jednostkowe', status: 'DONE', project: 'Backend API', deadline: '2025-10-10 15:30' },
    ];

    const getStatusStyle = (status: string) => {
        switch (status) {
            case 'TODO':
                return 'bg-blue-500';
            case 'IN_PROGRESS':
                return 'bg-orange-500';
            case 'DONE':
                return 'bg-green-500';
            case 'SUSPENDED':
                return 'bg-gray-500';
            default:
                return 'bg-gray-500';
        }
    };

    const getStatusText = (status: string) => {
        switch (status) {
            case 'TODO':
                return 'TODO';
            case 'IN_PROGRESS':
                return 'W TRAKCIE';
            case 'DONE':
                return 'UKOŃCZONE';
            case 'SUSPENDED':
                return 'ZAWIESZONE';
            default:
                return status;
        }
    };

    return (
        <div className="min-h-screen bg-gray-800">
            {/* Menu Bar - styl z poprzedniego frontendu */}
            <div className="fixed top-0 left-0 right-0 h-25 bg-gray-900 flex items-center justify-between px-10 shadow-lg z-50">
                {/* Logo */}
                <div className="flex items-center">
                    <div className="w-15 h-15 bg-emerald-500 rounded-xl flex items-center justify-center">
                        <span className="text-2xl font-bold text-white">TM</span>
                    </div>
                </div>

                {/* Menu Links */}
                <nav className="flex gap-8">
                    <button
                        onClick={() => navigate('/dashboard')}
                        className="text-white text-lg font-medium px-5 py-2.5 bg-gray-700 rounded-lg hover:bg-emerald-500 transition"
                    >
                        Strona główna
                    </button>
                    <button
                        onClick={() => navigate('/teams')}
                        className="text-white text-lg font-medium px-5 py-2.5 bg-gray-700 rounded-lg hover:bg-emerald-500 transition"
                    >
                        Zespoły
                    </button>
                    <button
                        onClick={() => navigate('/projects')}
                        className="text-white text-lg font-medium px-5 py-2.5 bg-gray-700 rounded-lg hover:bg-emerald-500 transition"
                    >
                        Projekty
                    </button>
                    <button
                        onClick={() => navigate('/kontakt')}
                        className="text-white text-lg font-medium px-5 py-2.5 bg-gray-700 rounded-lg hover:bg-emerald-500 transition"
                    >
                        Kontakt
                    </button>
                    <button
                        onClick={() => navigate('/welcome')}
                        className="text-white text-lg font-medium px-5 py-2.5 bg-gray-700 rounded-lg hover:bg-emerald-500 transition"
                    >
                        Profil
                    </button>
                    <button
                        onClick={handleLogout}
                        className="text-white text-lg font-medium px-5 py-2.5 bg-red-500 rounded-lg hover:bg-red-600 transition"
                    >
                        Wyloguj
                    </button>
                </nav>
            </div>

            {/* Main Content - styl z poprzedniego frontendu */}
            <div className="pt-32 px-4">
                <h2 className="text-center text-3xl text-white mb-5">
                    Witaj, <span className="text-emerald-400">{user?.username}</span>!
                </h2>
                <h3 className="text-center text-2xl text-emerald-500 mb-8">
                    Twoje przypisane zadania:
                </h3>

                {/* Task Table Header */}
                <div className="grid grid-cols-[2fr_1fr_1fr_1fr_0.5fr] gap-0 px-5 py-2 text-white text-xl font-bold uppercase bg-gray-800 border-b-2 border-white">
                    <div className="text-center py-2 border-r-2 border-white">Tytuł</div>
                    <div className="text-center py-2 border-r-2 border-white">Status</div>
                    <div className="text-center py-2 border-r-2 border-white">Projekt</div>
                    <div className="text-center py-2 border-r-2 border-white">Termin</div>
                    <div className="text-center py-2"></div>
                </div>

                {/* Task Rows */}
                {mockTasks.length > 0 ? (
                    mockTasks.map((task) => (
                        <button
                            key={task.id}
                            onClick={() => navigate(`/tasks/view/${task.id}`)}
                            className="w-full my-2.5 rounded-3xl overflow-hidden shadow-md hover:bg-gray-600 transition relative group"
                        >
                            {/* Progress Fill */}
                            <div className="absolute top-0 left-0 h-full w-[5%] bg-emerald-500 rounded-l-3xl"></div>

                            <div className="grid grid-cols-[2fr_1fr_1fr_1fr_0.5fr] gap-0 bg-gray-300 text-gray-900 text-xl font-bold relative z-10">
                                <div className="text-center py-2 px-1 border-r-2 border-white">{task.title}</div>
                                <div className="text-center py-2 px-1 border-r-2 border-white">
                  <span className={`inline-block ${getStatusStyle(task.status)} text-white px-2.5 py-1 rounded-3xl font-bold text-sm`}>
                    {getStatusText(task.status)}
                  </span>
                                </div>
                                <div className="text-center py-2 px-1 border-r-2 border-white">{task.project}</div>
                                <div className="text-center py-2 px-1 border-r-2 border-white">{task.deadline}</div>
                                <div className="text-center py-2 px-1 flex flex-col items-center justify-center gap-1">
                                    <span className="w-1.5 h-1.5 bg-white rounded-full"></span>
                                    <span className="w-1.5 h-1.5 bg-white rounded-full"></span>
                                    <span className="w-1.5 h-1.5 bg-white rounded-full"></span>
                                </div>
                            </div>
                        </button>
                    ))
                ) : (
                    <p className="text-center text-white text-xl mt-10">
                        Nie masz jeszcze żadnych przypisanych zadań.
                    </p>
                )}
            </div>
        </div>
    );
};

export default Dashboard;