// src/pages/TasksPage.tsx
import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import MainLayout from '../components/MainLayout';
import taskService from '../services/taskService';
import projectService from '../services/projectService';
import type { Task, Project, TaskStatus, TaskPriority } from '../types';

const TasksPage: React.FC = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

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
    const [assignedToMe, setAssignedToMe] = useState(true);

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
            filters.assignedToMe = assignedToMe;

            const data = await taskService.getAllTasks(filters);
            setTasks(data);
        } catch (error: any) {
            console.error('Failed to load tasks:', error);
            setError(error.message || 'Nie udało się załadować zadań');
        } finally {
            setLoading(false);
        }
    };

    const getStatusColor = (status: TaskStatus | undefined): string => {
        switch (status) {
            case 'NEW': return 'bg-blue-500';
            case 'IN_PROGRESS': return 'bg-yellow-500';
            case 'COMPLETED': return 'bg-green-500';
            case 'CANCELLED': return 'bg-red-500';
            default: return 'bg-gray-500';
        }
    };

    const getStatusText = (status: TaskStatus | undefined): string => {
        switch (status) {
            case 'NEW': return 'Nowe';
            case 'IN_PROGRESS': return 'W trakcie';
            case 'COMPLETED': return 'Ukończone';
            case 'CANCELLED': return 'Anulowane';
            default: return status || 'Nieznany';
        }
    };

    const getPriorityColor = (priority: TaskPriority | undefined): string => {
        switch (priority) {
            case 'URGENT': return 'text-red-500';
            case 'HIGH': return 'text-orange-500';
            case 'MEDIUM': return 'text-yellow-500';
            case 'LOW': return 'text-green-500';
            default: return 'text-gray-500';
        }
    };

    const getPriorityText = (priority: TaskPriority | undefined): string => {
        switch (priority) {
            case 'URGENT': return 'Pilne';
            case 'HIGH': return 'Wysoki';
            case 'MEDIUM': return 'Średni';
            case 'LOW': return 'Niski';
            default: return priority || 'Brak';
        }
    };

    if (loading) {
        return (
            <MainLayout>
                <div className="flex items-center justify-center h-64">
                    <div className="text-gray-400">Ładowanie zadań...</div>
                </div>
            </MainLayout>
        );
    }

    return (
        <MainLayout>
            <div className="space-y-6">
                {/* Header */}
                <div className="flex items-center justify-between">
                    <h1 className="text-3xl font-bold text-white">Moje Zadania</h1>
                </div>

                {/* Filters */}
                <div className="bg-gray-900 rounded-lg p-4 border border-gray-800">
                    <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
                        {/* Widok */}
                        <div>
                            <label className="block text-gray-400 text-sm font-medium mb-2">
                                Widok
                            </label>
                            <select
                                value={assignedToMe ? 'my' : 'all'}
                                onChange={(e) => setAssignedToMe(e.target.value === 'my')}
                                className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-primary-500"
                            >
                                <option value="my">Tylko moje zadania</option>
                                <option value="all">Wszystkie zadania</option>
                            </select>
                        </div>

                        {/* Project Filter */}
                        <div>
                            <label className="block text-gray-400 text-sm font-medium mb-2">
                                Projekt
                            </label>
                            <select
                                value={selectedProject || ''}
                                onChange={(e) => setSelectedProject(e.target.value ? Number(e.target.value) : null)}
                                className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-primary-500"
                            >
                                <option value="">Wszystkie projekty</option>
                                {projects.map((project) => (
                                    <option key={project.id} value={project.id}>
                                        {project.name}
                                    </option>
                                ))}
                            </select>
                        </div>

                        {/* Status Filter */}
                        <div>
                            <label className="block text-gray-400 text-sm font-medium mb-2">
                                Status
                            </label>
                            <select
                                value={selectedStatus}
                                onChange={(e) => setSelectedStatus(e.target.value as TaskStatus | '')}
                                className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-primary-500"
                            >
                                <option value="">Wszystkie statusy</option>
                                <option value="NEW">Nowe</option>
                                <option value="IN_PROGRESS">W trakcie</option>
                                <option value="COMPLETED">Ukończone</option>
                                <option value="CANCELLED">Anulowane</option>
                            </select>
                        </div>

                        {/* Priority Filter */}
                        <div>
                            <label className="block text-gray-400 text-sm font-medium mb-2">
                                Priorytet
                            </label>
                            <select
                                value={selectedPriority}
                                onChange={(e) => setSelectedPriority(e.target.value as TaskPriority | '')}
                                className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-primary-500"
                            >
                                <option value="">Wszystkie priorytety</option>
                                <option value="LOW">Niski</option>
                                <option value="MEDIUM">Średni</option>
                                <option value="HIGH">Wysoki</option>
                                <option value="URGENT">Pilny</option>
                            </select>
                        </div>

                        {/* Clear Filters */}
                        <div className="flex items-end">
                            <button
                                onClick={() => {
                                    setSelectedProject(null);
                                    setSelectedStatus('');
                                    setSelectedPriority('');
                                    setAssignedToMe(true);
                                }}
                                className="w-full px-4 py-2 bg-gray-800 hover:bg-gray-700 text-white rounded-lg transition"
                            >
                                Wyczyść filtry
                            </button>
                        </div>
                    </div>
                </div>

                {/* Error Message */}
                {error && (
                    <div className="bg-red-500 bg-opacity-10 border border-red-500 rounded-lg p-4">
                        <p className="text-red-400">{error}</p>
                    </div>
                )}

                {/* Tasks Grid */}
                {tasks.length === 0 ? (
                    <div className="bg-gray-900 rounded-lg p-8 text-center border border-gray-800">
                        <p className="text-gray-400">
                            {assignedToMe
                                ? 'Nie masz przypisanych żadnych zadań'
                                : 'Nie znaleziono zadań spełniających kryteria'}
                        </p>
                    </div>
                ) : (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                        {tasks.map((task) => (
                            <div
                                key={task.id}
                                className="bg-gray-900 rounded-lg p-6 border border-gray-800 hover:border-primary-500 transition cursor-pointer"
                                onClick={() => navigate(`/tasks/${task.id}`)}
                            >
                                {/* Task Header */}
                                <div className="flex items-start justify-between mb-3">
                                    <span className={`px-3 py-1 rounded-full text-xs font-semibold ${getStatusColor(task.status)} bg-opacity-20 text-white`}>
                                        {getStatusText(task.status)}
                                    </span>
                                    <span className={`text-sm font-semibold ${getPriorityColor(task.priority)}`}>
                                        {getPriorityText(task.priority)}
                                    </span>
                                </div>

                                {/* Task Content */}
                                <h3 className="text-xl font-bold text-white mb-2">{task.title}</h3>
                                {task.description && (
                                    <p className="text-gray-400 text-sm line-clamp-2 mb-3">{task.description}</p>
                                )}

                                {/* Task Meta */}
                                <div className="flex items-center gap-4 text-sm text-gray-400">
                                    {task.project && (
                                        <span className="flex items-center gap-1">
                                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
                                            </svg>
                                            {task.project.name}
                                        </span>
                                    )}
                                    {task.assignedUsers && task.assignedUsers.length > 0 && (
                                        <span className="flex items-center gap-1">
                                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                                            </svg>
                                            {task.assignedUsers.length} {task.assignedUsers.length === 1 ? 'osoba' : 'osoby'}
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

                                {/* Task Stats - POPRAWIONE: sprawdzenie czy istnieją */}
                                {((task.commentCount && task.commentCount > 0) || (task.fileCount && task.fileCount > 0)) && (
                                    <div className="flex items-center gap-4 mt-3 pt-3 border-t border-gray-800 text-xs text-gray-500">
                                        {task.commentCount && task.commentCount > 0 && (
                                            <span>💬 {task.commentCount} komentarzy</span>
                                        )}
                                        {task.fileCount && task.fileCount > 0 && (
                                            <span>📎 {task.fileCount} plików</span>
                                        )}
                                    </div>
                                )}
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </MainLayout>
    );
};

export default TasksPage;