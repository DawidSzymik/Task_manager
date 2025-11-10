// frontend/src/pages/admin/AdminProjectsPage.tsx
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import MainLayout from '../../components/MainLayout';
import adminService, {type ProjectDto } from '../../services/adminService';
import { useAuth } from '../../context/AuthContext';

const AdminProjectsPage: React.FC = () => {
    const navigate = useNavigate();
    const { user } = useAuth();
    const [projects, setProjects] = useState<ProjectDto[]>([]);
    const [filteredProjects, setFilteredProjects] = useState<ProjectDto[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);

    const [searchTerm, setSearchTerm] = useState('');

    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [selectedProject, setSelectedProject] = useState<ProjectDto | null>(null);

    useEffect(() => {
        if (user?.systemRole !== 'SUPER_ADMIN') {
            navigate('/dashboard');
            return;
        }
        loadProjects();
    }, [user, navigate]);

    useEffect(() => {
        filterProjects();
    }, [projects, searchTerm]);

    const loadProjects = async () => {
        try {
            setLoading(true);
            setError(null);
            const data = await adminService.getAllProjects();
            setProjects(data);
        } catch (err: any) {
            setError(err.message || 'Nie udało się załadować projektów');
        } finally {
            setLoading(false);
        }
    };

    const filterProjects = () => {
        let filtered = [...projects];

        if (searchTerm) {
            const term = searchTerm.toLowerCase();
            filtered = filtered.filter(
                (p) =>
                    p.name.toLowerCase().includes(term) ||
                    p.description?.toLowerCase().includes(term) ||
                    p.createdBy?.username.toLowerCase().includes(term)
            );
        }

        setFilteredProjects(filtered);
    };

    const handleDeleteProject = async () => {
        if (!selectedProject) return;

        try {
            setError(null);
            await adminService.deleteProject(selectedProject.id);
            setSuccess('Projekt usunięty pomyślnie!');
            setShowDeleteConfirm(false);
            setSelectedProject(null);
            loadProjects();
            setTimeout(() => setSuccess(null), 3000);
        } catch (err: any) {
            setError(err.message || 'Nie udało się usunąć projektu');
        }
    };

    const openDeleteConfirm = (project: ProjectDto) => {
        setSelectedProject(project);
        setShowDeleteConfirm(true);
    };

    const formatDate = (dateString?: string) => {
        if (!dateString) return 'Brak';
        return new Date(dateString).toLocaleDateString('pl-PL', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
        });
    };

    if (loading) {
        return (
            <MainLayout>
                <div className="flex items-center justify-center h-64">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
                </div>
            </MainLayout>
        );
    }

    return (
        <MainLayout>
            <div className="space-y-6">
                {/* Header */}
                <div className="flex justify-between items-center">
                    <div>
                        <h1 className="text-3xl font-bold text-white">📁 Zarządzanie Projektami</h1>
                        <p className="text-gray-400 mt-1">Przeglądaj i zarządzaj wszystkimi projektami</p>
                    </div>
                </div>

                {/* Alerts */}
                {error && (
                    <div className="bg-red-900/50 border border-red-500 text-red-300 px-4 py-3 rounded-lg">
                        {error}
                    </div>
                )}
                {success && (
                    <div className="bg-green-900/50 border border-green-500 text-green-300 px-4 py-3 rounded-lg">
                        {success}
                    </div>
                )}

                {/* Filters */}
                <div className="bg-gray-900 border border-gray-700 rounded-lg shadow p-4">
                    <input
                        type="text"
                        placeholder="Szukaj po nazwie projektu, opisie lub twórcy..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white placeholder-gray-400 focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                    />
                </div>

                {/* Projects Table */}
                <div className="bg-gray-900 border border-gray-700 rounded-lg shadow overflow-hidden">
                    <table className="min-w-full divide-y divide-gray-700">
                        <thead className="bg-gray-800">
                        <tr>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">
                                Nazwa Projektu
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">
                                Opis
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">
                                Twórca
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">
                                Członkowie
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">
                                Zadania
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">
                                Utworzono
                            </th>
                            <th className="px-6 py-3 text-right text-xs font-medium text-gray-400 uppercase tracking-wider">
                                Akcje
                            </th>
                        </tr>
                        </thead>
                        <tbody className="bg-gray-900 divide-y divide-gray-800">
                        {filteredProjects.length === 0 ? (
                            <tr>
                                <td colSpan={7} className="px-6 py-8 text-center text-gray-500">
                                    Nie znaleziono projektów
                                </td>
                            </tr>
                        ) : (
                            filteredProjects.map((project) => (
                                <tr key={project.id} className="hover:bg-gray-800 transition-colors">
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <div
                                            onClick={() => navigate(`/projects/${project.id}`)}
                                            className="text-sm font-medium text-primary-400 hover:text-primary-300 cursor-pointer"
                                        >
                                            {project.name}
                                        </div>
                                    </td>
                                    <td className="px-6 py-4">
                                        <div className="text-sm text-gray-300 max-w-xs truncate">
                                            {project.description || 'Brak opisu'}
                                        </div>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <div className="text-sm text-gray-300">
                                            {project.createdBy?.username || 'Nieznany'}
                                        </div>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <span className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-blue-900/50 text-blue-300 border border-blue-600">
                                            {project.memberCount || 0}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <span className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-primary-900/50 text-primary-300 border border-primary-600">
                                            {project.taskCount || 0}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-300">
                                        {formatDate(project.createdAt)}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                                        <div className="flex justify-end gap-2">
                                            <button
                                                onClick={() => navigate(`/projects/${project.id}`)}
                                                className="px-3 py-1 bg-primary-900/50 hover:bg-primary-800 text-primary-300 rounded border border-primary-600 transition-colors"
                                            >
                                                Wyświetl
                                            </button>
                                            <button
                                                onClick={() => openDeleteConfirm(project)}
                                                className="px-3 py-1 bg-red-900/50 hover:bg-red-800 text-red-300 rounded border border-red-600 transition-colors"
                                            >
                                                Usuń
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))
                        )}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* Delete Confirmation Modal */}
            {showDeleteConfirm && selectedProject && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                    <div className="bg-gray-900 border border-gray-700 rounded-lg p-6 max-w-md w-full">
                        <h2 className="text-xl font-bold text-red-400 mb-4">Potwierdź Usunięcie</h2>
                        <p className="text-gray-300 mb-6">
                            Czy na pewno chcesz usunąć projekt <strong>{selectedProject.name}</strong>?
                            <br /><br />
                            <span className="text-red-400 font-semibold">⚠️ To usunie również wszystkie powiązane zadania, komentarze i pliki!</span>
                            <br /><br />
                            Ta akcja nie może być cofnięta.
                        </p>
                        <div className="flex gap-3">
                            <button
                                onClick={() => {
                                    setShowDeleteConfirm(false);
                                    setSelectedProject(null);
                                }}
                                className="flex-1 px-4 py-2 border border-gray-600 text-gray-300 rounded-lg hover:bg-gray-800 transition-colors"
                            >
                                Anuluj
                            </button>
                            <button
                                onClick={handleDeleteProject}
                                className="flex-1 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors"
                            >
                                Usuń
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </MainLayout>
    );
};

export default AdminProjectsPage;