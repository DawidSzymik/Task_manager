// frontend/src/pages/admin/AdminUsersPage.tsx
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import MainLayout from '../../components/MainLayout';
import adminService, {type UserDto, type CreateUserRequest, type UpdateUserRequest } from '../../services/adminService';
import { useAuth } from '../../context/AuthContext';

const AdminUsersPage: React.FC = () => {
    const navigate = useNavigate();
    const { user } = useAuth();
    const [users, setUsers] = useState<UserDto[]>([]);
    const [filteredUsers, setFilteredUsers] = useState<UserDto[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);

    const [searchTerm, setSearchTerm] = useState('');
    const [filterActive, setFilterActive] = useState<boolean | undefined>(undefined);

    const [showCreateModal, setShowCreateModal] = useState(false);
    const [showEditModal, setShowEditModal] = useState(false);
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [selectedUser, setSelectedUser] = useState<UserDto | null>(null);

    const [createForm, setCreateForm] = useState<CreateUserRequest>({
        username: '',
        password: '',
        email: '',
        fullName: '',
        systemRole: 'USER',
    });

    const [editForm, setEditForm] = useState<UpdateUserRequest>({
        email: '',
        fullName: '',
        systemRole: 'USER',
        active: true,
    });

    useEffect(() => {
        if (user?.systemRole !== 'SUPER_ADMIN') {
            navigate('/dashboard');
            return;
        }
        loadUsers();
    }, [user, navigate]);

    useEffect(() => {
        filterUsers();
    }, [users, searchTerm, filterActive]);

    const loadUsers = async () => {
        try {
            setLoading(true);
            setError(null);
            const data = await adminService.getAllUsers();
            setUsers(data);
        } catch (err: any) {
            setError(err.message || 'Nie udało się załadować użytkowników');
        } finally {
            setLoading(false);
        }
    };

    const filterUsers = () => {
        let filtered = [...users];

        if (searchTerm) {
            const term = searchTerm.toLowerCase();
            filtered = filtered.filter(
                (u) =>
                    u.username.toLowerCase().includes(term) ||
                    u.email?.toLowerCase().includes(term) ||
                    u.fullName?.toLowerCase().includes(term)
            );
        }

        if (filterActive !== undefined) {
            filtered = filtered.filter((u) => u.active === filterActive);
        }

        setFilteredUsers(filtered);
    };

    const handleCreateUser = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            setError(null);
            await adminService.createUser(createForm);
            setSuccess('Użytkownik utworzony pomyślnie!');
            setShowCreateModal(false);
            setCreateForm({
                username: '',
                password: '',
                email: '',
                fullName: '',
                systemRole: 'USER',
            });
            loadUsers();
            setTimeout(() => setSuccess(null), 3000);
        } catch (err: any) {
            setError(err.message || 'Nie udało się utworzyć użytkownika');
        }
    };

    const handleUpdateUser = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!selectedUser) return;

        try {
            setError(null);
            await adminService.updateUser(selectedUser.id, editForm);
            setSuccess('Użytkownik zaktualizowany pomyślnie!');
            setShowEditModal(false);
            setSelectedUser(null);
            loadUsers();
            setTimeout(() => setSuccess(null), 3000);
        } catch (err: any) {
            setError(err.message || 'Nie udało się zaktualizować użytkownika');
        }
    };

    const handleDeleteUser = async () => {
        if (!selectedUser) return;

        try {
            setError(null);
            await adminService.deleteUser(selectedUser.id);
            setSuccess('Użytkownik usunięty pomyślnie!');
            setShowDeleteConfirm(false);
            setSelectedUser(null);
            loadUsers();
            setTimeout(() => setSuccess(null), 3000);
        } catch (err: any) {
            setError(err.message || 'Nie udało się usunąć użytkownika');
        }
    };

    const openEditModal = (user: UserDto) => {
        setSelectedUser(user);
        setEditForm({
            email: user.email || '',
            fullName: user.fullName || '',
            systemRole: user.systemRole,
            active: user.active,
        });
        setShowEditModal(true);
    };

    const openDeleteConfirm = (user: UserDto) => {
        setSelectedUser(user);
        setShowDeleteConfirm(true);
    };

    const getRoleBadgeColor = (role?: string) => {
        return role === 'SUPER_ADMIN'
            ? 'bg-purple-900/50 text-purple-300 border border-purple-600'
            : 'bg-blue-900/50 text-blue-300 border border-blue-600';
    };

    const getStatusBadgeColor = (active?: boolean) => {
        return active
            ? 'bg-green-900/50 text-green-300 border border-green-600'
            : 'bg-red-900/50 text-red-300 border border-red-600';
    };

    const formatDate = (dateString?: string) => {
        if (!dateString) return 'Nigdy';
        return new Date(dateString).toLocaleDateString('pl-PL', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
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
                        <h1 className="text-3xl font-bold text-white">👥 Zarządzanie Użytkownikami</h1>
                        <p className="text-gray-400 mt-1">Zarządzaj użytkownikami i uprawnieniami systemu</p>
                    </div>
                    <button
                        onClick={() => setShowCreateModal(true)}
                        className="px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-lg font-medium transition-colors flex items-center gap-2"
                    >
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                        </svg>
                        Utwórz Użytkownika
                    </button>
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
                            placeholder="Szukaj po nazwie użytkownika, emailu lub imieniu i nazwisku..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white placeholder-gray-400 focus:ring-2 focus:ring-emerald-500 focus:border-transparent"
                        />
                        <div className="flex gap-2">
                            <button
                                onClick={() => setFilterActive(undefined)}
                                className={`px-4 py-2 rounded-lg font-medium transition-colors ${
                                    filterActive === undefined
                                        ? 'bg-emerald-500 text-white'
                                        : 'bg-gray-800 text-gray-300 hover:bg-gray-700'
                                }`}
                            >
                                Wszystkie
                            </button>
                            <button
                                onClick={() => setFilterActive(true)}
                                className={`px-4 py-2 rounded-lg font-medium transition-colors ${
                                    filterActive === true
                                        ? 'bg-green-500 text-white'
                                        : 'bg-gray-800 text-gray-300 hover:bg-gray-700'
                                }`}
                            >
                                Aktywni
                            </button>
                            <button
                                onClick={() => setFilterActive(false)}
                                className={`px-4 py-2 rounded-lg font-medium transition-colors ${
                                    filterActive === false
                                        ? 'bg-gray-500 text-white'
                                        : 'bg-gray-800 text-gray-300 hover:bg-gray-700'
                                }`}
                            >
                                Nieaktywni
                            </button>
                        </div>
                    </div>
                </div>

                {/* Users Table */}
                <div className="bg-gray-900 border border-gray-700 rounded-lg shadow overflow-hidden">
                    <table className="min-w-full divide-y divide-gray-700">
                        <thead className="bg-gray-800">
                        <tr>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">
                                Użytkownik
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">
                                Email
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">
                                Rola
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">
                                Status
                            </th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">
                                Ostatnie Logowanie
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
                        {filteredUsers.length === 0 ? (
                            <tr>
                                <td colSpan={7} className="px-6 py-8 text-center text-gray-500">
                                    Nie znaleziono użytkowników
                                </td>
                            </tr>
                        ) : (
                            filteredUsers.map((userItem) => (
                                <tr key={userItem.id} className="hover:bg-gray-800 transition-colors">
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <div className="flex items-center">
                                            <div className="flex-shrink-0 h-10 w-10 bg-emerald-600 rounded-full flex items-center justify-center text-white font-semibold">
                                                {userItem.username.charAt(0).toUpperCase()}
                                            </div>
                                            <div className="ml-4">
                                                <div className="text-sm font-medium text-white">
                                                    {userItem.username}
                                                </div>
                                                <div className="text-sm text-gray-400">
                                                    {userItem.fullName || 'Brak imienia'}
                                                </div>
                                            </div>
                                        </div>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <div className="text-sm text-gray-300">{userItem.email || 'Brak'}</div>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${getRoleBadgeColor(userItem.systemRole)}`}>
                                            {userItem.systemRole}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${getStatusBadgeColor(userItem.active)}`}>
                                            {userItem.active ? 'Aktywny' : 'Nieaktywny'}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-300">
                                        {formatDate(userItem.lastLogin)}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-300">
                                        {formatDate(userItem.createdAt)}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                                        <div className="flex justify-end gap-2">
                                            <button
                                                onClick={() => openEditModal(userItem)}
                                                className="px-3 py-1 bg-blue-900/50 hover:bg-blue-800 text-blue-300 rounded border border-blue-600 transition-colors"
                                            >
                                                Edytuj
                                            </button>
                                            <button
                                                onClick={() => openDeleteConfirm(userItem)}
                                                disabled={userItem.id === user?.id}
                                                className="px-3 py-1 bg-red-900/50 hover:bg-red-800 text-red-300 rounded border border-red-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
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

            {/* Create User Modal */}
            {showCreateModal && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                    <div className="bg-gray-900 border border-gray-700 rounded-lg p-6 max-w-md w-full">
                        <h2 className="text-xl font-bold text-white mb-4">Utwórz Nowego Użytkownika</h2>
                        <form onSubmit={handleCreateUser} className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-300 mb-1">Nazwa użytkownika *</label>
                                <input
                                    type="text"
                                    required
                                    value={createForm.username}
                                    onChange={(e) => setCreateForm({ ...createForm, username: e.target.value })}
                                    className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:ring-2 focus:ring-emerald-500 focus:border-transparent"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-300 mb-1">Hasło *</label>
                                <input
                                    type="password"
                                    required
                                    value={createForm.password}
                                    onChange={(e) => setCreateForm({ ...createForm, password: e.target.value })}
                                    className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:ring-2 focus:ring-emerald-500 focus:border-transparent"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-300 mb-1">Email</label>
                                <input
                                    type="email"
                                    value={createForm.email}
                                    onChange={(e) => setCreateForm({ ...createForm, email: e.target.value })}
                                    className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:ring-2 focus:ring-emerald-500 focus:border-transparent"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-300 mb-1">Imię i nazwisko</label>
                                <input
                                    type="text"
                                    value={createForm.fullName}
                                    onChange={(e) => setCreateForm({ ...createForm, fullName: e.target.value })}
                                    className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:ring-2 focus:ring-emerald-500 focus:border-transparent"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-300 mb-1">Rola systemowa</label>
                                <select
                                    value={createForm.systemRole}
                                    onChange={(e) => setCreateForm({ ...createForm, systemRole: e.target.value as 'SUPER_ADMIN' | 'USER' })}
                                    className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:ring-2 focus:ring-emerald-500 focus:border-transparent"
                                >
                                    <option value="USER">USER</option>
                                    <option value="SUPER_ADMIN">SUPER_ADMIN</option>
                                </select>
                            </div>
                            <div className="flex gap-3">
                                <button
                                    type="button"
                                    onClick={() => {
                                        setShowCreateModal(false);
                                        setCreateForm({
                                            username: '',
                                            password: '',
                                            email: '',
                                            fullName: '',
                                            systemRole: 'USER',
                                        });
                                    }}
                                    className="flex-1 px-4 py-2 border border-gray-600 text-gray-300 rounded-lg hover:bg-gray-800 transition-colors"
                                >
                                    Anuluj
                                </button>
                                <button
                                    type="submit"
                                    className="flex-1 px-4 py-2 bg-emerald-500 text-white rounded-lg hover:bg-emerald-600 transition-colors"
                                >
                                    Utwórz
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* Edit User Modal */}
            {showEditModal && selectedUser && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                    <div className="bg-gray-900 border border-gray-700 rounded-lg p-6 max-w-md w-full">
                        <h2 className="text-xl font-bold text-white mb-4">Edytuj Użytkownika: {selectedUser.username}</h2>
                        <form onSubmit={handleUpdateUser} className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-300 mb-1">Email</label>
                                <input
                                    type="email"
                                    value={editForm.email}
                                    onChange={(e) => setEditForm({ ...editForm, email: e.target.value })}
                                    className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:ring-2 focus:ring-emerald-500 focus:border-transparent"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-300 mb-1">Imię i nazwisko</label>
                                <input
                                    type="text"
                                    value={editForm.fullName}
                                    onChange={(e) => setEditForm({ ...editForm, fullName: e.target.value })}
                                    className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:ring-2 focus:ring-emerald-500 focus:border-transparent"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-300 mb-1">Rola systemowa</label>
                                <select
                                    value={editForm.systemRole}
                                    onChange={(e) => setEditForm({ ...editForm, systemRole: e.target.value as 'SUPER_ADMIN' | 'USER' })}
                                    className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:ring-2 focus:ring-emerald-500 focus:border-transparent"
                                >
                                    <option value="USER">USER</option>
                                    <option value="SUPER_ADMIN">SUPER_ADMIN</option>
                                </select>
                            </div>
                            <div className="flex items-center">
                                <input
                                    type="checkbox"
                                    checked={editForm.active}
                                    onChange={(e) => setEditForm({ ...editForm, active: e.target.checked })}
                                    className="h-4 w-4 text-emerald-600 focus:ring-emerald-500 border-gray-600 rounded bg-gray-800"
                                />
                                <label className="ml-2 block text-sm text-gray-300">Aktywny</label>
                            </div>
                            <div className="flex gap-3">
                                <button
                                    type="button"
                                    onClick={() => {
                                        setShowEditModal(false);
                                        setSelectedUser(null);
                                    }}
                                    className="flex-1 px-4 py-2 border border-gray-600 text-gray-300 rounded-lg hover:bg-gray-800 transition-colors"
                                >
                                    Anuluj
                                </button>
                                <button
                                    type="submit"
                                    className="flex-1 px-4 py-2 bg-emerald-500 text-white rounded-lg hover:bg-emerald-600 transition-colors"
                                >
                                    Zaktualizuj
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* Delete Confirmation Modal */}
            {showDeleteConfirm && selectedUser && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                    <div className="bg-gray-900 border border-gray-700 rounded-lg p-6 max-w-md w-full">
                        <h2 className="text-xl font-bold text-red-400 mb-4">Potwierdź Usunięcie</h2>
                        <p className="text-gray-300 mb-6">
                            Czy na pewno chcesz usunąć użytkownika <strong>{selectedUser.username}</strong>?
                            Ta akcja nie może być cofnięta.
                        </p>
                        <div className="flex gap-3">
                            <button
                                onClick={() => {
                                    setShowDeleteConfirm(false);
                                    setSelectedUser(null);
                                }}
                                className="flex-1 px-4 py-2 border border-gray-600 text-gray-300 rounded-lg hover:bg-gray-800 transition-colors"
                            >
                                Anuluj
                            </button>
                            <button
                                onClick={handleDeleteUser}
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

export default AdminUsersPage;