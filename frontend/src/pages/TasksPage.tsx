// src/pages/TasksPage.tsx
import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import MainLayout from '../components/MainLayout';
import taskService from '../services/taskService';
import projectService from '../services/projectService';
import type { Task, Project, TaskStatus, TaskPriority } from '../types';

const TasksPage: React.FC = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams(); // usuń setSearchParams

    const [tasks, setTasks] = useState<Task[]>([]);
    const [projects, setProjects] = useState<Project[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    // Filtry
    const [selectedProject, setSelectedProject] = useState<number | null>(
        searchParams.get('projectId') ? Number(searchParams.get('projectId')) : null
    );
    const [selectedStatus, setSelectedStatus] = useState<TaskStatus | ''>('');
    const [selectedPriority, setSelectedPriority] = useState<TaskPriority | ''>('');
    const [assignedToMe, setAssignedToMe] = useState(false);

    useEffect(() => {
        loadInitialData();
    }, []);

    useEffect(() => {
        loadTasks();
    }, [selectedProject, selectedStatus, selectedPriority, assignedToMe]);

    const loadInitialData = async () => {
        try {
            const projectsData = await projectService.getAllProjects();
            setProjects(projectsData);
        } catch (error: any) {
            console.error('Failed to load projects:', error);
        }
    };

    const loadTasks = async () => {
        try {
            setLoading(true);
            setError(null);

            const filters: any = {};
            if (selectedProject) filters.projectId = selectedProject;
            if (selectedStatus) filters.status = selectedStatus;
            if (selectedPriority) filters.priority = selectedPriority;
            if (assignedToMe) filters.assignedToMe = true;

            const data = await taskService.getAllTasks(filters);
            setTasks(data);
        } catch (error: any) {
            console.error('Failed to load tasks:', error);
            setError(error.message || 'Nie udało się załadować zadań');
        } finally {
            setLoading(false);
        }
    };

    const handleDeleteTask = async (taskId: number, taskTitle: string) => {
        if (!window.confirm(`Czy na pewno chcesz usunąć zadanie "${taskTitle}"?`)) {
            return;
        }

        try {
            await taskService.deleteTask(taskId);
            await loadTasks();
        } catch (error: any) {
            console.error('Failed to delete task:', error);
            alert(error.message || 'Nie udało się usunąć zadania');
        }
    };

    const getStatusColor = (status?: TaskStatus) => {
        if (!status) return 'bg-gray-500';
        switch (status) {
            case 'NEW': return 'bg-blue-500';
            case 'IN_PROGRESS': return 'bg-yellow-500';
            case 'COMPLETED': return 'bg-green-500';
            case 'CANCELLED': return 'bg-gray-500';
            default: return 'bg-gray-500';
        }
    };

    const getStatusLabel = (status?: TaskStatus) => {
        if (!status) return 'Nieznany';
        switch (status) {
            case 'NEW': return 'Nowe';
            case 'IN_PROGRESS': return 'W trakcie';
            case 'COMPLETED': return 'Ukończone';
            case 'CANCELLED': return 'Anulowane';
            default: return status;
        }
    };

    const getPriorityColor = (priority?: TaskPriority) => {
        if (!priority) return 'text-gray-400';
        switch (priority) {
            case 'LOW': return 'text-green-400';
            case 'MEDIUM': return 'text-yellow-400';
            case 'HIGH': return 'text-orange-400';
            case 'URGENT': return 'text-red-400';
            default: return 'text-gray-400';
        }
    };

    const getPriorityLabel = (priority?: TaskPriority) => {
        if (!priority) return 'Nieznany';
        switch (priority) {
            case 'LOW': return 'Niski';
            case 'MEDIUM': return 'Średni';
            case 'HIGH': return 'Wysoki';
            case 'URGENT': return 'Pilny';
            default: return priority;
        }
    };

    if (loading) {
        return (
            <MainLayout>
                <div className="flex items-center justify-center h-96">
                    <div className="flex flex-col items-center">
                        <svg className="animate-spin h-12 w-12 text-emerald-500 mb-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                        </svg>
                        <p className="text-gray-400">Ładowanie zadań...</p>
                    </div>
                </div>
            </MainLayout>
        );
    }

    return (
        <MainLayout>
            <div className="max-w-7xl mx-auto">
                {/* Header */}
                <div className="flex items-center justify-between mb-8">
                    <div>
                        <h1 className="text-3xl font-bold text-white mb-2">Zadania</h1>
                        <p className="text-gray-400">Zarządzaj swoimi zadaniami</p>
                    </div>
                </div>

                {/* Filters */}
                <div className="bg-gray-900 border border-gray-800 rounded-lg p-6 mb-6">
                    <h2 className="text-lg font-semibold text-white mb-4">Filtry</h2>
                    <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                        {/* Project filter */}
                        <div>
                            <label className="block text-gray-400 text-sm mb-2">Projekt</label>
                            <select
                                value={selectedProject || ''}
                                onChange={(e) => setSelectedProject(e.target.value ? Number(e.target.value) : null)}
                                className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-emerald-500"
                            >
                                <option value="">Wszystkie projekty</option>
                                {projects.map((project) => (
                                    <option key={project.id} value={project.id}>
                                        {project.name}
                                    </option>
                                ))}
                            </select>
                        </div>

                        {/* Status filter */}
                        <div>
                            <label className="block text-gray-400 text-sm mb-2">Status</label>
                            <select
                                value={selectedStatus}
                                onChange={(e) => setSelectedStatus(e.target.value as TaskStatus | '')}
                                className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-emerald-500"
                            >
                                <option value="">Wszystkie statusy</option>
                                <option value="NEW">Nowe</option>
                                <option value="IN_PROGRESS">W trakcie</option>
                                <option value="COMPLETED">Ukończone</option>
                                <option value="CANCELLED">Anulowane</option>
                            </select>
                        </div>

                        {/* Priority filter */}
                        <div>
                            <label className="block text-gray-400 text-sm mb-2">Priorytet</label>
                            <select
                                value={selectedPriority}
                                onChange={(e) => setSelectedPriority(e.target.value as TaskPriority | '')}
                                className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-emerald-500"
                            >
                                <option value="">Wszystkie priorytety</option>
                                <option value="LOW">Niski</option>
                                <option value="MEDIUM">Średni</option>
                                <option value="HIGH">Wysoki</option>
                                <option value="URGENT">Pilny</option>
                            </select>
                        </div>

                        {/* Assigned to me */}
                        <div className="flex items-end">
                            <label className="flex items-center gap-2 px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg cursor-pointer hover:border-emerald-500 transition">
                                <input
                                    type="checkbox"
                                    checked={assignedToMe}
                                    onChange={(e) => setAssignedToMe(e.target.checked)}
                                    className="w-4 h-4 text-emerald-500 bg-gray-700 border-gray-600 rounded focus:ring-emerald-500"
                                />
                                <span className="text-white text-sm">Przypisane do mnie</span>
                            </label>
                        </div>
                    </div>
                </div>

                {/* Error message */}
                {error && (
                    <div className="mb-6 p-4 bg-red-500 bg-opacity-10 border border-red-500 rounded-lg text-red-400">
                        {error}
                    </div>
                )}

                {/* Tasks List */}
                {tasks.length === 0 ? (
                    <div className="text-center py-16">
                        <svg className="w-24 h-24 text-gray-600 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                        </svg>
                        <p className="text-gray-400 text-lg mb-4">Brak zadań do wyświetlenia</p>
                        <p className="text-gray-500 text-sm">Wybierz projekt, aby dodać nowe zadanie</p>
                    </div>
                ) : (
                    <div className="space-y-4">
                        {tasks.map((task) => (
                            <div
                                key={task.id}
                                className="bg-gray-900 border border-gray-800 rounded-lg p-6 hover:border-emerald-500 transition-colors cursor-pointer"
                                onClick={() => navigate(`/tasks/${task.id}`)}
                            >
                                <div className="flex items-start justify-between mb-4">
                                    <div className="flex-1">
                                        <div className="flex items-center gap-3 mb-2">
                                            <span className={`px-3 py-1 rounded-full text-xs font-semibold ${getStatusColor(task.status)} text-white`}>
                                                {getStatusLabel(task.status)}
                                            </span>
                                            <span className={`text-sm font-semibold ${getPriorityColor(task.priority)}`}>
                                                {getPriorityLabel(task.priority)}
                                            </span>
                                        </div>
                                        <h3 className="text-xl font-bold text-white mb-2">{task.title}</h3>
                                        {task.description && (
                                            <p className="text-gray-400 text-sm line-clamp-2 mb-3">{task.description}</p>
                                        )}
                                        <div className="flex items-center gap-4 text-sm text-gray-400">
                                            {task.project && (
                                                <span className="flex items-center gap-1">
                                                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
                                                    </svg>
                                                    {task.project.name}
                                                </span>
                                            )}
                                            {task.assignedTo && (
                                                <span className="flex items-center gap-1">
                                                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                                                    </svg>
                                                    {task.assignedTo.username}
                                                </span>
                                            )}
                                            {task.deadline && (
                                                <span className="flex items-center gap-1">
                                                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                                                    </svg>
                                                    {new Date(task.deadline).toLocaleDateString('pl-PL')}
                                                </span>
                                            )}
                                        </div>
                                    </div>
                                    <button
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            handleDeleteTask(task.id, task.title);
                                        }}
                                        className="p-2 bg-red-500 bg-opacity-10 hover:bg-red-500 hover:bg-opacity-20 text-red-400 rounded-lg transition"
                                    >
                                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                        </svg>
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </MainLayout>
    );
};

export default TasksPage;