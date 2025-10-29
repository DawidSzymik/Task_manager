// src/services/fileService.ts
import type { UploadedFile, ApiResponse } from '../types';

const API_BASE_URL = '/api/v1/files';

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

const fileService = {
    // Get files for task
    getTaskFiles: async (taskId: number): Promise<UploadedFile[]> => {
        try {
            const response = await fetch(`${API_BASE_URL}/tasks/${taskId}`, {
                method: 'GET',
                credentials: 'include',
            });

            const data = await handleResponse<UploadedFile[]>(response);
            return data.data || [];
        } catch (error) {
            console.error('Get files error:', error);
            throw error;
        }
    },

    // Upload file
    uploadFile: async (taskId: number, file: File): Promise<UploadedFile> => {
        try {
            const formData = new FormData();
            formData.append('file', file);

            const response = await fetch(`${API_BASE_URL}/tasks/${taskId}`, {
                method: 'POST',
                credentials: 'include',
                body: formData,
            });

            const data = await handleResponse<UploadedFile>(response);
            if (!data.data) throw new Error('Failed to upload file');
            return data.data;
        } catch (error) {
            console.error('Upload file error:', error);
            throw error;
        }
    },

    // Delete file
    deleteFile: async (id: number): Promise<void> => {
        try {
            const response = await fetch(`${API_BASE_URL}/${id}`, {
                method: 'DELETE',
                credentials: 'include',
            });

            await handleResponse<void>(response);
        } catch (error) {
            console.error('Delete file error:', error);
            throw error;
        }
    },

    // Get download URL
    getDownloadUrl: (id: number): string => {
        return `${API_BASE_URL}/${id}/download`;
    },
    getPreviewUrl: (id: number): string => {
        return `${API_BASE_URL}/${id}/preview`;
    },

    // Check if file can be previewed
    canPreview: async (id: number): Promise<boolean> => {
        try {
            const response = await fetch(`${API_BASE_URL}/${id}/can-preview`, {
                method: 'GET',
                credentials: 'include',
            });

            const data = await handleResponse<{ canPreview: boolean }>(response);
            return data.data?.canPreview || false;
        } catch (error) {
            console.error('Can preview check error:', error);
            return false;
        }
    },

    // Check if content type is previewable (client-side helper)
    isPreviewableContentType: (contentType: string): boolean => {
        if (!contentType) return false;

        return (
            contentType === 'application/pdf' ||
            contentType.startsWith('image/') ||
            contentType === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' ||
            contentType === 'application/vnd.ms-excel'
        );
    },
};

export default fileService;