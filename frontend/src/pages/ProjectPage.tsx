// src/pages/ProjectsPage.tsx
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import MainLayout from '../components/MainLayout';
import projectService from '../services/projectService';
import type { Project, CreateProjectRequest } from '../types';

const ProjectsPage: React.FC = () => {
    const navigate = useNavigate();
    const [projects, setProjects] = useState<Project[]>([]);
    const [loading, setLoading] = useState(true);
    const [showCreateModal, setShowCreateModal] = useState(false);
    const [formData, setFormData] = useState<CreateProjectRequest>({
        name: '',
        description: '',
    });
    const [error, setError] = useState<string | null>(null);
    const [creating, setCreating] = useState(false);

    useEffect(() => {
        loadProjects();
    }, []);

    const loadProjects = async () => {
        try {
            setLoading(true);
            const data = await projectService.getAllProjects();
            setProjects(data);
        } catch (error: any) {
            console.error('Failed to load projects:', error);
            setError(error.message || 'Nie udało się załadować projektów');
        } finally {
            setLoading(false);
        }
    };

    const handleCreateProject = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!formData.name.trim()) {
            setError('Nazwa projektu jest wymagana');
            return;
        }

        try {
            setCreating(true);
            setError(null);
            await projectService.createProject(formData);
            setShowCreateModal(false);
            setFormData({ name: '', description: '' });
            loadProjects();
        } catch (error: any) {
            console.error('Failed to create project:', error);
            setError(error.message || 'Nie udało się utworzyć projektu');
        } finally {
            setCreating(false);
        }
    };

    const handleDeleteProject = async (projectId: number, projectName: string) => {
        if (!window.confirm(`Czy na pewno chcesz usunąć projekt "${projectName}"?\n\nTa operacja usunie wszystkie zadania i jest nieodwracalna!`)) {
            return;
        }

        try {
            await projectService.deleteProject(projectId);
            loadProjects();
        } catch (error: any) {
            console.error('Failed to delete project:', error);
            alert(error.message || 'Nie udało się usunąć projektu');
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
                        <p className="text-gray-400">Ładowanie projektów...</p>
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
                        <h1 className="text-3xl font-bold text-white mb-2">Projekty</h1>
                        <p className="text-gray-400">Zarządzaj projektami i zadaniami</p>
                    </div>
                    <button
                        onClick={() => setShowCreateModal(true)}
                        className="flex items-center gap-2 px-6 py-3 bg-emerald-500 hover:bg-emerald-600 text-white rounded-lg transition"
                    >
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                        </svg>
                        Nowy projekt
                    </button>
                </div>

                {/* Error message */}
                {error && !creating && (
                    <div className="mb-6 p-4 bg-red-500 bg-opacity-10 border border-red-500 rounded-lg text-red-400">
                        {error}
                    </div>
                )}

                {/* Projects Grid */}
                {projects.length === 0 ? (
                    <div className="text-center py-16">
                        <svg className="w-24 h-24 text-gray-600 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
                        </svg>
                        <p className="text-gray-400 text-lg mb-4">Nie masz jeszcze żadnych projektów</p>
                        <button
                            onClick={() => setShowCreateModal(true)}
                            className="px-6 py-3 bg-emerald-500 hover:bg-emerald-600 text-white rounded-lg transition"
                        >
                            Utwórz pierwszy projekt
                        </button>
                    </div>
                ) : (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                        {projects.map((project) => (
                            <div
                                key={project.id}
                                className="bg-gray-900 border border-gray-800 rounded-lg p-6 hover:border-emerald-500 transition-colors cursor-pointer"
                                onClick={() => navigate(`/projects/${project.id}`)}
                            >
                                <div className="flex items-start justify-between mb-4">
                                    <div className="flex-1">
                                        <h3 className="text-xl font-bold text-white mb-2">{project.name}</h3>
                                        {project.description && (
                                            <p className="text-gray-400 text-sm mb-4 line-clamp-2">{project.description}</p>
                                        )}
                                    </div>
                                    <div className="w-12 h-12 bg-emerald-500 bg-opacity-20 rounded-lg flex items-center justify-center flex-shrink-0">
                                        <svg className="w-6 h-6 text-emerald-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
                                        </svg>
                                    </div>
                                </div>

                                <div className="grid grid-cols-2 gap-4 mb-4">
                                    <div className="flex items-center gap-2 text-gray-400 text-sm">
                                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
                                        </svg>
                                        <span>{project.memberCount || project.members?.length || 0} członków</span>
                                    </div>
                                    <div className="flex items-center gap-2 text-gray-400 text-sm">
                                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                                        </svg>
                                        <span>{project.taskCount || 0} zadań</span>
                                    </div>
                                </div>

                                <div className="flex gap-2" onClick={(e) => e.stopPropagation()}>
                                    <button
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            navigate(`/projects/${project.id}`);
                                        }}
                                        className="flex-1 px-4 py-2 bg-gray-800 hover:bg-gray-700 text-white rounded-lg transition text-sm"
                                    >
                                        Zobacz szczegóły
                                    </button>
                                    <button
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            handleDeleteProject(project.id, project.name);
                                        }}
                                        className="px-4 py-2 bg-red-500 bg-opacity-10 hover:bg-red-500 hover:bg-opacity-20 text-red-400 rounded-lg transition text-sm"
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

                {/* Create Project Modal */}
                {showCreateModal && (
                    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                        <div className="bg-gray-900 rounded-lg p-6 w-full max-w-md border border-gray-800">
                            <h2 className="text-2xl font-bold text-white mb-4">Utwórz nowy projekt</h2>

                            {error && (
                                <div className="mb-4 p-3 bg-red-500 bg-opacity-10 border border-red-500 rounded text-red-400 text-sm">
                                    {error}
                                </div>
                            )}

                            <form onSubmit={handleCreateProject}>
                                <div className="mb-4">
                                    <label className="block text-gray-400 text-sm font-medium mb-2">
                                        Nazwa projektu *
                                    </label>
                                    <input
                                        type="text"
                                        value={formData.name}
                                        onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                                        className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-emerald-500"
                                        placeholder="Wpisz nazwę projektu..."
                                        maxLength={100}
                                        required
                                    />
                                </div>

                                <div className="mb-6">
                                    <label className="block text-gray-400 text-sm font-medium mb-2">
                                        Opis (opcjonalnie)
                                    </label>
                                    <textarea
                                        value={formData.description}
                                        onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                                        className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-emerald-500"
                                        placeholder="Krótki opis projektu..."
                                        rows={3}
                                        maxLength={500}
                                    />
                                </div>

                                <div className="flex gap-3">
                                    <button
                                        type="button"
                                        onClick={() => {
                                            setShowCreateModal(false);
                                            setFormData({ name: '', description: '' });
                                            setError(null);
                                        }}
                                        className="flex-1 px-4 py-2 bg-gray-800 hover:bg-gray-700 text-white rounded-lg transition"
                                        disabled={creating}
                                    >
                                        Anuluj
                                    </button>
                                    <button
                                        type="submit"
                                        className="flex-1 px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-lg transition disabled:opacity-50 disabled:cursor-not-allowed"
                                        disabled={creating}
                                    >
                                        {creating ? 'Tworzenie...' : 'Utwórz projekt'}
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

export default ProjectsPage;