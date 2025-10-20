// src/pages/TeamDetailsPage.tsx
import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import MainLayout from '../components/MainLayout';
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
                userService.getAllUsers(undefined, true), // Get only active users
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
                        <svg className="animate-spin h-12 w-12 text-emerald-500 mb-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                        </svg>
                        <p className="text-gray-400">Ładowanie danych zespołu...</p>
                    </div>
                </div>
            </MainLayout>
        );
    }

    if (!team) {
        return (
            <MainLayout>
                <div className="text-center py-16">
                    <p className="text-gray-400 text-lg mb-4">Zespół nie został znaleziony</p>
                    <button
                        onClick={() => navigate('/teams')}
                        className="px-6 py-3 bg-emerald-500 hover:bg-emerald-600 text-white rounded-lg transition"
                    >
                        Powrót do listy zespołów
                    </button>
                </div>
            </MainLayout>
        );
    }

    const availableUsers = allUsers.filter(
        user => !members.some(member => member.id === user.id)
    );

    return (
        <MainLayout>
            <div className="max-w-7xl mx-auto">
                {/* Header */}
                <div className="mb-8">
                    <button
                        onClick={() => navigate('/teams')}
                        className="flex items-center gap-2 text-gray-400 hover:text-white transition mb-4"
                    >
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
                        </svg>
                        Powrót do listy zespołów
                    </button>

                    <div className="flex items-start justify-between">
                        <div>
                            <h1 className="text-3xl font-bold text-white mb-2">{team.name}</h1>
                            {team.description && (
                                <p className="text-gray-400">{team.description}</p>
                            )}
                        </div>
                        <div className="flex gap-2">
                            {team.canEdit && (
                                <button
                                    onClick={() => setShowEditModal(true)}
                                    className="px-4 py-2 bg-gray-800 hover:bg-gray-700 text-white rounded-lg transition flex items-center gap-2"
                                >
                                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                                    </svg>
                                    Edytuj
                                </button>
                            )}
                            {team.canDelete && (
                                <button
                                    onClick={handleDeleteTeam}
                                    className="px-4 py-2 bg-red-500 bg-opacity-10 hover:bg-red-500 hover:bg-opacity-20 text-red-400 rounded-lg transition flex items-center gap-2"
                                >
                                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                    </svg>
                                    Usuń zespół
                                </button>
                            )}
                        </div>
                    </div>
                </div>

                {/* Stats */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
                    <div className="bg-gray-900 border border-gray-800 rounded-lg p-6">
                        <div className="flex items-center gap-3 mb-2">
                            <div className="w-10 h-10 bg-purple-500 bg-opacity-20 rounded-lg flex items-center justify-center">
                                <svg className="w-5 h-5 text-purple-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
                                </svg>
                            </div>
                            <div>
                                <p className="text-2xl font-bold text-white">{members.length}</p>
                                <p className="text-gray-400 text-sm">Członków</p>
                            </div>
                        </div>
                    </div>

                    <div className="bg-gray-900 border border-gray-800 rounded-lg p-6">
                        <div className="flex items-center gap-3 mb-2">
                            <div className="w-10 h-10 bg-blue-500 bg-opacity-20 rounded-lg flex items-center justify-center">
                                <svg className="w-5 h-5 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
                                </svg>
                            </div>
                            <div>
                                <p className="text-2xl font-bold text-white">0</p>
                                <p className="text-gray-400 text-sm">Aktywnych projektów</p>
                            </div>
                        </div>
                    </div>

                    <div className="bg-gray-900 border border-gray-800 rounded-lg p-6">
                        <div className="flex items-center gap-3 mb-2">
                            <div className="w-10 h-10 bg-green-500 bg-opacity-20 rounded-lg flex items-center justify-center">
                                <svg className="w-5 h-5 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                                </svg>
                            </div>
                            <div>
                                <p className="text-2xl font-bold text-white">0</p>
                                <p className="text-gray-400 text-sm">Ukończonych zadań</p>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Members Section */}
                <div className="bg-gray-900 border border-gray-800 rounded-lg p-6">
                    <div className="flex items-center justify-between mb-6">
                        <h2 className="text-xl font-bold text-white">Członkowie zespołu</h2>
                        {team.canEdit && (
                            <button
                                onClick={() => setShowAddMemberModal(true)}
                                className="flex items-center gap-2 px-4 py-2 bg-purple-500 hover:bg-purple-600 text-white rounded-lg transition text-sm"
                            >
                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                                </svg>
                                Dodaj członka
                            </button>
                        )}
                    </div>

                    {members.length === 0 ? (
                        <div className="text-center py-8">
                            <svg className="w-16 h-16 text-gray-600 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                            </svg>
                            <p className="text-gray-400">Zespół nie ma jeszcze żadnych członków</p>
                        </div>
                    ) : (
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                            {members.map((member) => (
                                <div
                                    key={member.id}
                                    className="flex items-center justify-between p-4 bg-gray-800 rounded-lg hover:bg-gray-750 transition"
                                >
                                    <div className="flex items-center gap-3">
                                        <div className="w-10 h-10 bg-purple-500 rounded-full flex items-center justify-center">
                                            <span className="text-white font-semibold">
                                                {member.username.charAt(0).toUpperCase()}
                                            </span>
                                        </div>
                                        <div>
                                            <p className="text-white font-medium">{member.username}</p>
                                            {member.email && (
                                                <p className="text-gray-400 text-sm">{member.email}</p>
                                            )}
                                        </div>
                                    </div>
                                    {team.canEdit && (
                                        <button
                                            onClick={() => handleRemoveMember(member.id, member.username)}
                                            className="p-2 text-red-400 hover:bg-red-500 hover:bg-opacity-10 rounded transition"
                                            disabled={actionInProgress}
                                            title="Usuń z zespołu"
                                        >
                                            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                            </svg>
                                        </button>
                                    )}
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                {/* Add Member Modal */}
                {showAddMemberModal && (
                    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                        <div className="bg-gray-900 rounded-lg p-6 w-full max-w-md border border-gray-800">
                            <h2 className="text-2xl font-bold text-white mb-4">Dodaj członka do zespołu</h2>

                            {error && (
                                <div className="mb-4 p-3 bg-red-500 bg-opacity-10 border border-red-500 rounded text-red-400 text-sm">
                                    {error}
                                </div>
                            )}

                            {availableUsers.length === 0 ? (
                                <div className="mb-4 p-4 bg-yellow-500 bg-opacity-10 border border-yellow-500 rounded text-yellow-400 text-sm text-center">
                                    <p>Wszyscy aktywni użytkownicy są już w zespole lub nie ma dostępnych użytkowników.</p>
                                </div>
                            ) : (
                                <form onSubmit={handleAddMember}>
                                    <div className="mb-6">
                                        <label className="block text-gray-400 text-sm font-medium mb-2">
                                            Wybierz użytkownika
                                        </label>
                                        <select
                                            value={selectedUserId || ''}
                                            onChange={(e) => setSelectedUserId(parseInt(e.target.value))}
                                            className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-purple-500"
                                            required
                                        >
                                            <option value="">-- Wybierz użytkownika --</option>
                                            {availableUsers.map((user) => (
                                                <option key={user.id} value={user.id}>
                                                    {user.username} {user.email && `(${user.email})`}
                                                </option>
                                            ))}
                                        </select>
                                    </div>

                                    <div className="flex gap-3">
                                        <button
                                            type="button"
                                            onClick={() => {
                                                setShowAddMemberModal(false);
                                                setSelectedUserId(null);
                                                setError(null);
                                            }}
                                            className="flex-1 px-4 py-2 bg-gray-800 hover:bg-gray-700 text-white rounded-lg transition"
                                            disabled={actionInProgress}
                                        >
                                            Anuluj
                                        </button>
                                        <button
                                            type="submit"
                                            className="flex-1 px-4 py-2 bg-purple-500 hover:bg-purple-600 text-white rounded-lg transition disabled:opacity-50 disabled:cursor-not-allowed"
                                            disabled={actionInProgress || !selectedUserId}
                                        >
                                            {actionInProgress ? 'Dodawanie...' : 'Dodaj członka'}
                                        </button>
                                    </div>
                                </form>
                            )}

                            {availableUsers.length === 0 ? (
                                <button
                                    onClick={() => {
                                        setShowAddMemberModal(false);
                                        setError(null);
                                    }}
                                    className="w-full px-4 py-2 bg-gray-800 hover:bg-gray-700 text-white rounded-lg transition"
                                >
                                    Zamknij
                                </button>
                            ) : null}
                        </div>
                    </div>
                )}

                {/* Edit Team Modal */}
                {showEditModal && (
                    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                        <div className="bg-gray-900 rounded-lg p-6 w-full max-w-md border border-gray-800">
                            <h2 className="text-2xl font-bold text-white mb-4">Edytuj zespół</h2>

                            {error && (
                                <div className="mb-4 p-3 bg-red-500 bg-opacity-10 border border-red-500 rounded text-red-400 text-sm">
                                    {error}
                                </div>
                            )}

                            <form onSubmit={handleEditTeam}>
                                <div className="mb-4">
                                    <label className="block text-gray-400 text-sm font-medium mb-2">
                                        Nazwa zespołu *
                                    </label>
                                    <input
                                        type="text"
                                        value={editFormData.name}
                                        onChange={(e) => setEditFormData({ ...editFormData, name: e.target.value })}
                                        className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-purple-500"
                                        placeholder="Wpisz nazwę zespołu..."
                                        maxLength={50}
                                        required
                                    />
                                </div>

                                <div className="mb-6">
                                    <label className="block text-gray-400 text-sm font-medium mb-2">
                                        Opis (opcjonalnie)
                                    </label>
                                    <textarea
                                        value={editFormData.description}
                                        onChange={(e) => setEditFormData({ ...editFormData, description: e.target.value })}
                                        className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-purple-500"
                                        placeholder="Krótki opis zespołu..."
                                        rows={3}
                                        maxLength={200}
                                    />
                                </div>

                                <div className="flex gap-3">
                                    <button
                                        type="button"
                                        onClick={() => {
                                            setShowEditModal(false);
                                            setEditFormData({
                                                name: team?.name || '',
                                                description: team?.description || '',
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
                                        className="flex-1 px-4 py-2 bg-purple-500 hover:bg-purple-600 text-white rounded-lg transition disabled:opacity-50 disabled:cursor-not-allowed"
                                        disabled={actionInProgress}
                                    >
                                        {actionInProgress ? 'Zapisywanie...' : 'Zapisz zmiany'}
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

export default TeamDetailsPage;