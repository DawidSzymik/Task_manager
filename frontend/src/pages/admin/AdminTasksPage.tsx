// frontend/src/pages/admin/AdminTasksPage.tsx
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import MainLayout from '../../components/MainLayout';
import adminService, {type TaskDto } from '../../services/adminService';
import { useAuth } from '../../context/AuthContext';

const AdminTasksPage: React.FC = () => {
    const navigate = useNavigate();
    const { user } = useAuth();
    const [tasks, setTasks] = useState<TaskDto[]>([]);
    const [filteredTasks, setFilteredTasks] = useState<TaskDto[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);

    const [searchTerm, setSearchTerm] = useState('');
    const [filterStatus, setFilterStatus] = useState<string>('all');
    const [filterPriority, setFilterPriority] = useState<string>('all');

    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [selectedTask, setSelectedTask] = useState<TaskDto | null>(null);

    useEffect(() => {
        if (user?.systemRole !== 'SUPER_ADMIN') {
            navigate('/dashboard');
            return;
        }
        loadTasks();
    }, [user, navigate]);

    useEffect(() => {
        filterTasks();
    }, [tasks, searchTerm, filterStatus, filterPriority]);

    const loadTasks = async () => {
        try {
            setLoading(true);
            setError(null);
            const data = await adminService.getAllTasks();
            setTasks(data);
        } catch (err: any) {
            setError(err.message || 'Nie udało się załadować zadań');
        } finally {
            setLoading(false);
        }
    };

    const filterTasks = () => {
        let filtered = [...tasks];

        if (searchTerm) {
            const term = searchTerm.toLowerCase();
            filtered = filtered.filter(
                (t) =>
                    t.title.toLowerCase().includes(term) ||
                    t.description?.toLowerCase().includes(term) ||
                    t.project?.name.toLowerCase().includes(term) ||
                    t.assignedTo?.username.toLowerCase().includes(term)
            );
        }

        if (filterStatus !== 'all') {
            filtered = filtered.filter((t) => t.status === filterStatus);
        }

        if (filterPriority !== 'all') {
            filtered = filtered.filter((t) => t.priority === filterPriority);
        }

        setFilteredTasks(filtered);
    };

    const handleDeleteTask = async () => {
        if (!selectedTask) return;

        try {
            setError(null);
            await adminService.deleteTask(selectedTask.id);
            setSuccess('Zadanie usunięte pomyślnie!');
            setShowDeleteConfirm(false);
            setSelectedTask(null);
            loadTasks();
            setTimeout(() => setSuccess(null), 3000);
        } catch (err: any) {
            setError(err.message || 'Nie udało się usunąć zadania');
        }
    };

    const openDeleteConfirm = (task: TaskDto) => {
        setSelectedTask(task);
        setShowDeleteConfirm(true);
    };

    const getStatusBadgeColor = (status: string) => {
        const colors: Record<string, string> = {
            NEW: 'bg-blue-900/50 text-blue-300 border-blue-600',
            IN_PROGRESS: 'bg-yellow-900/50 text-yellow-300 border-yellow-600',
            COMPLETED: 'bg-green-900/50 text-green-300 border-green-600',
            CANCELLED: 'bg-red-900/50 text-red-300 border-red-600',
        };
        return colors[status] || 'bg-gray-900/50 text-gray-300 border-gray-600';
    };

    const getPriorityBadgeColor = (priority: string) => {
        const colors: Record<string, string> = {
            LOW: 'bg-blue-900/50 text-blue-300 border-blue-600',
            MEDIUM: 'bg-yellow-900/50 text-yellow-300 border-yellow-600',
            HIGH: 'bg-orange-900/50 text-orange-300 border-orange-600',
            URGENT: 'bg-red-900/50 text-red-300 border-red-600',
        };
        return colors[priority] || 'bg-gray-900/50 text-gray-300 border-gray-600';
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
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-emerald-600"></div>
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
                        <h1 className="text-3xl font-bold text-white">📋 Zarządzanie Zadaniami</h1>
                        <p className="text-gray-400 mt-1">Przeglądaj i zarządzaj wszystkimi zadaniami</p>
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
                    <div className="space-y-4">
                        <input
                            type="text"
                            placeholder="Szukaj po tytule zadania, opisie, projekcie lub osobie przypisanej..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white placeholder-gray-400 focus:ring-2 focus:ring-emerald-500 focus:border-transparent"
                        />
                        <div className="flex gap-4">
                            <div className="flex-1">
                                <label className="block text-sm font-medium text-gray-400 mb-1">Status</label>
                                <select
                                    value={filterStatus}
                                    onChange={(e) => setFilterStatus(e.target.value)}
                                    className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:ring-2 focus:ring-emerald-500 focus:border-transparent"
                                >
                                    <option value="all">Wszystkie Statusy</option>
                                    <option value="NEW">Nowe</option>
                                    <option value="IN_PROGRESS">W trakcie</option>
                                    <option value="COMPLETED">Ukończone</option>
                                    <option value="CANCELLED">Anulowane</option>
                                </select>
                            </div>
                            <div className="flex-1">
                                <label className="block text-sm font-medium text-gray-400 mb-1">Priorytet</label>
                                <select
                                    value={filterPriority}
                                    onChange={(e) => setFilterPriority(e.target.value)}
                                    className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:ring-2 focus:ring-emerald-500 focus:border-transparent"
                                >
                                    <option value="all">Wszystkie Priorytety</option>
                                    <option value="LOW">Niski</option>
                                    <option value="MEDIUM">Średni</option>
                                    <option value="HIGH">Wysoki</option>
                                    <option value="URGENT">Pilne</option>
                                </select>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Tasks Table */}
                <div className="bg-gray-900 border border-gray-700 rounded-lg shadow overflow-hidden">
                    <table className="min-w-full divide-y divide-gray-700">
                        <thead className="bg-gray-800">
                        <tr>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">
                                Tytuł Zadania
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">
                                Projekt
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">
                                Status
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">
                                Priorytet
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">
                                Przypisane do
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">
                                Termin
                            </th>
                            <th className="px-6 py-3 text-right text-xs font-medium text-gray-400 uppercase tracking-wider">
                                Akcje
                            </th>
                        </tr>
                        </thead>
                        <tbody className="bg-gray-900 divide-y divide-gray-800">
                        {filteredTasks.length === 0 ? (
                            <tr>
                                <td colSpan={7} className="px-6 py-8 text-center text-gray-500">
                                    Nie znaleziono zadań
                                </td>
                            </tr>
                        ) : (
                            filteredTasks.map((task) => (
                                <tr key={task.id} className="hover:bg-gray-800 transition-colors">
                                    <td className="px-6 py-4">
                                        <div
                                            onClick={() => navigate(`/tasks/${task.id}`)}
                                            className="text-sm font-medium text-emerald-400 hover:text-emerald-300 cursor-pointer max-w-xs truncate"
                                        >
                                            {task.title}
                                        </div>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <div className="text-sm text-gray-300">
                                            {task.project?.name || 'Brak projektu'}
                                        </div>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full border ${getStatusBadgeColor(task.status)}`}>
                                            {task.status}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full border ${getPriorityBadgeColor(task.priority)}`}>
                                            {task.priority}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <div className="text-sm text-gray-300">
                                            {task.assignedTo?.username || 'Nieprzypisane'}
                                        </div>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-300">
                                        {formatDate(task.deadline)}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                                        <div className="flex justify-end gap-2">
                                            <button
                                                onClick={() => navigate(`/tasks/${task.id}`)}
                                                className="px-3 py-1 bg-emerald-900/50 hover:bg-emerald-800 text-emerald-300 rounded border border-emerald-600 transition-colors"
                                            >
                                                Wyświetl
                                            </button>
                                            <button
                                                onClick={() => openDeleteConfirm(task)}
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
            {showDeleteConfirm && selectedTask && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                    <div className="bg-gray-900 border border-gray-700 rounded-lg p-6 max-w-md w-full">
                        <h2 className="text-xl font-bold text-red-400 mb-4">Potwierdź Usunięcie</h2>
                        <p className="text-gray-300 mb-6">
                            Czy na pewno chcesz usunąć zadanie <strong>{selectedTask.title}</strong>?
                            <br /><br />
                            <span className="text-red-400 font-semibold">⚠️ To usunie również wszystkie powiązane komentarze i pliki!</span>
                            <br /><br />
                            Ta akcja nie może być cofnięta.
                        </p>
                        <div className="flex gap-3">
                            <button
                                onClick={() => {
                                    setShowDeleteConfirm(false);
                                    setSelectedTask(null);
                                }}
                                className="flex-1 px-4 py-2 border border-gray-600 text-gray-300 rounded-lg hover:bg-gray-800 transition-colors"
                            >
                                Anuluj
                            </button>
                            <button
                                onClick={handleDeleteTask}
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

export default AdminTasksPage;