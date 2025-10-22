// src/services/commentService.ts
import type { Comment, CreateCommentRequest, UpdateCommentRequest, ApiResponse } from '../types';

const API_BASE_URL = '/api/v1/comments';

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

const commentService = {
    // Get comments for task
    getTaskComments: async (taskId: number): Promise<Comment[]> => {
        try {
            const response = await fetch(`${API_BASE_URL}/tasks/${taskId}`, {
                method: 'GET',
                credentials: 'include',
            });

            const data = await handleResponse<Comment[]>(response);
            return data.data || [];
        } catch (error) {
            console.error('Get comments error:', error);
            throw error;
        }
    },

    // Create comment - FIXED: dodaj taskId do body
    createComment: async (taskId: number, commentData: CreateCommentRequest): Promise<Comment> => {
        try {
            const response = await fetch(`${API_BASE_URL}/tasks/${taskId}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                // ✅ FIX: Dodaj taskId do body requesta
                body: JSON.stringify({
                    taskId: taskId,
                    text: commentData.text
                }),
            });

            const data = await handleResponse<Comment>(response);
            if (!data.data) throw new Error('Failed to create comment');
            return data.data;
        } catch (error) {
            console.error('Create comment error:', error);
            throw error;
        }
    },

    // Update comment
    updateComment: async (id: number, commentData: UpdateCommentRequest): Promise<Comment> => {
        try {
            const response = await fetch(`${API_BASE_URL}/${id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
                body: JSON.stringify(commentData),
            });

            const data = await handleResponse<Comment>(response);
            if (!data.data) throw new Error('Failed to update comment');
            return data.data;
        } catch (error) {
            console.error('Update comment error:', error);
            throw error;
        }
    },

    // Delete comment
    deleteComment: async (id: number): Promise<void> => {
        try {
            const response = await fetch(`${API_BASE_URL}/${id}`, {
                method: 'DELETE',
                credentials: 'include',
            });

            await handleResponse<void>(response);
        } catch (error) {
            console.error('Delete comment error:', error);
            throw error;
        }
    },
};

export default commentService;