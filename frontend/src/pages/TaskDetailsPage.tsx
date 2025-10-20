// src/pages/TaskDetailsPage.tsx
import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import MainLayout from '../components/MainLayout';
import taskService from '../services/taskService';
import projectService from '../services/projectService';
import commentService from '../services/commentService';
import fileService from '../services/fileService';
import type { Task, UpdateTaskRequest, TaskStatus, TaskPriority, ProjectMember, Comment, UploadedFile, CreateCommentRequest } from '../types';

const TaskDetailsPage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();

    const [task, setTask] = useState<Task | null>(null);
    const [projectMembers, setProjectMembers] = useState<ProjectMember[]>([]);
    const [comments, setComments] = useState<Comment[]>([]);
    const [files, setFiles] = useState<UploadedFile[]>([]);
    const [loading, setLoading] = useState(true);

    // Modals & Forms
    const [showEditModal, setShowEditModal] = useState(false);
    const [editFormData, setEditFormData] = useState<UpdateTaskRequest>({
        title: '',
        description: '',
        status: 'NEW',
        priority: 'MEDIUM',
        deadline: '',
        assignedToId: undefined,
    });

    // Comments
    const [commentText, setCommentText] = useState('');
    const [editingCommentId, setEditingCommentId] = useState<number | null>(null);
    const [editingCommentText, setEditingCommentText] = useState('');

    // Files
    const [uploadingFile, setUploadingFile] = useState(false);
    const [selectedFile, setSelectedFile] = useState<File | null>(null);

    const [error, setError] = useState<string | null>(null);
    const [actionInProgress, setActionInProgress] = useState(false);

    useEffect(() => {
        if (id) {
            loadTaskData();
        }
    }, [id]);

    const loadTaskData = async () => {
        try {
            setLoading(true);
            const taskId = parseInt(id!);
            const taskData = await taskService.getTaskById(taskId);
            setTask(taskData);

            // Load project members, comments, and files in parallel
            if (taskData.project) {
                const [members, commentsData, filesData] = await Promise.all([
                    projectService.getProjectMembers(taskData.project.id),
                    commentService.getTaskComments(taskId),
                    fileService.getTaskFiles(taskId),
                ]);

                setProjectMembers(members);
                setComments(commentsData);
                setFiles(filesData);
            }

            setEditFormData({
                title: taskData.title,
                description: taskData.description || '',
                status: taskData.status,
                priority: taskData.priority,
                deadline: taskData.deadline ? taskData.deadline.slice(0, 16) : '',
                assignedToId: taskData.assignedTo?.id,
            });
        } catch (error: any) {
            console.error('Failed to load task:', error);
            setError(error.message || 'Nie udało się załadować zadania');
        } finally {
            setLoading(false);
        }
    };

    // Task handlers
    const handleUpdateTask = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!editFormData.title?.trim()) {
            setError('Tytuł zadania jest wymagany');
            return;
        }

        try {
            setActionInProgress(true);
            setError(null);
            await taskService.updateTask(parseInt(id!), editFormData);
            setShowEditModal(false);
            await loadTaskData();
        } catch (error: any) {
            console.error('Failed to update task:', error);
            setError(error.message || 'Nie udało się zaktualizować zadania');
        } finally {
            setActionInProgress(false);
        }
    };

    const handleQuickStatusChange = async (newStatus: TaskStatus) => {
        try {
            await taskService.updateTask(parseInt(id!), { status: newStatus });
            await loadTaskData();
        } catch (error: any) {
            console.error('Failed to update status:', error);
            alert(error.message || 'Nie udało się zmienić statusu');
        }
    };

    const handleQuickPriorityChange = async (newPriority: TaskPriority) => {
        try {
            await taskService.updateTask(parseInt(id!), { priority: newPriority });
            await loadTaskData();
        } catch (error: any) {
            console.error('Failed to update priority:', error);
            alert(error.message || 'Nie udało się zmienić priorytetu');
        }
    };

    const handleQuickAssignChange = async (userId: number | undefined) => {
        try {
            await taskService.updateTask(parseInt(id!), { assignedToId: userId });
            await loadTaskData();
        } catch (error: any) {
            console.error('Failed to update assignment:', error);
            alert(error.message || 'Nie udało się zmienić przypisania');
        }
    };

    const handleDeleteTask = async () => {
        if (!window.confirm(`Czy na pewno chcesz usunąć zadanie "${task?.title}"?`)) return;

        try {
            await taskService.deleteTask(parseInt(id!));
            navigate(task?.project ? `/projects/${task.project.id}` : '/tasks');
        } catch (error: any) {
            console.error('Failed to delete task:', error);
            alert(error.message || 'Nie udało się usunąć zadania');
        }
    };

    // Comment handlers
    const handleAddComment = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!commentText.trim()) return;

        try {
            const commentData: CreateCommentRequest = { text: commentText.trim() };
            await commentService.createComment(parseInt(id!), commentData);
            setCommentText('');
            await loadTaskData();
        } catch (error: any) {
            console.error('Failed to add comment:', error);
            alert(error.message || 'Nie udało się dodać komentarza');
        }
    };

    const handleUpdateComment = async (commentId: number) => {
        if (!editingCommentText.trim()) return;

        try {
            await commentService.updateComment(commentId, { text: editingCommentText.trim() });
            setEditingCommentId(null);
            setEditingCommentText('');
            await loadTaskData();
        } catch (error: any) {
            console.error('Failed to update comment:', error);
            alert(error.message || 'Nie udało się zaktualizować komentarza');
        }
    };

    const handleDeleteComment = async (commentId: number) => {
        if (!window.confirm('Czy na pewno chcesz usunąć ten komentarz?')) return;

        try {
            await commentService.deleteComment(commentId);
            await loadTaskData();
        } catch (error: any) {
            console.error('Failed to delete comment:', error);
            alert(error.message || 'Nie udało się usunąć komentarza');
        }
    };

    // File handlers
    const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
        if (e.target.files && e.target.files[0]) {
            const file = e.target.files[0];

            // Check file size (max 10MB)
            if (file.size > 10 * 1024 * 1024) {
                alert('Plik jest za duży! Maksymalny rozmiar to 10MB.');
                return;
            }

            setSelectedFile(file);
        }
    };

    const handleFileUpload = async () => {
        if (!selectedFile) return;

        try {
            setUploadingFile(true);
            await fileService.uploadFile(parseInt(id!), selectedFile);
            setSelectedFile(null);
            // Reset file input
            const fileInput = document.getElementById('fileInput') as HTMLInputElement;
            if (fileInput) fileInput.value = '';
            await loadTaskData();
        } catch (error: any) {
            console.error('Failed to upload file:', error);
            alert(error.message || 'Nie udało się przesłać pliku');
        } finally {
            setUploadingFile(false);
        }
    };

    const handleDeleteFile = async (fileId: number, fileName: string) => {
        if (!window.confirm(`Czy na pewno chcesz usunąć plik "${fileName}"?`)) return;

        try {
            await fileService.deleteFile(fileId);
            await loadTaskData();
        } catch (error: any) {
            console.error('Failed to delete file:', error);
            alert(error.message || 'Nie udało się usunąć pliku');
        }
    };

    // Helper functions
    const getStatusColor = (status: TaskStatus) => {
        switch (status) {
            case 'NEW': return 'bg-blue-500';
            case 'IN_PROGRESS': return 'bg-yellow-500';
            case 'COMPLETED': return 'bg-green-500';
            case 'CANCELLED': return 'bg-gray-500';
            default: return 'bg-gray-500';
        }
    };

    const getStatusLabel = (status: TaskStatus) => {
        switch (status) {
            case 'NEW': return 'Nowe';
            case 'IN_PROGRESS': return 'W trakcie';
            case 'COMPLETED': return 'Ukończone';
            case 'CANCELLED': return 'Anulowane';
            default: return status;
        }
    };

    const getPriorityColor = (priority: TaskPriority) => {
        switch (priority) {
            case 'LOW': return 'text-green-400 bg-green-500';
            case 'MEDIUM': return 'text-yellow-400 bg-yellow-500';
            case 'HIGH': return 'text-orange-400 bg-orange-500';
            case 'URGENT': return 'text-red-400 bg-red-500';
            default: return 'text-gray-400 bg-gray-500';
        }
    };

    const getPriorityLabel = (priority: TaskPriority) => {
        switch (priority) {
            case 'LOW': return 'Niski';
            case 'MEDIUM': return 'Średni';
            case 'HIGH': return 'Wysoki';
            case 'URGENT': return 'Pilny';
            default: return priority;
        }
    };

    const formatFileSize = (bytes: number): string => {
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
        return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
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
                        <p className="text-gray-400">Ładowanie zadania...</p>
                    </div>
                </div>
            </MainLayout>
        );
    }

    if (!task) {
        return (
            <MainLayout>
                <div className="text-center py-16">
                    <p className="text-gray-400 text-lg">Zadanie nie zostało znalezione</p>
                    <button
                        onClick={() => navigate('/tasks')}
                        className="mt-4 px-6 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-lg transition"
                    >
                        Wróć do zadań
                    </button>
                </div>
            </MainLayout>
        );
    }

    return (
        <MainLayout>
            <div className="max-w-7xl mx-auto">
                {/* Header */}
                <div className="mb-8">
                    <button
                        onClick={() => task.project ? navigate(`/projects/${task.project.id}`) : navigate('/tasks')}
                        className="flex items-center gap-2 text-gray-400 hover:text-white mb-4 transition"
                    >
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
                        </svg>
                        Wróć {task.project ? 'do projektu' : 'do zadań'}
                    </button>

                    <div className="flex items-start justify-between">
                        <div className="flex-1">
                            <div className="flex items-center gap-3 mb-3">
                                <span className={`px-4 py-1.5 rounded-full text-sm font-semibold ${getStatusColor(task.status)} text-white`}>
                                    {getStatusLabel(task.status)}
                                </span>
                                <span className={`px-4 py-1.5 rounded-full text-sm font-semibold ${getPriorityColor(task.priority).split(' ')[1]} bg-opacity-20 ${getPriorityColor(task.priority).split(' ')[0]}`}>
                                    {getPriorityLabel(task.priority)}
                                </span>
                            </div>
                            <h1 className="text-3xl font-bold text-white mb-2">{task.title}</h1>
                            {task.project && (
                                <button
                                    onClick={() => navigate(`/projects/${task.project!.id}`)}
                                    className="text-emerald-400 hover:text-emerald-300 text-sm transition"
                                >
                                    📁 {task.project.name}
                                </button>
                            )}
                        </div>
                        <div className="flex gap-2">
                            <button
                                onClick={() => setShowEditModal(true)}
                                className="px-4 py-2 bg-gray-800 hover:bg-gray-700 text-white rounded-lg transition"
                            >
                                Edytuj
                            </button>
                            <button
                                onClick={handleDeleteTask}
                                className="px-4 py-2 bg-red-500 bg-opacity-10 hover:bg-red-500 hover:bg-opacity-20 text-red-400 rounded-lg transition"
                            >
                                Usuń
                            </button>
                        </div>
                    </div>
                </div>

                {/* Error message */}
                {error && (
                    <div className="mb-6 p-4 bg-red-500 bg-opacity-10 border border-red-500 rounded-lg text-red-400">
                        {error}
                    </div>
                )}

                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                    {/* Main content - Left column */}
                    <div className="lg:col-span-2 space-y-6">
                        {/* Description */}
                        <div className="bg-gray-900 border border-gray-800 rounded-lg p-6">
                            <h2 className="text-xl font-bold text-white mb-4">Opis</h2>
                            {task.description ? (
                                <p className="text-gray-300 whitespace-pre-wrap">{task.description}</p>
                            ) : (
                                <p className="text-gray-500 italic">Brak opisu</p>
                            )}
                        </div>

                        {/* Quick Actions */}
                        <div className="bg-gray-900 border border-gray-800 rounded-lg p-6">
                            <h2 className="text-xl font-bold text-white mb-4">Szybkie akcje</h2>

                            {/* Status change */}
                            <div className="mb-6">
                                <h3 className="text-sm font-semibold text-gray-400 mb-3">Zmień status</h3>
                                <div className="grid grid-cols-2 gap-3">
                                    {(['NEW', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'] as TaskStatus[]).map((status) => (
                                        <button
                                            key={status}
                                            onClick={() => handleQuickStatusChange(status)}
                                            disabled={task.status === status}
                                            className={`p-3 rounded-lg border-2 transition text-sm font-semibold ${
                                                task.status === status
                                                    ? `${getStatusColor(status)} border-transparent text-white`
                                                    : 'bg-gray-800 border-gray-700 text-gray-300 hover:border-gray-600'
                                            }`}
                                        >
                                            {getStatusLabel(status)}
                                        </button>
                                    ))}
                                </div>
                            </div>

                            {/* Priority change */}
                            <div className="mb-6">
                                <h3 className="text-sm font-semibold text-gray-400 mb-3">Zmień priorytet</h3>
                                <div className="grid grid-cols-2 gap-3">
                                    {(['LOW', 'MEDIUM', 'HIGH', 'URGENT'] as TaskPriority[]).map((priority) => (
                                        <button
                                            key={priority}
                                            onClick={() => handleQuickPriorityChange(priority)}
                                            disabled={task.priority === priority}
                                            className={`p-3 rounded-lg border-2 transition text-sm font-semibold ${
                                                task.priority === priority
                                                    ? `${getPriorityColor(priority).split(' ')[1]} bg-opacity-20 border-transparent ${getPriorityColor(priority).split(' ')[0]}`
                                                    : 'bg-gray-800 border-gray-700 text-gray-300 hover:border-gray-600'
                                            }`}
                                        >
                                            {getPriorityLabel(priority)}
                                        </button>
                                    ))}
                                </div>
                            </div>

                            {/* Assignment change */}
                            <div>
                                <h3 className="text-sm font-semibold text-gray-400 mb-3">Przypisz do</h3>
                                <select
                                    value={task.assignedTo?.id || ''}
                                    onChange={(e) => handleQuickAssignChange(e.target.value ? Number(e.target.value) : undefined)}
                                    className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-emerald-500"
                                >
                                    <option value="">Nie przypisano</option>
                                    {projectMembers.map((member) => (
                                        <option key={member.user.id} value={member.user.id}>
                                            {member.user.username}
                                        </option>
                                    ))}
                                </select>
                            </div>
                        </div>

                        {/* Comments Section */}
                        <div className="bg-gray-900 border border-gray-800 rounded-lg p-6">
                            <h2 className="text-xl font-bold text-white mb-4">
                                Komentarze ({comments.length})
                            </h2>

                            {/* Add comment form */}
                            <form onSubmit={handleAddComment} className="mb-6">
                                <textarea
                                    value={commentText}
                                    onChange={(e) => setCommentText(e.target.value)}
                                    className="w-full px-4 py-3 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-emerald-500 mb-3"
                                    placeholder="Dodaj komentarz..."
                                    rows={3}
                                />
                                <button
                                    type="submit"
                                    disabled={!commentText.trim()}
                                    className="px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-lg transition disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                    Dodaj komentarz
                                </button>
                            </form>

                            {/* Comments list */}
                            <div className="space-y-4">
                                {comments.length === 0 ? (
                                    <p className="text-gray-500 italic text-center py-4">Brak komentarzy</p>
                                ) : (
                                    comments.map((comment) => (
                                        <div key={comment.id} className="bg-gray-800 rounded-lg p-4">
                                            <div className="flex items-start justify-between mb-2">
                                                <div className="flex items-center gap-2">
                                                    <div className="w-8 h-8 bg-emerald-500 rounded-full flex items-center justify-center">
                                                        <span className="text-white text-sm font-semibold">
                                                            {comment.author.username.charAt(0).toUpperCase()}
                                                        </span>
                                                    </div>
                                                    <div>
                                                        <p className="text-white font-medium">{comment.author.username}</p>
                                                        <p className="text-gray-400 text-xs">
                                                            {new Date(comment.createdAt).toLocaleString('pl-PL')}
                                                        </p>
                                                    </div>
                                                </div>
                                                {comment.canEdit && (
                                                    <div className="flex gap-2">
                                                        <button
                                                            onClick={() => {
                                                                setEditingCommentId(comment.id);
                                                                setEditingCommentText(comment.text);
                                                            }}
                                                            className="text-blue-400 hover:text-blue-300 text-sm"
                                                        >
                                                            Edytuj
                                                        </button>
                                                        {comment.canDelete && (
                                                            <button
                                                                onClick={() => handleDeleteComment(comment.id)}
                                                                className="text-red-400 hover:text-red-300 text-sm"
                                                            >
                                                                Usuń
                                                            </button>
                                                        )}
                                                    </div>
                                                )}
                                            </div>
                                            {editingCommentId === comment.id ? (
                                                <div>
                                                    <textarea
                                                        value={editingCommentText}
                                                        onChange={(e) => setEditingCommentText(e.target.value)}
                                                        className="w-full px-3 py-2 bg-gray-700 border border-gray-600 rounded text-white focus:outline-none focus:border-emerald-500 mb-2"
                                                        rows={3}
                                                    />
                                                    <div className="flex gap-2">
                                                        <button
                                                            onClick={() => handleUpdateComment(comment.id)}
                                                            className="px-3 py-1 bg-emerald-500 hover:bg-emerald-600 text-white rounded text-sm"
                                                        >
                                                            Zapisz
                                                        </button>
                                                        <button
                                                            onClick={() => {
                                                                setEditingCommentId(null);
                                                                setEditingCommentText('');
                                                            }}
                                                            className="px-3 py-1 bg-gray-600 hover:bg-gray-500 text-white rounded text-sm"
                                                        >
                                                            Anuluj
                                                        </button>
                                                    </div>
                                                </div>
                                            ) : (
                                                <p className="text-gray-300 whitespace-pre-wrap">{comment.text}</p>
                                            )}
                                        </div>
                                    ))
                                )}
                            </div>
                        </div>

                        {/* Files Section */}
                        <div className="bg-gray-900 border border-gray-800 rounded-lg p-6">
                            <h2 className="text-xl font-bold text-white mb-4">
                                Załączniki ({files.length})
                            </h2>

                            {/* Upload form */}
                            <div className="mb-6 p-4 bg-gray-800 rounded-lg border-2 border-dashed border-gray-700">
                                <input
                                    id="fileInput"
                                    type="file"
                                    onChange={handleFileSelect}
                                    className="hidden"
                                />
                                <label
                                    htmlFor="fileInput"
                                    className="flex flex-col items-center justify-center cursor-pointer"
                                >
                                    <svg className="w-12 h-12 text-gray-500 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                                    </svg>
                                    <p className="text-gray-400 text-sm">
                                        {selectedFile ? selectedFile.name : 'Kliknij aby wybrać plik'}
                                    </p>
                                    <p className="text-gray-500 text-xs mt-1">Maksymalny rozmiar: 10MB</p>
                                </label>
                                {selectedFile && (
                                    <div className="mt-3 flex justify-center gap-2">
                                        <button
                                            onClick={handleFileUpload}
                                            disabled={uploadingFile}
                                            className="px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-lg transition disabled:opacity-50"
                                        >
                                            {uploadingFile ? 'Przesyłanie...' : 'Prześlij'}
                                        </button>
                                        <button
                                            onClick={() => {
                                                setSelectedFile(null);
                                                const fileInput = document.getElementById('fileInput') as HTMLInputElement;
                                                if (fileInput) fileInput.value = '';
                                            }}
                                            className="px-4 py-2 bg-gray-700 hover:bg-gray-600 text-white rounded-lg transition"
                                        >
                                            Anuluj
                                        </button>
                                    </div>
                                )}
                            </div>

                            {/* Files list */}
                            <div className="space-y-3">
                                {files.length === 0 ? (
                                    <p className="text-gray-500 italic text-center py-4">Brak załączników</p>
                                ) : (
                                    files.map((file) => (
                                        <div key={file.id} className="flex items-center justify-between p-3 bg-gray-800 rounded-lg hover:bg-gray-750 transition">
                                            <div className="flex items-center gap-3 flex-1">
                                                <div className="w-10 h-10 bg-blue-500 bg-opacity-20 rounded flex items-center justify-center flex-shrink-0">
                                                    <svg className="w-5 h-5 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
                                                    </svg>
                                                </div>
                                                <div className="flex-1 min-w-0">
                                                    <p className="text-white font-medium truncate">{file.originalName}</p>
                                                    <div className="flex items-center gap-3 text-xs text-gray-400">
                                                        <span>{formatFileSize(file.fileSize)}</span>
                                                        <span>•</span>
                                                        <span>{file.uploadedBy.username}</span>
                                                        <span>•</span>
                                                        <span>{new Date(file.uploadedAt).toLocaleDateString('pl-PL')}</span>
                                                    </div>
                                                </div>
                                            </div>
                                            <div className="flex gap-2">
                                                <a
                                                    href={fileService.getDownloadUrl(file.id)}
                                                    download
                                                    className="p-2 text-emerald-400 hover:bg-emerald-500 hover:bg-opacity-10 rounded transition"
                                                    title="Pobierz"
                                                >
                                                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                                                    </svg>
                                                </a>
                                                {file.canDelete && (
                                                    <button
                                                        onClick={() => handleDeleteFile(file.id, file.originalName)}
                                                        className="p-2 text-red-400 hover:bg-red-500 hover:bg-opacity-10 rounded transition"
                                                        title="Usuń"
                                                    >
                                                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                                        </svg>
                                                    </button>
                                                )}
                                            </div>
                                        </div>
                                    ))
                                )}
                            </div>
                        </div>
                    </div>

                    {/* Sidebar - Right column */}
                    <div className="space-y-6">
                        {/* Task info */}
                        <div className="bg-gray-900 border border-gray-800 rounded-lg p-6">
                            <h2 className="text-lg font-bold text-white mb-4">Informacje</h2>
                            <div className="space-y-4">
                                {/* Assigned to */}
                                <div>
                                    <p className="text-gray-400 text-sm mb-1">Przypisane do</p>
                                    {task.assignedTo ? (
                                        <div className="flex items-center gap-2">
                                            <div className="w-8 h-8 bg-emerald-500 rounded-full flex items-center justify-center">
                                                <span className="text-white text-sm font-semibold">
                                                    {task.assignedTo.username.charAt(0).toUpperCase()}
                                                </span>
                                            </div>
                                            <span className="text-white">{task.assignedTo.username}</span>
                                        </div>
                                    ) : (
                                        <p className="text-gray-500 italic">Nie przypisano</p>
                                    )}
                                </div>

                                {/* Created by */}
                                {task.createdBy && (
                                    <div>
                                        <p className="text-gray-400 text-sm mb-1">Utworzone przez</p>
                                        <div className="flex items-center gap-2">
                                            <div className="w-8 h-8 bg-blue-500 rounded-full flex items-center justify-center">
                                                <span className="text-white text-sm font-semibold">
                                                    {task.createdBy.username.charAt(0).toUpperCase()}
                                                </span>
                                            </div>
                                            <span className="text-white">{task.createdBy.username}</span>
                                        </div>
                                    </div>
                                )}

                                {/* Deadline */}
                                <div>
                                    <p className="text-gray-400 text-sm mb-1">Deadline</p>
                                    {task.deadline ? (
                                        <div className="flex items-center gap-2">
                                            <svg className="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                                            </svg>
                                            <span className="text-white">
                                                {new Date(task.deadline).toLocaleString('pl-PL')}
                                            </span>
                                        </div>
                                    ) : (
                                        <p className="text-gray-500 italic">Brak deadline</p>
                                    )}
                                </div>

                                {/* Created at */}
                                {task.createdAt && (
                                    <div>
                                        <p className="text-gray-400 text-sm mb-1">Utworzono</p>
                                        <span className="text-white text-sm">
                                            {new Date(task.createdAt).toLocaleString('pl-PL')}
                                        </span>
                                    </div>
                                )}

                                {/* Completed at */}
                                {task.completedAt && (
                                    <div>
                                        <p className="text-gray-400 text-sm mb-1">Ukończono</p>
                                        <span className="text-white text-sm">
                                            {new Date(task.completedAt).toLocaleString('pl-PL')}
                                        </span>
                                    </div>
                                )}

                                {/* Activity stats */}
                                <div className="pt-4 border-t border-gray-800">
                                    <div className="flex items-center justify-between mb-2">
                                        <span className="text-gray-400 text-sm">Komentarze</span>
                                        <span className="text-white font-semibold">{comments.length}</span>
                                    </div>
                                    <div className="flex items-center justify-between">
                                        <span className="text-gray-400 text-sm">Załączniki</span>
                                        <span className="text-white font-semibold">{files.length}</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Edit Task Modal */}
                {showEditModal && (
                    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
                        <div className="bg-gray-900 rounded-lg p-6 w-full max-w-2xl border border-gray-800 max-h-[90vh] overflow-y-auto">
                            <h2 className="text-2xl font-bold text-white mb-4">Edytuj zadanie</h2>

                            {error && (
                                <div className="mb-4 p-3 bg-red-500 bg-opacity-10 border border-red-500 rounded text-red-400 text-sm">
                                    {error}
                                </div>
                            )}

                            <form onSubmit={handleUpdateTask}>
                                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                                    <div className="md:col-span-2">
                                        <label className="block text-gray-400 text-sm font-medium mb-2">Tytuł *</label>
                                        <input
                                            type="text"
                                            value={editFormData.title}
                                            onChange={(e) => setEditFormData({ ...editFormData, title: e.target.value })}
                                            className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-emerald-500"
                                            maxLength={200}
                                            required
                                        />
                                    </div>

                                    <div className="md:col-span-2">
                                        <label className="block text-gray-400 text-sm font-medium mb-2">Opis</label>
                                        <textarea
                                            value={editFormData.description}
                                            onChange={(e) => setEditFormData({ ...editFormData, description: e.target.value })}
                                            className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-emerald-500"
                                            rows={4}
                                            maxLength={2000}
                                        />
                                    </div>

                                    <div>
                                        <label className="block text-gray-400 text-sm font-medium mb-2">Status</label>
                                        <select
                                            value={editFormData.status}
                                            onChange={(e) => setEditFormData({ ...editFormData, status: e.target.value as TaskStatus })}
                                            className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-emerald-500"
                                        >
                                            <option value="NEW">Nowe</option>
                                            <option value="IN_PROGRESS">W trakcie</option>
                                            <option value="COMPLETED">Ukończone</option>
                                            <option value="CANCELLED">Anulowane</option>
                                        </select>
                                    </div>

                                    <div>
                                        <label className="block text-gray-400 text-sm font-medium mb-2">Priorytet</label>
                                        <select
                                            value={editFormData.priority}
                                            onChange={(e) => setEditFormData({ ...editFormData, priority: e.target.value as TaskPriority })}
                                            className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-emerald-500"
                                        >
                                            <option value="LOW">Niski</option>
                                            <option value="MEDIUM">Średni</option>
                                            <option value="HIGH">Wysoki</option>
                                            <option value="URGENT">Pilny</option>
                                        </select>
                                    </div>

                                    <div>
                                        <label className="block text-gray-400 text-sm font-medium mb-2">Deadline</label>
                                        <input
                                            type="datetime-local"
                                            value={editFormData.deadline}
                                            onChange={(e) => setEditFormData({ ...editFormData, deadline: e.target.value })}
                                            className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-emerald-500"
                                        />
                                    </div>

                                    <div>
                                        <label className="block text-gray-400 text-sm font-medium mb-2">Przypisz do</label>
                                        <select
                                            value={editFormData.assignedToId || ''}
                                            onChange={(e) => setEditFormData({ ...editFormData, assignedToId: e.target.value ? Number(e.target.value) : undefined })}
                                            className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white focus:outline-none focus:border-emerald-500"
                                        >
                                            <option value="">Nie przypisano</option>
                                            {projectMembers.map((member) => (
                                                <option key={member.user.id} value={member.user.id}>
                                                    {member.user.username}
                                                </option>
                                            ))}
                                        </select>
                                    </div>
                                </div>

                                <div className="flex gap-3 mt-6">
                                    <button
                                        type="button"
                                        onClick={() => {
                                            setShowEditModal(false);
                                            setError(null);
                                        }}
                                        className="flex-1 px-4 py-2 bg-gray-800 hover:bg-gray-700 text-white rounded-lg transition"
                                        disabled={actionInProgress}
                                    >
                                        Anuluj
                                    </button>
                                    <button
                                        type="submit"
                                        className="flex-1 px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-lg transition disabled:opacity-50"
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

export default TaskDetailsPage;