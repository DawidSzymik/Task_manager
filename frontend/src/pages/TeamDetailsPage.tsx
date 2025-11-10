// src/pages/TeamDetailsPage.tsx - Z AVATARAMI UŻYTKOWNIKÓW ✅
import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import MainLayout from '../components/MainLayout';
import UserAvatar from '../components/UserAvatar'; // ✅ DODANY IMPORT
import teamService from '../services/teamService';
import userService from '../services/userService';
import type { Team, User } from '../types';

const TeamDetailsPage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const [team, setTeam] = useState<Team | null>(null);
    const [members, setMembers] = useState<User[]>([]);
    const [allUsers, setAllUsers] = useState<User[]>([]);
    const [loading, setLoading] = useState(true);
    const [showAddMemberModal, setShowAddMemberModal] = useState(false);
    const [showEditModal, setShowEditModal] = useState(false);
    const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
    const [editFormData, setEditFormData] = useState({ name: '', description: '' });
    const [error, setError] = useState<string | null>(null);
    const [actionInProgress, setActionInProgress] = useState(false);

    useEffect(() => {
        if (id) {
            loadTeamData();
        }
    }, [id]);

    const loadTeamData = async () => {
        try {
            setLoading(true);
            const teamId = parseInt(id!);

            const [teamData, membersData, usersData] = await Promise.all([
                teamService.getTeamById(teamId),
                teamService.getTeamMembers(teamId),
                userService.getAllUsers(undefined, true),
            ]);

            setTeam(teamData);
            setMembers(membersData);
            setAllUsers(usersData);
            setEditFormData({
                name: teamData.name,
                description: teamData.description || '',
            });
        } catch (error: any) {
            console.error('Failed to load team:', error);
            setError(error.message || 'Nie udało się załadować danych zespołu');
        } finally {
            setLoading(false);
        }
    };

    const handleAddMember = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!selectedUserId || !id) return;

        try {
            setActionInProgress(true);
            setError(null);
            await teamService.addMemberToTeam(parseInt(id), selectedUserId);
            setShowAddMemberModal(false);
            setSelectedUserId(null);
            await loadTeamData();
        } catch (error: any) {
            console.error('Failed to add member:', error);
            setError(error.message || 'Nie udało się dodać członka');
        } finally {
            setActionInProgress(false);
        }
    };

    const handleRemoveMember = async (userId: number, username: string) => {
        if (!window.confirm(`Czy na pewno chcesz usunąć ${username} z zespołu?`)) {
            return;
        }

        try {
            setActionInProgress(true);
            await teamService.removeMemberFromTeam(parseInt(id!), userId);
            await loadTeamData();
        } catch (error: any) {
            console.error('Failed to remove member:', error);
            alert(error.message || 'Nie udało się usunąć członka');
        } finally {
            setActionInProgress(false);
        }
    };

    const handleEditTeam = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!editFormData.name.trim() || !id) return;

        try {
            setActionInProgress(true);
            setError(null);
            await teamService.updateTeam(parseInt(id), editFormData);
            setShowEditModal(false);
            await loadTeamData();
        } catch (error: any) {
            console.error('Failed to update team:', error);
            setError(error.message || 'Nie udało się zaktualizować zespołu');
        } finally {
            setActionInProgress(false);
        }
    };

    const handleDeleteTeam = async () => {
        if (!team || !id) return;

        if (!window.confirm(`Czy na pewno chcesz usunąć zespół "${team.name}"?\n\nTa operacja jest nieodwracalna!`)) {
            return;
        }

        try {
            await teamService.deleteTeam(parseInt(id));
            navigate('/teams');
        } catch (error: any) {
            console.error('Failed to delete team:', error);
            alert(error.message || 'Nie udało się usunąć zespołu');
        }
    };

    if (loading) {
        return (
            <MainLayout>
                <div className="flex items-center justify-center h-96">
                    <div className="flex flex-col items-center">
                        <svg className="animate-spin h-12 w-12 text-primary-500 mb-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                        </svg>
                        <p className="text-gray-400">Ładowanie danych zespołu...</p>
                    </div>
                </div>
            </MainLayout>
        );
    }

    if (error && !team) {
        return (
            <MainLayout>
                <div className="text-center py-12">
                    <p className="text-red-500 text-lg mb-4">{error}</p>
                    <button
                        onClick={() => navigate('/teams')}
                        className="px-6 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700"
                    >
                        Powrót do listy zespołów
                    </button>
                </div>
            </MainLayout>
        );
    }

    if (!team) {
        return (
            <MainLayout>
                <div className="text-center py-12">
                    <p className="text-gray-400 text-lg">Zespół nie został znaleziony</p>
                </div>
            </MainLayout>
        );
    }

    const availableUsers = allUsers.filter(user =>
        !members.some(member => member.id === user.id)
    );

    return (
        <MainLayout>
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                {/* Header */}
                <div className="mb-8">
                    <div className="flex items-center justify-between mb-4">
                        <div>
                            <h1 className="text-3xl font-bold text-white mb-2">{team.name}</h1>
                            {team.description && (
                                <p className="text-gray-400">{team.description}</p>
                            )}
                        </div>
                        {team.canEdit && (
                            <div className="flex gap-3">
                                <button
                                    onClick={() => setShowEditModal(true)}
                                    className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                                >
                                    ✏️ Edytuj
                                </button>
                                {team.canDelete && (
                                    <button
                                        onClick={handleDeleteTeam}
                                        className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors"
                                    >
                                        🗑️ Usuń
                                    </button>
                                )}
                            </div>
                        )}
                    </div>

                    {/* Statystyki zespołu */}
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
                        {/* Członkowie */}
                        <div className="bg-gray-900 border border-gray-800 rounded-lg p-6">
                            <div className="flex items-center justify-between">
                                <div>
                                    <p className="text-gray-400 text-sm mb-1">Członków</p>
                                    <p className="text-3xl font-bold text-white">
                                        {team.memberCount || members.length || 0}
                                    </p>
                                </div>
                                <div className="text-4xl">👥</div>
                            </div>
                        </div>

                        {/* Projekty */}
                        <div className="bg-gray-900 border border-gray-800 rounded-lg p-6">
                            <div className="flex items-center justify-between">
                                <div>
                                    <p className="text-gray-400 text-sm mb-1">Aktywnych projektów</p>
                                    <p className="text-3xl font-bold text-primary-500">
                                        {team.projectCount || 0}
                                    </p>
                                </div>
                                <div className="text-4xl">📁</div>
                            </div>
                        </div>

                        {/* Zadania */}
                        <div className="bg-gray-900 border border-gray-800 rounded-lg p-6">
                            <div className="flex items-center justify-between">
                                <div>
                                    <p className="text-gray-400 text-sm mb-1">Wszystkich zadań</p>
                                    <p className="text-3xl font-bold text-blue-500">
                                        {team.taskCount || 0}
                                    </p>
                                </div>
                                <div className="text-4xl">✅</div>
                            </div>
                        </div>
                    </div>
                </div>

                {error && (
                    <div className="mb-6 bg-red-900 border border-red-700 text-red-200 px-4 py-3 rounded-lg">
                        {error}
                    </div>
                )}

                {/* Members Section - ✅ Z AVATARAMI */}
                <div className="bg-gray-900 border border-gray-800 rounded-lg p-6 mb-8">
                    <div className="flex items-center justify-between mb-6">
                        <h2 className="text-xl font-bold text-white">Członkowie zespołu</h2>
                        {team.canEdit && (
                            <button
                                onClick={() => setShowAddMemberModal(true)}
                                className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors"
                            >
                                + Dodaj członka
                            </button>
                        )}
                    </div>

                    {members.length > 0 ? (
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                            {members.map((member) => (
                                <div
                                    key={member.id}
                                    className="bg-gray-800 border border-gray-700 rounded-lg p-4 hover:border-primary-500 transition-colors"
                                >
                                    {/* ✅ ZMIENIONE - Dodano avatar i zmieniono layout */}
                                    <div className="flex items-center gap-3">
                                        {/* ✅ Avatar użytkownika */}
                                        <UserAvatar user={member} size="lg" />

                                        {/* Informacje o użytkowniku */}
                                        <div className="flex-1 min-w-0">
                                            <p className="font-semibold text-white truncate">{member.username}</p>
                                            {member.fullName && (
                                                <p className="text-sm text-gray-400 truncate">{member.fullName}</p>
                                            )}
                                            {member.email && (
                                                <p className="text-xs text-gray-500 truncate">{member.email}</p>
                                            )}
                                        </div>

                                        {/* Przycisk usuwania */}
                                        {team.canEdit && (
                                            <button
                                                onClick={() => handleRemoveMember(member.id, member.username)}
                                                className="text-red-500 hover:text-red-400 flex-shrink-0"
                                                disabled={actionInProgress}
                                                title="Usuń z zespołu"
                                            >
                                                🗑️
                                            </button>
                                        )}
                                    </div>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <div className="text-center py-8 text-gray-400">
                            Zespół nie ma jeszcze żadnych członków
                        </div>
                    )}
                </div>

                {/* Back button */}
                <div className="flex justify-center">
                    <button
                        onClick={() => navigate('/teams')}
                        className="px-6 py-2 bg-gray-800 text-white rounded-lg hover:bg-gray-700 transition-colors"
                    >
                        ← Powrót do listy zespołów
                    </button>
                </div>
            </div>

            {/* Add Member Modal */}
            {showAddMemberModal && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                    <div className="bg-gray-900 rounded-lg p-6 max-w-md w-full mx-4 border border-gray-800">
                        <h2 className="text-xl font-bold text-white mb-4">Dodaj członka do zespołu</h2>
                        <form onSubmit={handleAddMember}>
                            <div className="mb-4">
                                <label className="block text-gray-300 mb-2">Wybierz użytkownika</label>
                                <select
                                    value={selectedUserId || ''}
                                    onChange={(e) => setSelectedUserId(parseInt(e.target.value))}
                                    className="w-full bg-gray-800 border border-gray-700 text-white rounded-lg px-4 py-2 focus:outline-none focus:border-primary-500"
                                    required
                                >
                                    <option value="">-- Wybierz użytkownika --</option>
                                    {availableUsers.map((user) => (
                                        <option key={user.id} value={user.id}>
                                            {user.username} {user.fullName ? `(${user.fullName})` : ''}
                                        </option>
                                    ))}
                                </select>
                            </div>
                            <div className="flex gap-3">
                                <button
                                    type="submit"
                                    disabled={actionInProgress}
                                    className="flex-1 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                    {actionInProgress ? 'Dodawanie...' : 'Dodaj'}
                                </button>
                                <button
                                    type="button"
                                    onClick={() => {
                                        setShowAddMemberModal(false);
                                        setSelectedUserId(null);
                                    }}
                                    className="flex-1 px-4 py-2 bg-gray-800 text-white rounded-lg hover:bg-gray-700"
                                >
                                    Anuluj
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* Edit Team Modal */}
            {showEditModal && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                    <div className="bg-gray-900 rounded-lg p-6 max-w-md w-full mx-4 border border-gray-800">
                        <h2 className="text-xl font-bold text-white mb-4">Edytuj zespół</h2>
                        <form onSubmit={handleEditTeam}>
                            <div className="mb-4">
                                <label className="block text-gray-300 mb-2">Nazwa zespołu</label>
                                <input
                                    type="text"
                                    value={editFormData.name}
                                    onChange={(e) => setEditFormData({ ...editFormData, name: e.target.value })}
                                    className="w-full bg-gray-800 border border-gray-700 text-white rounded-lg px-4 py-2 focus:outline-none focus:border-primary-500"
                                    required
                                />
                            </div>
                            <div className="mb-4">
                                <label className="block text-gray-300 mb-2">Opis (opcjonalnie)</label>
                                <textarea
                                    value={editFormData.description}
                                    onChange={(e) => setEditFormData({ ...editFormData, description: e.target.value })}
                                    className="w-full bg-gray-800 border border-gray-700 text-white rounded-lg px-4 py-2 focus:outline-none focus:border-primary-500"
                                    rows={3}
                                />
                            </div>
                            <div className="flex gap-3">
                                <button
                                    type="submit"
                                    disabled={actionInProgress}
                                    className="flex-1 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                    {actionInProgress ? 'Zapisywanie...' : 'Zapisz'}
                                </button>
                                <button
                                    type="button"
                                    onClick={() => {
                                        setShowEditModal(false);
                                        setEditFormData({
                                            name: team?.name || '',
                                            description: team?.description || '',
                                        });
                                    }}
                                    className="flex-1 px-4 py-2 bg-gray-800 text-white rounded-lg hover:bg-gray-700"
                                >
                                    Anuluj
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </MainLayout>
    );
};

export default TeamDetailsPage;