// frontend/src/pages/ProjectDetailsPage.tsx - POPRAWIONA WERSJA BEZ BŁĘDÓW
import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import MainLayout from '../components/MainLayout';
import TaskStatusManager from '../components/TaskStatusManager';
import StatusRequestsPanel from '../components/StatusRequestsPanel';
import projectService from '../services/projectService';
import userService from '../services/userService';
import taskService from '../services/taskService';
import type { Project, ProjectMember, User, ProjectRole, Task, CreateTaskRequest } from '../types';

const ProjectDetailsPage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();

    const [project, setProject] = useState<Project | null>(null);
    const [members, setMembers] = useState<ProjectMember[]>([]);
    const [tasks, setTasks] = useState<Task[]>([]);
    const [allUsers, setAllUsers] = useState<User[]>([]);
    const [loading, setLoading] = useState(true);
    const [currentUserRole, setCurrentUserRole] = useState<ProjectRole>('VIEWER');

    // Tab state
    const [activeTab, setActiveTab] = useState<'tasks' | 'members' | 'requests'>('tasks');

    // Modals
    const [showAddMemberModal, setShowAddMemberModal] = useState(false);
    const [showEditModal, setShowEditModal] = useState(false);
    const [showCreateTaskModal, setShowCreateTaskModal] = useState(false);

    // Form states
    const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
    const [selectedRole, setSelectedRole] = useState<ProjectRole>('MEMBER');
    const [editFormData, setEditFormData] = useState({ name: '', description: '' });
    const [taskFormData, setTaskFormData] = useState<CreateTaskRequest>({
        title: '',
        description: '',
        priority: 'MEDIUM',
        deadline: '',
        projectId: 0,
        assignedUserIds: [],
    });

    const [error, setError] = useState<string | null>(null);
    const [actionInProgress, setActionInProgress] = useState(false);
    const groupUsersByTeam = (users: User[]) => {
        const teamGroups: { [teamName: string]: User[] } = {};
        const usersWithoutTeam: User[] = [];

        users.forEach(user => {
            if (user.teams && user.teams.length > 0) {
                user.teams.forEach(team => {
                    if (!teamGroups[team.name]) {
                        teamGroups[team.name] = [];
                    }
                    if (!teamGroups[team.name].some(u => u.id === user.id)) {
                        teamGroups[team.name].push(user);
                    }
                });
            } else {
                usersWithoutTeam.push(user);
            }
        });

        return { teamGroups, usersWithoutTeam };
    };

    useEffect(() => {
        if (id) {
            loadProjectData();
        }
    }, [id]);

    const loadProjectData = async () => {
        try {
            setLoading(true);
            const projectId = parseInt(id!);

            const [projectData, membersData, tasksData, usersData] = await Promise.all([
                projectService.getProjectById(projectId),
                projectService.getProjectMembers(projectId),
                taskService.getTasksByProject(projectId), // ✅ POPRAWIONE - właściwa nazwa metody
                userService.getAllUsers(),
            ]);

            setProject(projectData);
            setMembers(membersData);
            setTasks(tasksData);
            setAllUsers(usersData);

            // Determine current user's role
            const currentMember = membersData.find((m: ProjectMember) => m.user.username === localStorage.getItem('username')); // ✅ POPRAWIONE
            if (currentMember) {
                setCurrentUserRole(currentMember.role);
            }

            setError(null);
        } catch (err: any) {
            setError(err.message || 'Failed to load project data');
        } finally {
            setLoading(false);
        }
    };

    const handleCreateTask = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!taskFormData.title.trim()) {
            setError('Tytuł zadania jest wymagany');
            return;
        }

        try {
            setActionInProgress(true);
            setError(null);

            const taskData: CreateTaskRequest = {
                ...taskFormData,
                projectId: parseInt(id!),
                assignedUserIds: taskFormData.assignedUserIds || [], // ✅ POPRAWIONE
            };

            await taskService.createTask(taskData);
            await loadProjectData();
            setShowCreateTaskModal(false);
            setTaskFormData({
                title: '',
                description: '',
                priority: 'MEDIUM',
                deadline: '',
                projectId: 0,
                assignedUserIds: [],
            });
        } catch (err: any) {
            setError(err.message || 'Failed to create task');
        } finally {
            setActionInProgress(false);
        }
    };

    const handleAddMember = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!selectedUserId) {
            setError('Wybierz użytkownika');
            return;
        }

        try {
            setActionInProgress(true);
            setError(null);

            await projectService.addMemberToProject(parseInt(id!), selectedUserId, selectedRole); // ✅ POPRAWIONE
            await loadProjectData();

            setShowAddMemberModal(false);
            setSelectedUserId(null);
            setSelectedRole('MEMBER');
        } catch (err: any) {
            setError(err.message || 'Failed to add member');
        } finally {
            setActionInProgress(false);
        }
    };

    const handleRemoveMember = async (userId: number) => { // ✅ POPRAWIONE - użyj userId zamiast memberId
        if (!window.confirm('Czy na pewno chcesz usunąć tego członka z projektu?')) {
            return;
        }

        try {
            setError(null);
            await projectService.removeMemberFromProject(parseInt(id!), userId); // ✅ POPRAWIONE
            await loadProjectData();
        } catch (err: any) {
            setError(err.message || 'Failed to remove member');
        }
    };

    const handleUpdateProject = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!editFormData.name.trim()) {
            setError('Nazwa projektu jest wymagana');
            return;
        }

        try {
            setActionInProgress(true);
            setError(null);

            await projectService.updateProject(parseInt(id!), editFormData);
            await loadProjectData();
            setShowEditModal(false);
        } catch (err: any) {
            setError(err.message || 'Failed to update project');
        } finally {
            setActionInProgress(false);
        }
    };

    const getRoleColor = (role: ProjectRole): string => { // ✅ POPRAWIONE - dodano typ zwracany
        const colors: { [key in ProjectRole]: string } = { // ✅ POPRAWIONE - dodano typ
            ADMIN: 'bg-red-500',
            MEMBER: 'bg-blue-500',
            VIEWER: 'bg-gray-500',
        };
        return colors[role] || 'bg-gray-500';
    };

    const getPriorityColor = (priority: string): string => { // ✅ POPRAWIONE - dodano typ zwracany
        const colors: { [key: string]: string } = { // ✅ POPRAWIONE
            LOW: 'text-green-400',
            MEDIUM: 'text-yellow-400',
            HIGH: 'text-orange-400',
            URGENT: 'text-red-400',
        };
        return colors[priority] || 'text-gray-400';
    };

    if (loading) {
        return (
            <MainLayout>
                <div className="flex items-center justify-center h-64">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-emerald-500" />
                </div>
            </MainLayout>
        );
    }

    if (!project) {
        return (
            <MainLayout>
                <div className="text-center py-16">
                    <p className="text-gray-400 text-lg">Projekt nie został znaleziony</p>
                    <button
                        onClick={() => navigate('/projects')}
                        className="mt-4 px-6 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-lg transition"
                    >
                        Wróć do projektów
                    </button>
                </div>
            </MainLayout>
        );
    }

    const tasksByStatus = {
        NEW: tasks.filter((t: Task) => t.status === 'NEW').length,
        IN_PROGRESS: tasks.filter((t: Task) => t.status === 'IN_PROGRESS').length,
        COMPLETED: tasks.filter((t: Task) => t.status === 'COMPLETED').length,
    };

    const availableUsers = allUsers.filter(
        (user: User) => !members.some((member: ProjectMember) => member.user.id === user.id)
    );

    const availableMemberUsers = members.filter((m: ProjectMember) => m.role !== 'VIEWER');

    return (
        <MainLayout>
            <div className="max-w-7xl mx-auto">
                {/* Header */}
                <div className="flex items-start justify-between mb-8">
                    <div className="flex-1">
                        <button
                            onClick={() => navigate('/projects')}
                            className="flex items-center gap-2 text-gray-400 hover:text-white mb-4 transition"
                        >
                            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
                            </svg>
                            Wróć do projektów
                        </button>
                        <h1 className="text-3xl font-bold text-white mb-2">{project.name}</h1>
                        {project.description && (
                            <p className="text-gray-400">{project.description}</p>
                        )}
                        <div className="mt-2">
                            <span className={`px-3 py-1 rounded-full text-xs font-medium text-white ${getRoleColor(currentUserRole)}`}>
                                {currentUserRole === 'ADMIN' ? 'Administrator' : currentUserRole === 'MEMBER' ? 'Członek' : 'Obserwator'}
                            </span>
                        </div>
                    </div>
                    <div className="flex gap-2">
                        {currentUserRole === 'ADMIN' && (
                            <>
                                <button
                                    onClick={() => {
                                        setEditFormData({ name: project.name, description: project.description || '' });
                                        setShowEditModal(true);
                                    }}
                                    className="px-4 py-2 bg-gray-800 hover:bg-gray-700 text-white rounded-lg transition text-sm"
                                >
                                    Edytuj projekt
                                </button>
                            </>
                        )}
                    </div>
                </div>

                {error && (
                    <div className="mb-6 p-4 bg-red-500 bg-opacity-20 border border-red-500 rounded-lg text-red-500">
                        {error}
                    </div>
                )}

                {/* Stats Cards */}
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
                    <div className="bg-gray-900 border border-gray-800 rounded-lg p-6">
                        <div className="flex items-center gap-3">
                            <div className="w-10 h-10 bg-blue-500 bg-opacity-20 rounded-lg flex items-center justify-center">
                                <svg className="w-5 h-5 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                                </svg>
                            </div>
                            <div>
                                <p className="text-2xl font-bold text-white">{tasks.length}</p>
                                <p className="text-gray-400 text-sm">Wszystkich zadań</p>
                            </div>
                        </div>
                    </div>

                    <div className="bg-gray-900 border border-gray-800 rounded-lg p-6">
                        <div className="flex items-center gap-3">
                            <div className="w-10 h-10 bg-gray-500 bg-opacity-20 rounded-lg flex items-center justify-center">
                                <svg className="w-5 h-5 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                                </svg>
                            </div>
                            <div>
                                <p className="text-2xl font-bold text-white">{tasksByStatus.NEW}</p>
                                <p className="text-gray-400 text-sm">Nowych</p>
                            </div>
                        </div>
                    </div>

                    <div className="bg-gray-900 border border-gray-800 rounded-lg p-6">
                        <div className="flex items-center gap-3">
                            <div className="w-10 h-10 bg-blue-500 bg-opacity-20 rounded-lg flex items-center justify-center">
                                <svg className="w-5 h-5 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                                </svg>
                            </div>
                            <div>
                                <p className="text-2xl font-bold text-white">{tasksByStatus.IN_PROGRESS}</p>
                                <p className="text-gray-400 text-sm">W trakcie</p>
                            </div>
                        </div>
                    </div>

                    <div className="bg-gray-900 border border-gray-800 rounded-lg p-6">
                        <div className="flex items-center gap-3">
                            <div className="w-10 h-10 bg-green-500 bg-opacity-20 rounded-lg flex items-center justify-center">
                                <svg className="w-5 h-5 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                                </svg>
                            </div>
                            <div>
                                <p className="text-2xl font-bold text-white">{tasksByStatus.COMPLETED}</p>
                                <p className="text-gray-400 text-sm">Ukończonych</p>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Tabs Navigation */}
                <div className="mb-6 border-b border-gray-800">
                    <div className="flex gap-6">
                        <button
                            onClick={() => setActiveTab('tasks')}
                            className={`pb-4 px-2 font-medium transition ${
                                activeTab === 'tasks'
                                    ? 'text-emerald-500 border-b-2 border-emerald-500'
                                    : 'text-gray-400 hover:text-white'
                            }`}
                        >
                            Zadania ({tasks.length})
                        </button>
                        <button
                            onClick={() => setActiveTab('members')}
                            className={`pb-4 px-2 font-medium transition ${
                                activeTab === 'members'
                                    ? 'text-emerald-500 border-b-2 border-emerald-500'
                                    : 'text-gray-400 hover:text-white'
                            }`}
                        >
                            Członkowie ({members.length})
                        </button>
                        {currentUserRole === 'ADMIN' && (
                            <button
                                onClick={() => setActiveTab('requests')}
                                className={`pb-4 px-2 font-medium transition ${
                                    activeTab === 'requests'
                                        ? 'text-emerald-500 border-b-2 border-emerald-500'
                                        : 'text-gray-400 hover:text-white'
                                }`}
                            >
                                Prośby o zmianę statusu
                            </button>
                        )}
                    </div>
                </div>

                {/* Tab Content */}
                {activeTab === 'tasks' && (
                    <div className="bg-gray-900 border border-gray-800 rounded-lg p-6 mb-8">
                        <div className="flex items-center justify-between mb-6">
                            <h2 className="text-xl font-bold text-white">Zadania</h2>
                            {(currentUserRole === 'ADMIN' || currentUserRole === 'MEMBER') && (
                                <button
                                    onClick={() => setShowCreateTaskModal(true)}
                                    className="flex items-center gap-2 px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-lg transition text-sm"
                                >
                                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                                    </svg>
                                    Nowe zadanie
                                </button>
                            )}
                        </div>

                        {tasks.length === 0 ? (
                            <div className="text-center py-12">
                                <div className="w-16 h-16 bg-gray-800 rounded-full flex items-center justify-center mx-auto mb-4">
                                    <svg className="w-8 h-8 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                                    </svg>
                                </div>
                                <h3 className="text-gray-400 font-medium mb-2">Brak zadań</h3>
                                <p className="text-gray-500 text-sm">Utwórz pierwsze zadanie w tym projekcie</p>
                            </div>
                        ) : (
                            <div className="space-y-3">
                                {tasks.map((task: Task) => (
                                    <div
                                        key={task.id}
                                        className="bg-gray-800 border border-gray-700 rounded-lg p-4 hover:border-gray-600 transition cursor-pointer"
                                        onClick={() => navigate(`/tasks/${task.id}`)}
                                    >
                                        <div className="flex items-start justify-between gap-4">
                                            <div className="flex-1">
                                                <h3 className="text-white font-medium mb-2">{task.title}</h3>
                                                {task.description && (
                                                    <p className="text-gray-400 text-sm mb-3 line-clamp-2">{task.description}</p>
                                                )}
                                                <div className="flex items-center gap-3 text-xs">
                                                    <span className={getPriorityColor(task.priority)}>
                                                        {task.priority === 'LOW' && '🟢 Niski'}
                                                        {task.priority === 'MEDIUM' && '🟡 Średni'}
                                                        {task.priority === 'HIGH' && '🟠 Wysoki'}
                                                        {task.priority === 'URGENT' && '🔴 Pilny'}
                                                    </span>
                                                    {task.deadline && (
                                                        <>
                                                            <span className="text-gray-600">•</span>
                                                            <span className="text-gray-400">
                                                                📅 {new Date(task.deadline).toLocaleDateString('pl-PL')}
                                                            </span>
                                                        </>
                                                    )}
                                                    {task.assignedUsers && task.assignedUsers.length > 0 && (
                                                        <>
                                                            <span className="text-gray-600">•</span>
                                                            <span className="text-gray-400">
                                                                👤 {task.assignedUsers.map((u: User) => u.username).join(', ')}
                                                            </span>
                                                        </>
                                                    )}
                                                </div>
                                            </div>
                                            <div onClick={(e) => e.stopPropagation()}>
                                                <TaskStatusManager
                                                    task={task}
                                                    userRole={currentUserRole}
                                                    onStatusChanged={loadProjectData}
                                                />
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                )}

                {activeTab === 'members' && (
                    <div className="bg-gray-900 border border-gray-800 rounded-lg p-6 mb-8">
                        <div className="flex items-center justify-between mb-6">
                            <h2 className="text-xl font-bold text-white">Członkowie projektu</h2>
                            {currentUserRole === 'ADMIN' && (
                                <button
                                    onClick={() => setShowAddMemberModal(true)}
                                    className="flex items-center gap-2 px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-lg transition text-sm"
                                >
                                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                                    </svg>
                                    Dodaj członka
                                </button>
                            )}
                        </div>

                        <div className="space-y-3">
                            {members.map((member: ProjectMember) => (
                                <div key={member.id} className="bg-gray-800 border border-gray-700 rounded-lg p-4 flex items-center justify-between">
                                    <div>
                                        <p className="text-white font-medium">{member.user.username}</p>
                                        <p className="text-gray-400 text-sm">{member.user.email}</p>
                                    </div>
                                    <div className="flex items-center gap-3">
                                        <span className={`px-3 py-1 rounded-full text-xs font-medium text-white ${getRoleColor(member.role)}`}>
                                            {member.role === 'ADMIN' ? 'Administrator' : member.role === 'MEMBER' ? 'Członek' : 'Obserwator'}
                                        </span>
                                        {currentUserRole === 'ADMIN' && member.role !== 'ADMIN' && (
                                            <button
                                                onClick={() => handleRemoveMember(member.user.id)}
                                                className="px-3 py-1 bg-red-600 hover:bg-red-700 text-white rounded text-xs font-medium transition"
                                            >
                                                Usuń
                                            </button>
                                        )}
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                )}

                {activeTab === 'requests' && currentUserRole === 'ADMIN' && (
                    <StatusRequestsPanel
                        projectId={parseInt(id!)}
                        onRequestHandled={loadProjectData}
                    />
                )}

                {/* Create Task Modal */}
                {showCreateTaskModal && (
                    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
                        <div className="bg-gray-900 border border-gray-800 rounded-lg p-6 max-w-2xl w-full max-h-[90vh] overflow-y-auto">
                            <h3 className="text-xl font-bold text-white mb-4">Utwórz nowe zadanie</h3>

                            <form onSubmit={handleCreateTask} className="space-y-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-300 mb-2">
                                        Tytuł zadania *
                                    </label>
                                    <input
                                        type="text"
                                        value={taskFormData.title}
                                        onChange={(e) => setTaskFormData({ ...taskFormData, title: e.target.value })}
                                        className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-emerald-500"
                                        placeholder="Np. Implementacja nowej funkcji"
                                        required
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-300 mb-2">
                                        Opis
                                    </label>
                                    <textarea
                                        value={taskFormData.description}
                                        onChange={(e) => setTaskFormData({ ...taskFormData, description: e.target.value })}
                                        className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-emerald-500"
                                        placeholder="Szczegółowy opis zadania..."
                                        rows={4}
                                    />
                                </div>

                                <div className="grid grid-cols-2 gap-4">
                                    <div>
                                        <label className="block text-sm font-medium text-gray-300 mb-2">
                                            Priorytet
                                        </label>
                                        <select
                                            value={taskFormData.priority}
                                            onChange={(e) => setTaskFormData({ ...taskFormData, priority: e.target.value as any })}
                                            className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-emerald-500"
                                        >
                                            <option value="LOW">Niski</option>
                                            <option value="MEDIUM">Średni</option>
                                            <option value="HIGH">Wysoki</option>
                                            <option value="URGENT">Pilny</option>
                                        </select>
                                    </div>

                                    <div>
                                        <label className="block text-sm font-medium text-gray-300 mb-2">
                                            Termin
                                        </label>
                                        <input
                                            type="datetime-local"
                                            value={taskFormData.deadline}
                                            onChange={(e) => setTaskFormData({ ...taskFormData, deadline: e.target.value })}
                                            className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-emerald-500"
                                        />
                                    </div>
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-300 mb-2">
                                        Przypisz do użytkowników
                                    </label>
                                    <select
                                        multiple
                                        value={(taskFormData.assignedUserIds || []).map(String)}
                                        onChange={(e) => {
                                            const selectedOptions = Array.from(e.target.selectedOptions, option => parseInt(option.value));
                                            setTaskFormData({ ...taskFormData, assignedUserIds: selectedOptions });
                                        }}
                                        className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-emerald-500"
                                        size={5}
                                    >
                                        {availableMemberUsers.map((member: ProjectMember) => (
                                            <option key={member.user.id} value={member.user.id}>
                                                {member.user.username} ({member.user.email})
                                            </option>
                                        ))}
                                    </select>
                                    <p className="text-xs text-gray-500 mt-1">Przytrzymaj Ctrl/Cmd aby wybrać wielu użytkowników</p>
                                </div>

                                <div className="flex gap-3">
                                    <button
                                        type="button"
                                        onClick={() => {
                                            setShowCreateTaskModal(false);
                                            setTaskFormData({
                                                title: '',
                                                description: '',
                                                priority: 'MEDIUM',
                                                deadline: '',
                                                projectId: 0,
                                                assignedUserIds: [],
                                            });
                                        }}
                                        className="flex-1 px-4 py-2 bg-gray-800 hover:bg-gray-700 text-white rounded-lg transition"
                                        disabled={actionInProgress}
                                    >
                                        Anuluj
                                    </button>
                                    <button
                                        type="submit"
                                        className="flex-1 px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-lg transition disabled:opacity-50"
                                        disabled={actionInProgress}
                                    >
                                        {actionInProgress ? 'Tworzenie...' : 'Utwórz zadanie'}
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                )}

                {/* Add Member Modal */}
                {showAddMemberModal && (
                    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
                        <div className="bg-gray-900 border border-gray-800 rounded-lg p-6 max-w-md w-full">
                            <h3 className="text-xl font-bold text-white mb-4">Dodaj członka do projektu</h3>

                            <form onSubmit={handleAddMember} className="space-y-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-300 mb-2">
                                        Wybierz użytkownika
                                    </label>
                                    <select
                                        value={selectedUserId || ''}
                                        onChange={(e) => setSelectedUserId(parseInt(e.target.value))}
                                        className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-emerald-500"
                                        required
                                        size={8}
                                    >
                                        <option value="">-- Wybierz użytkownika --</option>
                                        {(() => {
                                            const { teamGroups, usersWithoutTeam } = groupUsersByTeam(availableUsers);
                                            const sortedTeamNames = Object.keys(teamGroups).sort();

                                            return (
                                                <>
                                                    {/* Użytkownicy pogrupowani według zespołów */}
                                                    {sortedTeamNames.map((teamName) => {
                                                        const sortedUsers = [...teamGroups[teamName]].sort((a, b) =>
                                                            a.username.localeCompare(b.username)
                                                        );

                                                        return (
                                                            <optgroup
                                                                key={teamName}
                                                                label={teamName}
                                                                style={{ fontWeight: 'bold', color: '#10b981' }}
                                                            >
                                                                {sortedUsers.map(user => (
                                                                    <option
                                                                        key={user.id}
                                                                        value={user.id}
                                                                        style={{ paddingLeft: '20px', fontWeight: 'normal' }}
                                                                    >
                                                                        &nbsp;&nbsp;{user.username} ({user.email})
                                                                    </option>
                                                                ))}
                                                            </optgroup>
                                                        );
                                                    })}

                                                    {/* Użytkownicy bez zespołu */}
                                                    {usersWithoutTeam.length > 0 && (
                                                        <optgroup
                                                            label="Użytkownicy bez zespołu"
                                                            style={{ fontWeight: 'bold', color: '#6b7280' }}
                                                        >
                                                            {usersWithoutTeam
                                                                .sort((a, b) => a.username.localeCompare(b.username))
                                                                .map(user => (
                                                                    <option
                                                                        key={user.id}
                                                                        value={user.id}
                                                                        style={{ paddingLeft: '20px', fontWeight: 'normal' }}
                                                                    >
                                                                        &nbsp;&nbsp;{user.username} ({user.email})
                                                                    </option>
                                                                ))
                                                            }
                                                        </optgroup>
                                                    )}
                                                </>
                                            );
                                        })()}
                                    </select>
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-300 mb-2">
                                        Rola
                                    </label>
                                    <select
                                        value={selectedRole}
                                        onChange={(e) => setSelectedRole(e.target.value as ProjectRole)}
                                        className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-emerald-500"
                                    >
                                        <option value="VIEWER">Obserwator</option>
                                        <option value="MEMBER">Członek</option>
                                        <option value="ADMIN">Administrator</option>
                                    </select>
                                </div>

                                <div className="flex gap-3">
                                    <button
                                        type="button"
                                        onClick={() => {
                                            setShowAddMemberModal(false);
                                            setSelectedUserId(null);
                                            setSelectedRole('MEMBER');
                                        }}
                                        className="flex-1 px-4 py-2 bg-gray-800 hover:bg-gray-700 text-white rounded-lg transition"
                                        disabled={actionInProgress}
                                    >
                                        Anuluj
                                    </button>
                                    <button
                                        type="submit"
                                        className="flex-1 px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-lg transition disabled:opacity-50"
                                        disabled={actionInProgress}
                                    >
                                        {actionInProgress ? 'Dodawanie...' : 'Dodaj członka'}
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                )}

                {/* Edit Project Modal */}
                {showEditModal && (
                    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
                        <div className="bg-gray-900 border border-gray-800 rounded-lg p-6 max-w-md w-full">
                            <h3 className="text-xl font-bold text-white mb-4">Edytuj projekt</h3>

                            <form onSubmit={handleUpdateProject} className="space-y-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-300 mb-2">
                                        Nazwa projektu *
                                    </label>
                                    <input
                                        type="text"
                                        value={editFormData.name}
                                        onChange={(e) => setEditFormData({ ...editFormData, name: e.target.value })}
                                        className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-emerald-500"
                                        required
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-300 mb-2">
                                        Opis
                                    </label>
                                    <textarea
                                        value={editFormData.description}
                                        onChange={(e) => setEditFormData({ ...editFormData, description: e.target.value })}
                                        className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-emerald-500"
                                        rows={4}
                                    />
                                </div>

                                <div className="flex gap-3">
                                    <button
                                        type="button"
                                        onClick={() => setShowEditModal(false)}
                                        className="flex-1 px-4 py-2 bg-gray-800 hover:bg-gray-700 text-white rounded-lg transition"
                                        disabled={actionInProgress}
                                    >
                                        Anuluj
                                    </button>
                                    <button
                                        type="submit"
                                        className="flex-1 px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-lg transition disabled:opacity-50"
                                        disabled={actionInProgress}
                                    >
                                        {actionInProgress ? 'Zapisywanie...' : 'Zapisz'}
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                )}
            </div>
        </MainLayout>
    );
};

export default ProjectDetailsPage;