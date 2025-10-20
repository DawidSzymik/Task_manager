// src/pages/ProjectDetailsPage.tsx
import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import MainLayout from '../components/MainLayout';
import projectService from '../services/projectService';
import userService from '../services/userService';
import taskService from '../services/taskService';
import type { Project, ProjectMember, User, ProjectRole, Task, CreateTaskRequest, TaskPriority } from '../types';

const ProjectDetailsPage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();

    const [project, setProject] = useState<Project | null>(null);
    const [members, setMembers] = useState<ProjectMember[]>([]);
    const [tasks, setTasks] = useState<Task[]>([]);
    const [allUsers, setAllUsers] = useState<User[]>([]);
    const [loading, setLoading] = useState(true);

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
        assignedToId: undefined,
    });

    const [error, setError] = useState<string | null>(null);
    const [actionInProgress, setActionInProgress] = useState(false);

    useEffect(() => {
        if (id) {
            loadProjectData();
        }
    }, [id]);

    const loadProjectData = async () => {
        try {
            setLoading(true);
            const projectId = parseInt(id!);

            const [projectData, membersData, usersData, tasksData] = await Promise.all([
                projectService.getProjectById(projectId),
                projectService.getProjectMembers(projectId),
                userService.getAllUsers(undefined, true),
                taskService.getTasksByProject(projectId),
            ]);

            setProject(projectData);
            setMembers(membersData);
            setAllUsers(usersData);
            setTasks(tasksData);
            setEditFormData({
                name: projectData.name,
                description: projectData.description || '',
            });
            setTaskFormData(prev => ({ ...prev, projectId }));
        } catch (error: any) {
            console.error('Failed to load project:', error);
            setError(error.message || 'Nie udało się załadować danych projektu');
        } finally {
            setLoading(false);
        }
    };

    // Member management handlers
    const handleAddMember = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!selectedUserId || !id) return;

        try {
            setActionInProgress(true);
            setError(null);
            await projectService.addMemberToProject(parseInt(id), selectedUserId, selectedRole);
            setShowAddMemberModal(false);
            setSelectedUserId(null);
            setSelectedRole('MEMBER');
            await loadProjectData();
        } catch (error: any) {
            console.error('Failed to add member:', error);
            setError(error.message || 'Nie udało się dodać członka');
        } finally {
            setActionInProgress(false);
        }
    };

    const handleRemoveMember = async (userId: number, username: string) => {
        if (!window.confirm(`Czy na pewno chcesz usunąć ${username} z projektu?`)) return;

        try {
            setActionInProgress(true);
            await projectService.removeMemberFromProject(parseInt(id!), userId);
            await loadProjectData();
        } catch (error: any) {
            console.error('Failed to remove member:', error);
            alert(error.message || 'Nie udało się usunąć członka');
        } finally {
            setActionInProgress(false);
        }
    };

    const handleChangeRole = async (userId: number, currentRole: ProjectRole) => {
        const newRole = prompt(`Zmień rolę użytkownika (ADMIN, MEMBER, VIEWER):`, currentRole);
        if (!newRole || newRole === currentRole) return;

        const validRoles = ['ADMIN', 'MEMBER', 'VIEWER'];
        if (!validRoles.includes(newRole.toUpperCase())) {
            alert('Nieprawidłowa rola! Dozwolone: ADMIN, MEMBER, VIEWER');
            return;
        }

        try {
            setActionInProgress(true);
            await projectService.changeMemberRole(parseInt(id!), userId, newRole.toUpperCase() as ProjectRole);
            await loadProjectData();
        } catch (error: any) {
            console.error('Failed to change role:', error);
            alert(error.message || 'Nie udało się zmienić roli');
        } finally {
            setActionInProgress(false);
        }
    };

    // Task management handlers
    const handleCreateTask = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!taskFormData.title.trim()) {
            setError('Tytuł zadania jest wymagany');
            return;
        }

        try {
            setActionInProgress(true);
            setError(null);
            await taskService.createTask(taskFormData);
            setShowCreateTaskModal(false);
            setTaskFormData({
                title: '',
                description: '',
                priority: 'MEDIUM',
                deadline: '',
                projectId: parseInt(id!),
                assignedToId: undefined,
            });
            await loadProjectData();
        } catch (error: any) {
            console.error('Failed to create task:', error);
            setError(error.message || 'Nie udało się utworzyć zadania');
        } finally {
            setActionInProgress(false);
        }
    };

    const handleDeleteTask = async (taskId: number, taskTitle: string) => {
        if (!window.confirm(`Czy na pewno chcesz usunąć zadanie "${taskTitle}"?`)) return;

        try {
            await taskService.deleteTask(taskId);
            await loadProjectData();
        } catch (error: any) {
            console.error('Failed to delete task:', error);
            alert(error.message || 'Nie udało się usunąć zadania');
        }
    };

    // Project management handlers
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
            setShowEditModal(false);
            await loadProjectData();
        } catch (error: any) {
            console.error('Failed to update project:', error);
            setError(error.message || 'Nie udało się zaktualizować projektu');
        } finally {
            setActionInProgress(false);
        }
    };

    const handleDeleteProject = async () => {
        if (!window.confirm(`Czy na pewno chcesz usunąć projekt "${project?.name}"?\n\nTa operacja usunie wszystkie zadania i jest nieodwracalna!`)) return;

        try {
            await projectService.deleteProject(parseInt(id!));
            navigate('/projects');
        } catch (error: any) {
            console.error('Failed to delete project:', error);
            alert(error.message || 'Nie udało się usunąć projektu');
        }
    };

    const getRoleBadge = (role: ProjectRole) => {
        const colors = {
            ADMIN: 'bg-red-500',
            MEMBER: 'bg-blue-500',
            VIEWER: 'bg-gray-500',
        };
        const labels = {
            ADMIN: 'Admin',
            MEMBER: 'Członek',
            VIEWER: 'Obserwator',
        };
        return { color: colors[role], label: labels[role] };
    };

    const getStatusColor = (status: string) => {
        switch (status) {
            case 'NEW': return 'bg-blue-500';
            case 'IN_PROGRESS': return 'bg-yellow-500';
            case 'COMPLETED': return 'bg-green-500';
            case 'CANCELLED': return 'bg-gray-500';
            default: return 'bg-gray-500';
        }
    };

    const getStatusLabel = (status: string) => {
        switch (status) {
            case 'NEW': return 'Nowe';
            case 'IN_PROGRESS': return 'W trakcie';
            case 'COMPLETED': return 'Ukończone';
            case 'CANCELLED': return 'Anulowane';
            default: return status;
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
                        <p className="text-gray-400">Ładowanie projektu...</p>
                    </div>
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
        NEW: tasks.filter(t => t.status === 'NEW').length,
        IN_PROGRESS: tasks.filter(t => t.status === 'IN_PROGRESS').length,
        COMPLETED: tasks.filter(t => t.status === 'COMPLETED').length,
    };

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
                    </div>
                    <div className="flex gap-2">
                        <button
                            onClick={() => setShowEditModal(true)}
                            className="px-4 py-2 bg-gray-800 hover:bg-gray-700 text-white rounded-lg transition"
                        >
                            Edytuj
                        </button>
                        <button
                            onClick={handleDeleteProject}
                            className="px-4 py-2 bg-red-500 bg-opacity-10 hover:bg-red-500 hover:bg-opacity-20 text-red-400 rounded-lg transition"
                        >
                            Usuń projekt
                        </button>
                    </div>
                </div>

                {/* Error message */}
                {error && (
                    <div className="mb-6 p-4 bg-red-500 bg-opacity-10 border border-red-500 rounded-lg text-red-400">
                        {error}
                    </div>
                )}

                {/* Stats */}
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
                    <div className="bg-gray-900 border border-gray-800 rounded-lg p-6">
                        <div className="flex items-center gap-3">
                            <div className="w-10 h-10 bg-emerald-500 bg-opacity-20 rounded-lg flex items-center justify-center">
                                <svg className="w-5 h-5 text-emerald-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                                </svg>
                            </div>
                            <div>
                                <p className="text-2xl font-bold text-white">{members.length}</p>
                                <p className="text-gray-400 text-sm">Członków</p>
                            </div>
                        </div>
                    </div>

                    <div className="bg-gray-900 border border-gray-800 rounded-lg p-6">
                        <div className="flex items-center gap-3">
                            <div className="w-10 h-10 bg-blue-500 bg-opacity-20 rounded-lg flex items-center justify-center">
                                <svg className="w-5 h-5 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
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
                            <div className="w-10 h-10 bg-yellow-500 bg-opacity-20 rounded-lg flex items-center justify-center">
                                <svg className="w-5 h-5 text-yellow-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
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

                {/* Tasks Section */}
                <div className="bg-gray-900 border border-gray-800 rounded-lg p-6 mb-8">
                    <div className="flex items-center justify-between mb-6">
                        <h2 className="text-xl font-bold text-white">Zadania</h2>
                        <button
                            onClick={() => setShowCreateTaskModal(true)}
                            className="flex items-center gap-2 px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-lg transition text-sm"
                        >
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                            </svg>
                            Nowe zadanie
                        </button>
                    </div>

                    {tasks.length === 0 ? (
                        <div className="text-center py-8">
                            <svg className="w-16 h-16 text-gray-600 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                            </svg>
                            <p className="text-gray-400">Projekt nie ma jeszcze żadnych zadań</p>
                        </div>
                    ) : (
                        <div className="space-y-3">
                            {tasks.map((task) => (
                                <div
                                    key={task.id}
                                    className="flex items-center justify-between p-4 bg-gray-800 rounded-lg hover:bg-gray-750 transition cursor-pointer"
                                    onClick={() => navigate(`/tasks/${task.id}`)}
                                >
                                    <div className="flex items-center gap-3 flex-1">
                                        <span className={`px-3 py-1 rounded-full text-xs font-semibold ${getStatusColor(task.status)} text-white`}>
                                            {getStatusLabel(task.status)}
                                        </span>
                                        <span className="text-white font-medium">{task.title}</span>
                                        {task.assignedTo && (
                                            <span className="text-gray-400 text-sm">
                                                → {task.assignedTo.username}
                                            </span>
                                        )}
                                    </div>
                                    <button
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            handleDeleteTask(task.id, task.title);
                                        }}
                                        className="p-2 text-red-400 hover:bg-red-500 hover:bg-opacity-10 rounded transition"
                                    >
                                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                        </svg>
                                    </button>
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                {/* Members Section */}
                <div className="bg-gray-900 border border-gray-800 rounded-lg p-6 mb-8">
                    <div className="flex items-center justify-between mb-6">
                        <h2 className="text-xl font-bold text-white">Członkowie projektu</h2>
                        <button
                            onClick={() => setShowAddMemberModal(true)}
                            className="flex items-center gap-2 px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-lg transition text-sm"
                        >
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                            </svg>
                            Dodaj członka
                        </button>
                    </div>

                    {members.length === 0 ? (
                        <div className="text-center py-8">
                            <svg className="w-16 h-16 text-gray-600 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                            </svg>
                            <p className="text-gray-400">Projekt nie ma jeszcze żadnych członków</p>
                        </div>
                    ) : (
                        <div className="space-y-3">
                            {members.map((member) => {
                                const badge = getRoleBadge(member.role);
                                return (
                                    <div
                                        key={member.id}
                                        className="flex items-center justify-between p-4 bg-gray-800 rounded-lg hover:bg-gray-750 transition"
                                    >
                                        <div className="flex items-center gap-3 flex-1">
                                            <div className="w-10 h-10 bg-emerald-500 rounded-full flex items-center justify-center">
                                                <span className="text-white font-semibold">
                                                    {member.user.username.charAt(0).toUpperCase()}
                                                </span>
                                            </div>
                                            <div>
                                                <p className="text-white font-medium">{member.user.username}</p>
                                                <p className="text-gray-400 text-sm">{member.user.email || 'Brak email'}</p>
                                            </div>
                                            <span className={`ml-4 px-3 py-1 rounded-full text-xs font-semibold ${badge.color} text-white`}>
                                                {badge.label}
                                            </span>
                                        </div>
                                        <div className="flex gap-2">
                                            <button
                                                onClick={() => handleChangeRole(member.user.id, member.role)}
                                                className="p-2 text-blue-400 hover:bg-blue-500 hover:bg-opacity-10 rounded transition"
                                                title="Zmień rolę"
                                            >
                                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4" />
                                                </svg>
                                            </button>
                                            <button
                                                onClick={() => handleRemoveMember(member.user.id, member.user.username)}
                                                className="p-2 text-red-400 hover:bg-red-500 hover:bg-opacity-10 rounded transition"
                                                title="Usuń członka"
                                            >
                                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                                </svg>
                                            </button>
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </div>

                {/* Create Task Modal */}
                {showCreateTaskModal && (
                    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                        <div className="bg-gray-900 rounded-lg p-6 w-full max-w-md border border-gray-800">
                            <h2 className="text-2xl font-bold text-white mb-4">Utwórz nowe zadanie</h2>

                            {error && (
                                <div className="mb-4 p-3 bg-red-500 bg-opacity-10 border border-red-500 rounded text-red-400 text-sm">
                                    {error}
                                </div>
                            )}

                            <form onSubmit={handleCreateTask}>
                                <div className="mb-4">
                                    <label className="block text-gray-400 text-sm font-medium mb-2">Tytuł *</label>
                                    <input
                                        type="text"
                                        value={taskFormData.title}
                                        onChange={(e) => setTaskFormData({ ...taskFormData, title: e.target.value })}
                                        className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-emerald-500"
                                        placeholder="Wpisz tytuł zadania..."
                                        maxLength={200}
                                        required
                                    />
                                </div>

                                <div className="mb-4">
                                    <label className="block text-gray-400 text-sm font-medium mb-2">Opis</label>
                                    <textarea
                                        value={taskFormData.description}
                                        onChange={(e) => setTaskFormData({ ...taskFormData, description: e.target.value })}
                                        className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-emerald-500"
                                        placeholder="Opisz zadanie..."
                                        rows={3}
                                        maxLength={2000}
                                    />
                                </div>

                                <div className="mb-4">
                                    <label className="block text-gray-400 text-sm font-medium mb-2">Priorytet</label>
                                    <select
                                        value={taskFormData.priority}
                                        onChange={(e) => setTaskFormData({ ...taskFormData, priority: e.target.value as TaskPriority })}
                                        className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-emerald-500"
                                    >
                                        <option value="LOW">Niski</option>
                                        <option value="MEDIUM">Średni</option>
                                        <option value="HIGH">Wysoki</option>
                                        <option value="URGENT">Pilny</option>
                                    </select>
                                </div>

                                <div className="mb-4">
                                    <label className="block text-gray-400 text-sm font-medium mb-2">Deadline</label>
                                    <input
                                        type="datetime-local"
                                        value={taskFormData.deadline}
                                        onChange={(e) => setTaskFormData({ ...taskFormData, deadline: e.target.value })}
                                        className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-emerald-500"
                                    />
                                </div>

                                <div className="mb-6">
                                    <label className="block text-gray-400 text-sm font-medium mb-2">Przypisz do</label>
                                    <select
                                        value={taskFormData.assignedToId || ''}
                                        onChange={(e) => setTaskFormData({ ...taskFormData, assignedToId: e.target.value ? Number(e.target.value) : undefined })}
                                        className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-emerald-500"
                                    >
                                        <option value="">Nie przypisano</option>
                                        {members.map((member) => (
                                            <option key={member.user.id} value={member.user.id}>
                                                {member.user.username}
                                            </option>
                                        ))}
                                    </select>
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
                                                projectId: parseInt(id!),
                                                assignedToId: undefined,
                                            });
                                            setError(null);
                                        }}
                                        className="flex-1 px-4 py-2 bg-gray-800 hover:bg-gray-700 text-white rounded-lg transition"
                                        disabled={actionInProgress}
                                    >
                                        Anuluj
                                    </button>
                                    <button
                                        type="submit"
                                        className="flex-1 px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-lg transition disabled:opacity-50 disabled:cursor-not-allowed"
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
                    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                        <div className="bg-gray-900 rounded-lg p-6 w-full max-w-md border border-gray-800">
                            <h2 className="text-2xl font-bold text-white mb-4">Dodaj członka</h2>

                            <form onSubmit={handleAddMember}>
                                <div className="mb-4">
                                    <label className="block text-gray-400 text-sm font-medium mb-2">Użytkownik *</label>
                                    <select
                                        value={selectedUserId || ''}
                                        onChange={(e) => setSelectedUserId(e.target.value ? Number(e.target.value) : null)}
                                        className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-emerald-500"
                                        required
                                    >
                                        <option value="">Wybierz użytkownika</option>
                                        {allUsers
                                            .filter(u => !members.some(m => m.user.id === u.id))
                                            .map((user) => (
                                                <option key={user.id} value={user.id}>
                                                    {user.username} {user.email ? `(${user.email})` : ''}
                                                </option>
                                            ))}
                                    </select>
                                </div>

                                <div className="mb-6">
                                    <label className="block text-gray-400 text-sm font-medium mb-2">Rola *</label>
                                    <select
                                        value={selectedRole}
                                        onChange={(e) => setSelectedRole(e.target.value as ProjectRole)}
                                        className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-emerald-500"
                                        required
                                    >
                                        <option value="ADMIN">Admin - pełne uprawnienia</option>
                                        <option value="MEMBER">Członek - może edytować zadania</option>
                                        <option value="VIEWER">Obserwator - tylko podgląd</option>
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
                                        {actionInProgress ? 'Dodawanie...' : 'Dodaj'}
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                )}

                {/* Edit Project Modal */}
                {showEditModal && (
                    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                        <div className="bg-gray-900 rounded-lg p-6 w-full max-w-md border border-gray-800">
                            <h2 className="text-2xl font-bold text-white mb-4">Edytuj projekt</h2>

                            <form onSubmit={handleUpdateProject}>
                                <div className="mb-4">
                                    <label className="block text-gray-400 text-sm font-medium mb-2">Nazwa projektu *</label>
                                    <input
                                        type="text"
                                        value={editFormData.name}
                                        onChange={(e) => setEditFormData({ ...editFormData, name: e.target.value })}
                                        className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-emerald-500"
                                        maxLength={100}
                                        required
                                    />
                                </div>

                                <div className="mb-6">
                                    <label className="block text-gray-400 text-sm font-medium mb-2">Opis</label>
                                    <textarea
                                        value={editFormData.description}
                                        onChange={(e) => setEditFormData({ ...editFormData, description: e.target.value })}
                                        className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-emerald-500"
                                        rows={3}
                                        maxLength={500}
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