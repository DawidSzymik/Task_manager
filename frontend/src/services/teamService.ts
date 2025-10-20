// src/services/teamService.ts
import type { Team, CreateTeamRequest, UpdateTeamRequest, User, ApiResponse } from '../types';

const API_BASE_URL = '/api/v1/teams';

const handleResponse = async <T>(response: Response): Promise<ApiResponse<T>> => {
    const data = await response.json();

    if (!response.ok || !data.success) {
        throw {
            message: data.error || data.message || 'Wystąpił błąd',
            status: response.status,
        };
    }

    return data;
};

export const teamService = {
    // Get all teams
    getAllTeams: async (myTeams: boolean = false): Promise<Team[]> => {
        try {
            const url = myTeams ? `${API_BASE_URL}?myTeams=true` : API_BASE_URL;
            const response = await fetch(url, {
                method: 'GET',
                credentials: 'include',
            });

            const data = await handleResponse<Team[]>(response);
            return data.data || [];
        } catch (error) {
            console.error('Get teams error:', error);
            throw error;
        }
    },

    // Get team by ID
    getTeamById: async (id: number): Promise<Team> => {
        try {
            const response = await fetch(`${API_BASE_URL}/${id}`, {
                method: 'GET',
                credentials: 'include',
            });

            const data = await handleResponse<Team>(response);
            if (!data.data) throw new Error('Team not found');
            return data.data;
        } catch (error) {
            console.error('Get team error:', error);
            throw error;
        }
    },

    // Create new team
    createTeam: async (teamData: CreateTeamRequest): Promise<Team> => {
        try {
            const response = await fetch(API_BASE_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify(teamData),
            });

            const data = await handleResponse<Team>(response);
            if (!data.data) throw new Error('Failed to create team');
            return data.data;
        } catch (error) {
            console.error('Create team error:', error);
            throw error;
        }
    },

    // Update team
    updateTeam: async (id: number, teamData: UpdateTeamRequest): Promise<Team> => {
        try {
            const response = await fetch(`${API_BASE_URL}/${id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify(teamData),
            });

            const data = await handleResponse<Team>(response);
            if (!data.data) throw new Error('Failed to update team');
            return data.data;
        } catch (error) {
            console.error('Update team error:', error);
            throw error;
        }
    },

    // Delete team
    deleteTeam: async (id: number): Promise<void> => {
        try {
            const response = await fetch(`${API_BASE_URL}/${id}`, {
                method: 'DELETE',
                credentials: 'include',
            });

            await handleResponse<void>(response);
        } catch (error) {
            console.error('Delete team error:', error);
            throw error;
        }
    },

    // Get team members
    getTeamMembers: async (id: number): Promise<User[]> => {
        try {
            const response = await fetch(`${API_BASE_URL}/${id}/members`, {
                method: 'GET',
                credentials: 'include',
            });

            const data = await handleResponse<User[]>(response);
            return data.data || [];
        } catch (error) {
            console.error('Get team members error:', error);
            throw error;
        }
    },

    // Add member to team
    addMemberToTeam: async (teamId: number, userId: number): Promise<void> => {
        try {
            const response = await fetch(`${API_BASE_URL}/${teamId}/members`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify({ userId }),
            });

            await handleResponse<void>(response);
        } catch (error) {
            console.error('Add member error:', error);
            throw error;
        }
    },

    // Remove member from team
    removeMemberFromTeam: async (teamId: number, userId: number): Promise<void> => {
        try {
            const response = await fetch(`${API_BASE_URL}/${teamId}/members/${userId}`, {
                method: 'DELETE',
                credentials: 'include',
            });

            await handleResponse<void>(response);
        } catch (error) {
            console.error('Remove member error:', error);
            throw error;
        }
    },
};

export default teamService;