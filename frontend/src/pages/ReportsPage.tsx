// src/pages/ReportsPage.tsx
import React, { useEffect, useState } from 'react';
import MainLayout from '../components/MainLayout';
import taskService from '../services/taskService';
import projectService from '../services/projectService';
import type { Task, Project } from '../types';

interface TaskStats {
    total: number;
    new: number;
    inProgress: number;
    completed: number;
    cancelled: number;
}

interface PriorityStats {
    low: number;
    medium: number;
    high: number;
    urgent: number;
}

interface ProjectStats {
    id: number;
    name: string;
    taskCount: number;
    completedCount: number;
    completionRate: number;
}

const ReportsPage: React.FC = () => {
    const [tasks, setTasks] = useState<Task[]>([]);
    const [projects, setProjects] = useState<Project[]>([]);
    const [taskStats, setTaskStats] = useState<TaskStats>({
        total: 0,
        new: 0,
        inProgress: 0,
        completed: 0,
        cancelled: 0,
    });
    const [priorityStats, setPriorityStats] = useState<PriorityStats>({
        low: 0,
        medium: 0,
        high: 0,
        urgent: 0,
    });
    const [projectStats, setProjectStats] = useState<ProjectStats[]>([]);
    const [loading, setLoading] = useState(true);
    const [selectedPeriod, setSelectedPeriod] = useState<'all' | 'month' | 'week'>('all');

    useEffect(() => {
        loadData();
    }, []);

    useEffect(() => {
        calculateStats();
    }, [tasks, projects, selectedPeriod]);

    const loadData = async () => {
        try {
            setLoading(true);
            const [tasksData, projectsData] = await Promise.all([
                taskService.getAllTasks(),
                projectService.getAllProjects(),
            ]);
            setTasks(tasksData);
            setProjects(projectsData);
        } catch (error) {
            console.error('Failed to load data:', error);
        } finally {
            setLoading(false);
        }
    };

    const filterTasksByPeriod = (tasks: Task[]): Task[] => {
        if (selectedPeriod === 'all') return tasks;

        const now = new Date();
        const filterDate = new Date();

        if (selectedPeriod === 'week') {
            filterDate.setDate(now.getDate() - 7);
        } else if (selectedPeriod === 'month') {
            filterDate.setMonth(now.getMonth() - 1);
        }

        return tasks.filter(task => {
            const createdAt = new Date(task.createdAt || '');
            return createdAt >= filterDate;
        });
    };

    const calculateStats = () => {
        const filteredTasks = filterTasksByPeriod(tasks);

        // Task status stats
        const stats: TaskStats = {
            total: filteredTasks.length,
            new: filteredTasks.filter(t => t.status === 'NEW').length,
            inProgress: filteredTasks.filter(t => t.status === 'IN_PROGRESS').length,
            completed: filteredTasks.filter(t => t.status === 'COMPLETED').length,
            cancelled: filteredTasks.filter(t => t.status === 'CANCELLED').length,
        };
        setTaskStats(stats);

        // Priority stats
        const priority: PriorityStats = {
            low: filteredTasks.filter(t => t.priority === 'LOW').length,
            medium: filteredTasks.filter(t => t.priority === 'MEDIUM').length,
            high: filteredTasks.filter(t => t.priority === 'HIGH').length,
            urgent: filteredTasks.filter(t => t.priority === 'URGENT').length,
        };
        setPriorityStats(priority);

        // Project stats
        const projectStatsData = projects.map(project => {
            const projectTasks = filteredTasks.filter(t => t.project?.id === project.id);
            const completedTasks = projectTasks.filter(t => t.status === 'COMPLETED');
            return {
                id: project.id,
                name: project.name,
                taskCount: projectTasks.length,
                completedCount: completedTasks.length,
                completionRate: projectTasks.length > 0
                    ? Math.round((completedTasks.length / projectTasks.length) * 100)
                    : 0,
            };
        }).sort((a, b) => b.taskCount - a.taskCount);
        setProjectStats(projectStatsData);
    };

    const getCompletionRate = (): number => {
        if (taskStats.total === 0) return 0;
        return Math.round((taskStats.completed / taskStats.total) * 100);
    };

    const getOverdueTasks = (): Task[] => {
        const now = new Date();
        return tasks.filter(task => {
            if (!task.deadline || task.status === 'COMPLETED' || task.status === 'CANCELLED') {
                return false;
            }
            return new Date(task.deadline) < now;
        });
    };

    const getUpcomingTasks = (): Task[] => {
        const now = new Date();
        const weekFromNow = new Date();
        weekFromNow.setDate(now.getDate() + 7);

        return tasks.filter(task => {
            if (!task.deadline || task.status === 'COMPLETED' || task.status === 'CANCELLED') {
                return false;
            }
            const deadline = new Date(task.deadline);
            return deadline >= now && deadline <= weekFromNow;
        }).sort((a, b) => {
            const dateA = new Date(a.deadline!);
            const dateB = new Date(b.deadline!);
            return dateA.getTime() - dateB.getTime();
        });
    };

    if (loading) {
        return (
            <MainLayout>
                <div className="flex items-center justify-center h-96">
                    <div className="text-gray-400">Ładowanie raportów...</div>
                </div>
            </MainLayout>
        );
    }

    const overdueTasks = getOverdueTasks();
    const upcomingTasks = getUpcomingTasks();

    return (
        <MainLayout>
            <div className="max-w-7xl mx-auto">
                {/* Header */}
                <div className="mb-6">
                    <div className="flex items-center justify-between">
                        <h1 className="text-3xl font-bold text-white">📊 Raporty i Statystyki</h1>
                        <div className="flex gap-2">
                            <button
                                onClick={() => setSelectedPeriod('week')}
                                className={`px-4 py-2 rounded-lg transition-colors ${
                                    selectedPeriod === 'week'
                                        ? 'bg-primary-500 text-white'
                                        : 'bg-gray-800 text-gray-400 hover:bg-gray-700'
                                }`}
                            >
                                Tydzień
                            </button>
                            <button
                                onClick={() => setSelectedPeriod('month')}
                                className={`px-4 py-2 rounded-lg transition-colors ${
                                    selectedPeriod === 'month'
                                        ? 'bg-primary-500 text-white'
                                        : 'bg-gray-800 text-gray-400 hover:bg-gray-700'
                                }`}
                            >
                                Miesiąc
                            </button>
                            <button
                                onClick={() => setSelectedPeriod('all')}
                                className={`px-4 py-2 rounded-lg transition-colors ${
                                    selectedPeriod === 'all'
                                        ? 'bg-primary-500 text-white'
                                        : 'bg-gray-800 text-gray-400 hover:bg-gray-700'
                                }`}
                            >
                                Wszystko
                            </button>
                        </div>
                    </div>
                </div>

                {/* Overview Cards */}
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-6">
                    <div className="bg-gray-900 rounded-lg p-6 border border-gray-800">
                        <div className="flex items-center justify-between mb-4">
                            <div className="w-12 h-12 bg-blue-500 bg-opacity-20 rounded-lg flex items-center justify-center">
                                <svg className="w-6 h-6 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                                </svg>
                            </div>
                        </div>
                        <h3 className="text-3xl font-bold text-white mb-1">{taskStats.total}</h3>
                        <p className="text-gray-400 text-sm">Wszystkie zadania</p>
                    </div>

                    <div className="bg-gray-900 rounded-lg p-6 border border-gray-800">
                        <div className="flex items-center justify-between mb-4">
                            <div className="w-12 h-12 bg-green-500 bg-opacity-20 rounded-lg flex items-center justify-center">
                                <svg className="w-6 h-6 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                </svg>
                            </div>
                        </div>
                        <h3 className="text-3xl font-bold text-white mb-1">{taskStats.completed}</h3>
                        <p className="text-gray-400 text-sm">Ukończone ({getCompletionRate()}%)</p>
                    </div>

                    <div className="bg-gray-900 rounded-lg p-6 border border-gray-800">
                        <div className="flex items-center justify-between mb-4">
                            <div className="w-12 h-12 bg-yellow-500 bg-opacity-20 rounded-lg flex items-center justify-center">
                                <svg className="w-6 h-6 text-yellow-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                                </svg>
                            </div>
                        </div>
                        <h3 className="text-3xl font-bold text-white mb-1">{taskStats.inProgress}</h3>
                        <p className="text-gray-400 text-sm">W trakcie</p>
                    </div>

                    <div className="bg-gray-900 rounded-lg p-6 border border-gray-800">
                        <div className="flex items-center justify-between mb-4">
                            <div className="w-12 h-12 bg-red-500 bg-opacity-20 rounded-lg flex items-center justify-center">
                                <svg className="w-6 h-6 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                                </svg>
                            </div>
                        </div>
                        <h3 className="text-3xl font-bold text-white mb-1">{overdueTasks.length}</h3>
                        <p className="text-gray-400 text-sm">Przeterminowane</p>
                    </div>
                </div>

                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
                    {/* Status Chart */}
                    <div className="bg-gray-900 rounded-lg p-6 border border-gray-800">
                        <h2 className="text-xl font-bold text-white mb-4">Status zadań</h2>
                        <div className="space-y-4">
                            <div>
                                <div className="flex items-center justify-between mb-2">
                                    <span className="text-gray-400">Nowe</span>
                                    <span className="text-white font-semibold">{taskStats.new}</span>
                                </div>
                                <div className="w-full bg-gray-800 rounded-full h-2">
                                    <div
                                        className="bg-blue-500 h-2 rounded-full transition-all"
                                        style={{ width: `${taskStats.total > 0 ? (taskStats.new / taskStats.total) * 100 : 0}%` }}
                                    />
                                </div>
                            </div>

                            <div>
                                <div className="flex items-center justify-between mb-2">
                                    <span className="text-gray-400">W trakcie</span>
                                    <span className="text-white font-semibold">{taskStats.inProgress}</span>
                                </div>
                                <div className="w-full bg-gray-800 rounded-full h-2">
                                    <div
                                        className="bg-yellow-500 h-2 rounded-full transition-all"
                                        style={{ width: `${taskStats.total > 0 ? (taskStats.inProgress / taskStats.total) * 100 : 0}%` }}
                                    />
                                </div>
                            </div>

                            <div>
                                <div className="flex items-center justify-between mb-2">
                                    <span className="text-gray-400">Ukończone</span>
                                    <span className="text-white font-semibold">{taskStats.completed}</span>
                                </div>
                                <div className="w-full bg-gray-800 rounded-full h-2">
                                    <div
                                        className="bg-green-500 h-2 rounded-full transition-all"
                                        style={{ width: `${taskStats.total > 0 ? (taskStats.completed / taskStats.total) * 100 : 0}%` }}
                                    />
                                </div>
                            </div>

                            <div>
                                <div className="flex items-center justify-between mb-2">
                                    <span className="text-gray-400">Anulowane</span>
                                    <span className="text-white font-semibold">{taskStats.cancelled}</span>
                                </div>
                                <div className="w-full bg-gray-800 rounded-full h-2">
                                    <div
                                        className="bg-gray-500 h-2 rounded-full transition-all"
                                        style={{ width: `${taskStats.total > 0 ? (taskStats.cancelled / taskStats.total) * 100 : 0}%` }}
                                    />
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Priority Chart */}
                    <div className="bg-gray-900 rounded-lg p-6 border border-gray-800">
                        <h2 className="text-xl font-bold text-white mb-4">Priorytety zadań</h2>
                        <div className="space-y-4">
                            <div>
                                <div className="flex items-center justify-between mb-2">
                                    <span className="text-gray-400">🔵 Niski</span>
                                    <span className="text-white font-semibold">{priorityStats.low}</span>
                                </div>
                                <div className="w-full bg-gray-800 rounded-full h-2">
                                    <div
                                        className="bg-blue-500 h-2 rounded-full transition-all"
                                        style={{ width: `${taskStats.total > 0 ? (priorityStats.low / taskStats.total) * 100 : 0}%` }}
                                    />
                                </div>
                            </div>

                            <div>
                                <div className="flex items-center justify-between mb-2">
                                    <span className="text-gray-400">🟡 Średni</span>
                                    <span className="text-white font-semibold">{priorityStats.medium}</span>
                                </div>
                                <div className="w-full bg-gray-800 rounded-full h-2">
                                    <div
                                        className="bg-yellow-500 h-2 rounded-full transition-all"
                                        style={{ width: `${taskStats.total > 0 ? (priorityStats.medium / taskStats.total) * 100 : 0}%` }}
                                    />
                                </div>
                            </div>

                            <div>
                                <div className="flex items-center justify-between mb-2">
                                    <span className="text-gray-400">🟠 Wysoki</span>
                                    <span className="text-white font-semibold">{priorityStats.high}</span>
                                </div>
                                <div className="w-full bg-gray-800 rounded-full h-2">
                                    <div
                                        className="bg-orange-500 h-2 rounded-full transition-all"
                                        style={{ width: `${taskStats.total > 0 ? (priorityStats.high / taskStats.total) * 100 : 0}%` }}
                                    />
                                </div>
                            </div>

                            <div>
                                <div className="flex items-center justify-between mb-2">
                                    <span className="text-gray-400">🔴 Pilny</span>
                                    <span className="text-white font-semibold">{priorityStats.urgent}</span>
                                </div>
                                <div className="w-full bg-gray-800 rounded-full h-2">
                                    <div
                                        className="bg-red-500 h-2 rounded-full transition-all"
                                        style={{ width: `${taskStats.total > 0 ? (priorityStats.urgent / taskStats.total) * 100 : 0}%` }}
                                    />
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Project Stats */}
                <div className="bg-gray-900 rounded-lg p-6 border border-gray-800 mb-6">
                    <h2 className="text-xl font-bold text-white mb-4">Statystyki projektów</h2>
                    <div className="overflow-x-auto">
                        <table className="w-full">
                            <thead>
                            <tr className="border-b border-gray-800">
                                <th className="text-left py-3 px-4 text-gray-400 font-semibold">Projekt</th>
                                <th className="text-center py-3 px-4 text-gray-400 font-semibold">Zadania</th>
                                <th className="text-center py-3 px-4 text-gray-400 font-semibold">Ukończone</th>
                                <th className="text-center py-3 px-4 text-gray-400 font-semibold">Postęp</th>
                            </tr>
                            </thead>
                            <tbody>
                            {projectStats.length > 0 ? (
                                projectStats.map(project => (
                                    <tr key={project.id} className="border-b border-gray-800 hover:bg-gray-800 transition-colors">
                                        <td className="py-3 px-4 text-white">{project.name}</td>
                                        <td className="py-3 px-4 text-center text-gray-400">{project.taskCount}</td>
                                        <td className="py-3 px-4 text-center text-gray-400">{project.completedCount}</td>
                                        <td className="py-3 px-4">
                                            <div className="flex items-center gap-3">
                                                <div className="flex-1 bg-gray-800 rounded-full h-2">
                                                    <div
                                                        className="bg-primary-500 h-2 rounded-full transition-all"
                                                        style={{ width: `${project.completionRate}%` }}
                                                    />
                                                </div>
                                                <span className="text-white font-semibold w-12 text-right">
                                                        {project.completionRate}%
                                                    </span>
                                            </div>
                                        </td>
                                    </tr>
                                ))
                            ) : (
                                <tr>
                                    <td colSpan={4} className="py-8 text-center text-gray-400">
                                        Brak projektów do wyświetlenia
                                    </td>
                                </tr>
                            )}
                            </tbody>
                        </table>
                    </div>
                </div>

                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                    {/* Overdue Tasks */}
                    <div className="bg-gray-900 rounded-lg p-6 border border-gray-800">
                        <h2 className="text-xl font-bold text-white mb-4 flex items-center gap-2">
                            <span className="text-red-500">⚠️</span> Przeterminowane zadania
                        </h2>
                        {overdueTasks.length > 0 ? (
                            <div className="space-y-3">
                                {overdueTasks.slice(0, 5).map(task => (
                                    <div key={task.id} className="p-3 bg-gray-800 rounded-lg hover:bg-gray-750 transition-colors">
                                        <h4 className="font-semibold text-white mb-1">{task.title}</h4>
                                        <div className="flex items-center gap-2 text-sm text-gray-400">
                                            <span>Termin: {new Date(task.deadline!).toLocaleDateString('pl-PL')}</span>
                                            {task.project && <span>• {task.project.name}</span>}
                                        </div>
                                    </div>
                                ))}
                                {overdueTasks.length > 5 && (
                                    <div className="text-center text-gray-400 text-sm">
                                        +{overdueTasks.length - 5} więcej
                                    </div>
                                )}
                            </div>
                        ) : (
                            <div className="text-center text-gray-400 py-8">
                                Brak przeterminowanych zadań! 🎉
                            </div>
                        )}
                    </div>

                    {/* Upcoming Tasks */}
                    <div className="bg-gray-900 rounded-lg p-6 border border-gray-800">
                        <h2 className="text-xl font-bold text-white mb-4 flex items-center gap-2">
                            <span className="text-yellow-500">📅</span> Nadchodzące terminy (7 dni)
                        </h2>
                        {upcomingTasks.length > 0 ? (
                            <div className="space-y-3">
                                {upcomingTasks.slice(0, 5).map(task => (
                                    <div key={task.id} className="p-3 bg-gray-800 rounded-lg hover:bg-gray-750 transition-colors">
                                        <h4 className="font-semibold text-white mb-1">{task.title}</h4>
                                        <div className="flex items-center gap-2 text-sm text-gray-400">
                                            <span>Termin: {new Date(task.deadline!).toLocaleDateString('pl-PL')}</span>
                                            {task.project && <span>• {task.project.name}</span>}
                                        </div>
                                    </div>
                                ))}
                                {upcomingTasks.length > 5 && (
                                    <div className="text-center text-gray-400 text-sm">
                                        +{upcomingTasks.length - 5} więcej
                                    </div>
                                )}
                            </div>
                        ) : (
                            <div className="text-center text-gray-400 py-8">
                                Brak nadchodzących terminów w tym tygodniu
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </MainLayout>
    );
};

export default ReportsPage;