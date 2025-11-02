// src/services/searchService.ts

const API_BASE_URL = '/api/v1/search';

export interface SearchResultUser {
    id: number;
    username: string;
    email?: string;
    systemRole: string;
    type: 'user';
    url: string;
}

export interface SearchResultTask {
    id: number;
    title: string;
    description?: string;
    status: string;
    priority: string;
    projectName: string;
    projectId: number;
    type: 'task';
    url: string;
}

export interface SearchResultProject {
    id: number;
    name: string;
    description?: string;
    memberCount: number;
    taskCount: number;
    type: 'project';
    url: string;
}

export interface SearchResponse {
    success: boolean;
    query: string;
    users: SearchResultUser[];
    tasks: SearchResultTask[];
    projects: SearchResultProject[];
    totalResults: number;
}

const handleResponse = async <T>(response: Response): Promise<T> => {
    const data = await response.json();
    if (!response.ok || !data.success) {
        throw new Error(data.error || data.message || 'Search failed');
    }
    return data as T;
};

const searchService = {
    /**
     * Global search across users, tasks, and projects
     * @param query - Search query string
     * @param limit - Maximum results per category (default 5)
     */
    globalSearch: async (query: string, limit: number = 5): Promise<SearchResponse> => {
        try {
            const params = new URLSearchParams();
            params.append('q', query);
            params.append('limit', limit.toString());

            const response = await fetch(`${API_BASE_URL}?${params.toString()}`, {
                method: 'GET',
                credentials: 'include',
            });

            return await handleResponse<SearchResponse>(response);
        } catch (error) {
            console.error('Global search error:', error);
            throw error;
        }
    },
};

export default searchService;