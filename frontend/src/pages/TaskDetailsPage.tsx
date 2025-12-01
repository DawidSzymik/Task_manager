// frontend/src/pages/TaskDetailsPage.tsx - Z PODGLĄDEM PLIKÓW
import React, { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import MainLayout from '../components/MainLayout';
import FilePreview from '../components/FilePreview';
import taskService from '../services/taskService';
import projectService from '../services/projectService';
import commentService from '../services/commentService';
import fileService from '../services/fileService';
import statusRequestService from '../services/statusRequestService';
import type {
    Task,
    UpdateTaskRequest,
    Comment,
    UploadedFile,
    StatusChangeRequest,
    ProjectRole
} from '../types';
import UserAvatar from "../components/UserAvatar.tsx";

const TaskDetailsPage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    useNavigate();
    const chatEndRef = useRef<HTMLDivElement>(null);

    const [task, setTask] = useState<Task | null>(null);
    const [comments, setComments] = useState<Comment[]>([]);
    const [files, setFiles] = useState<UploadedFile[]>([]);
    const [, setStatusRequests] = useState<StatusChangeRequest[]>([]);
    const [userRole, setUserRole] = useState<ProjectRole | null>(null);
    const [loading, setLoading] = useState(true);


    // ⭐ NOWY STATE - Podgląd plików
    const [previewFile, setPreviewFile] = useState<{
        id: number;
        name: string;
        type: string;
    } | null>(null);

    const [, setEditFormData] = useState<UpdateTaskRequest>({
        title: '',
        description: '',
        status: 'NEW',
        priority: 'MEDIUM',
        deadline: '',
        assignedToId: undefined,
    });

    // Comments & Files
    const [commentText, setCommentText] = useState('');
    const [editingCommentId, setEditingCommentId] = useState<number | null>(null);
    const [editingCommentText, setEditingCommentText] = useState('');
    const [uploadingFile, setUploadingFile] = useState(false);
    const [selectedFile, setSelectedFile] = useState<File | null>(null);

    useEffect(() => {
        if (id) {
            loadTaskData();
        }
    }, [id]);

    useEffect(() => {
        chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [comments, files]);

    const loadTaskData = async () => {
        try {
            setLoading(true);
            const taskId = parseInt(id!);
            const taskData = await taskService.getTaskById(taskId);
            setTask(taskData);

            if (taskData.project) {
                const [members, commentsData, filesData, requestsData] = await Promise.all([
                    projectService.getProjectMembers(taskData.project.id).catch(() => []),
                    commentService.getTaskComments(taskId).catch(() => []),
                    fileService.getTaskFiles(taskId).catch(() => []),
                    statusRequestService.getTaskRequests(taskId).catch(() => []),
                ]);

                setComments(commentsData);
                setFiles(filesData);
                setStatusRequests(requestsData.filter(r => r.status === 'PENDING'));

                const currentUsername = localStorage.getItem('username');
                const currentMember = members.find(m => m.user.username === currentUsername);
                setUserRole(currentMember?.role || null);
            }

            setEditFormData({
                title: taskData.title,
                description: taskData.description || '',
                status: taskData.status,
                priority: taskData.priority,
                deadline: taskData.deadline ? new Date(taskData.deadline).toISOString().slice(0, 16) : '',
                assignedToId: taskData.assignedTo?.id,
            });
        } catch (error: any) {
            console.error('❌ Failed to load task:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleAddComment = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!commentText.trim()) return;
        try {
            await commentService.createComment(parseInt(id!), { text: commentText.trim() });
            setCommentText('');
            await loadTaskData();
        } catch (error: any) {
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
            alert(error.message || 'Nie udało się zaktualizować komentarza');
        }
    };

    const handleDeleteComment = async (commentId: number) => {
        if (!window.confirm('Czy na pewno chcesz usunąć ten komentarz?')) return;
        try {
            await commentService.deleteComment(commentId);
            await loadTaskData();
        } catch (error: any) {
            alert(error.message || 'Nie udało się usunąć komentarza');
        }
    };

    const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
        if (e.target.files && e.target.files[0]) {
            const file = e.target.files[0];
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
            await loadTaskData();
        } catch (error: any) {
            alert(error.message || 'Nie udało się przesłać pliku');
        } finally {
            setUploadingFile(false);
        }
    };

    const handleDeleteFile = async (fileId: number) => {
        if (!window.confirm('Czy na pewno chcesz usunąć ten plik?')) return;
        try {
            await fileService.deleteFile(fileId);
            await loadTaskData();
        } catch (error: any) {
            alert(error.message || 'Nie udało się usunąć pliku');
        }
    };

    // ⭐ NOWA FUNKCJA - Otwórz podgląd pliku
    const handlePreviewFile = (file: UploadedFile) => {
        console.log('🔍 Opening preview for file:', file);
        console.log('📋 Preview data:', {
            id: file.id,
            name: file.originalName,
            type: file.contentType
        });

        setPreviewFile({
            id: file.id,
            name: file.originalName,
            type: file.contentType,
        });
    };

    // ⭐ NOWA FUNKCJA - Zamknij podgląd
    const handleClosePreview = () => {
        setPreviewFile(null);
    };

    if (loading) {
        return (
            <MainLayout>
                <div className="flex items-center justify-center h-96">
                    <div className="text-gray-400">Ładowanie...</div>
                </div>
            </MainLayout>
        );
    }

    if (!task) {
        return (
            <MainLayout>
                <div className="text-center text-gray-400">Nie znaleziono zadania</div>
            </MainLayout>
        );
    }

    const chatItems = [
        ...comments.map(c => ({ type: 'comment' as const, data: c, timestamp: new Date(c.createdAt) })),
        ...files.map(f => ({ type: 'file' as const, data: f, timestamp: new Date(f.uploadedAt) }))
    ].sort((a, b) => a.timestamp.getTime() - b.timestamp.getTime());

    return (
        <MainLayout>
            <div className="max-w-5xl mx-auto h-[calc(100vh-120px)] flex flex-col">
                {/* Header */}
                <div className="bg-gray-900 rounded-t-lg p-4 border-b border-gray-700">
                    <div className="flex items-start justify-between">
                        <div className="flex-1">
                            <div className="flex items-center gap-3 mb-2">
                                <h1 className="text-2xl font-bold text-white">{task.title}</h1>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Chat container - ZAKTUALIZOWANA SEKCJA Z PROFESJONALNYMI PRZYCISKAMI */}
                <div className="flex-1 overflow-y-auto bg-gray-900 p-4 space-y-4">
                    {chatItems.map((item, idx) => (
                        <div key={`${item.type}-${idx}`} className="flex items-start gap-3">
                            {item.type === 'comment' ? (
                                // ✅ Komentarze z avatarem i profesjonalnymi przyciskami
                                <div className="flex gap-3 w-full">
                                    <UserAvatar user={item.data.author} size="sm" />
                                    <div className="flex-1">
                                        <div className="flex items-center gap-2 mb-1">
                                            <span className="text-sm font-semibold text-white">{item.data.author.username}</span>
                                            <span className="text-xs text-gray-500">{new Date(item.data.createdAt).toLocaleString('pl-PL')}</span>
                                        </div>
                                        {editingCommentId === item.data.id ? (
                                            <div className="space-y-2">
                                                <textarea
                                                    value={editingCommentText}
                                                    onChange={(e) => setEditingCommentText(e.target.value)}
                                                    className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded text-white"
                                                    rows={3}
                                                />
                                                <div className="flex gap-2">
                                                    <button
                                                        onClick={() => handleUpdateComment(item.data.id)}
                                                        className="flex items-center gap-1.5 px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 rounded-lg text-white text-sm transition-all duration-200 shadow-sm hover:shadow-md"
                                                    >
                                                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                                        </svg>
                                                        Zapisz
                                                    </button>
                                                    <button
                                                        onClick={() => {
                                                            setEditingCommentId(null);
                                                            setEditingCommentText('');
                                                        }}
                                                        className="flex items-center gap-1.5 px-3 py-1.5 bg-gray-700 hover:bg-gray-600 rounded-lg text-white text-sm transition-all duration-200"
                                                    >
                                                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                                        </svg>
                                                        Anuluj
                                                    </button>
                                                </div>
                                            </div>
                                        ) : (
                                            <>
                                                <p className="text-gray-300 bg-gray-800 rounded-lg px-4 py-3">{item.data.text}</p>
                                                {item.data.canEdit && (
                                                    <div className="flex gap-2 mt-2">
                                                        <button
                                                            onClick={() => {
                                                                setEditingCommentId(item.data.id);
                                                                setEditingCommentText(item.data.text);
                                                            }}
                                                            className="flex items-center gap-1.5 px-3 py-1.5 text-xs text-blue-400 hover:text-blue-300 hover:bg-blue-500/10 rounded-lg transition-all duration-200"
                                                        >
                                                            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                                                            </svg>
                                                            Edytuj
                                                        </button>
                                                        {item.data.canDelete && (
                                                            <button
                                                                onClick={() => handleDeleteComment(item.data.id)}
                                                                className="flex items-center gap-1.5 px-3 py-1.5 text-xs text-red-400 hover:text-red-300 hover:bg-red-500/10 rounded-lg transition-all duration-200"
                                                            >
                                                                <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                                                </svg>
                                                                Usuń
                                                            </button>
                                                        )}
                                                    </div>
                                                )}
                                            </>
                                        )}
                                    </div>
                                </div>
                            ) : (
                                // ⭐ ZAKTUALIZOWANA SEKCJA PLIKÓW z profesjonalnymi przyciskami
                                <div className="flex gap-3 w-full">
                                    <UserAvatar user={item.data.uploadedBy} size="sm" />
                                    <div className="flex-1">
                                        <div className="flex items-center gap-2 mb-1">
                                            <span className="text-sm font-semibold text-white">{item.data.uploadedBy.username}</span>
                                            <span className="text-xs text-gray-500">{new Date(item.data.uploadedAt).toLocaleString('pl-PL')}</span>
                                        </div>
                                        <div className="bg-gray-800 rounded-lg px-4 py-3">
                                            <div className="flex items-center justify-between">
                                                <div className="flex-1">
                                                    <p className="text-white font-medium">{item.data.originalName}</p>
                                                    <p className="text-xs text-gray-500">{item.data.fileSizeFormatted || `${(item.data.fileSize / 1024).toFixed(1)} KB`}</p>
                                                </div>
                                                <div className="flex gap-2">
                                                    {/* ⭐ PRZYCISK PODGLĄDU - tylko dla PDF i obrazów */}
                                                    {fileService.isPreviewableContentType(item.data.contentType) && (
                                                        <button
                                                            onClick={() => handlePreviewFile(item.data)}
                                                            className="flex items-center gap-1.5 p-2 bg-purple-600 hover:bg-purple-700 rounded-lg text-white transition-all duration-200 shadow-sm hover:shadow-md hover:-translate-y-0.5"
                                                            title="Podgląd pliku"
                                                        >
                                                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                                                            </svg>
                                                        </button>
                                                    )}
                                                    {/* Przycisk pobierania */}
                                                    <a
                                                        href={fileService.getDownloadUrl(item.data.id)}
                                                        download
                                                        className="flex items-center gap-1.5 p-2 bg-blue-600 hover:bg-blue-700 rounded-lg text-white transition-all duration-200 shadow-sm hover:shadow-md hover:-translate-y-0.5"
                                                        title="Pobierz plik"
                                                    >
                                                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                                                        </svg>
                                                    </a>
                                                    {/* Przycisk usuwania */}
                                                    {item.data.canDelete && (
                                                        <button
                                                            onClick={() => handleDeleteFile(item.data.id)}
                                                            className="flex items-center gap-1.5 p-2 bg-red-600 hover:bg-red-700 rounded-lg text-white transition-all duration-200 shadow-sm hover:shadow-md hover:-translate-y-0.5"
                                                            title="Usuń plik"
                                                        >
                                                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                                            </svg>
                                                        </button>
                                                    )}
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            )}
                        </div>
                    ))}
                    <div ref={chatEndRef} />
                </div>

                {/* Input section - ZAKTUALIZOWANE PRZYCISKI */}
                {userRole !== 'VIEWER' && (
                    <div className="bg-gray-900 rounded-b-lg border-t border-gray-700 p-4">
                        <form onSubmit={handleAddComment} className="space-y-3">
                            <textarea
                                value={commentText}
                                onChange={(e) => setCommentText(e.target.value)}
                                placeholder="Napisz komentarz..."
                                className="w-full px-4 py-3 bg-gray-800 border border-gray-700 rounded-lg text-white"
                                rows={3}
                            />
                            <div className="flex justify-between">
                                <div className="flex gap-2">
                                    <input
                                        type="file"
                                        id="file-upload"
                                        onChange={handleFileSelect}
                                        className="hidden"
                                    />
                                    <label
                                        htmlFor="file-upload"
                                        className="flex items-center gap-2 px-4 py-2 bg-gray-700 hover:bg-gray-600 text-gray-300 rounded-lg cursor-pointer transition-all duration-200 shadow-sm hover:shadow-md"
                                    >
                                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.172 7l-6.586 6.586a2 2 0 102.828 2.828l6.414-6.586a4 4 0 00-5.656-5.656l-6.415 6.585a6 6 0 108.486 8.486L20.5 13" />
                                        </svg>
                                        Załącz plik
                                    </label>
                                    {selectedFile && (
                                        <div className="flex items-center gap-2 px-3 py-1 bg-gray-800 rounded-lg border border-gray-700">
                                            <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
                                            </svg>
                                            <span className="text-sm text-gray-300">{selectedFile.name}</span>
                                            <button
                                                type="button"
                                                onClick={() => setSelectedFile(null)}
                                                className="text-gray-500 hover:text-gray-300 transition-colors"
                                            >
                                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                                </svg>
                                            </button>
                                        </div>
                                    )}
                                </div>
                                <div className="flex gap-2">
                                    {selectedFile && (
                                        <button
                                            type="button"
                                            onClick={handleFileUpload}
                                            disabled={uploadingFile}
                                            className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-700 disabled:cursor-not-allowed text-white rounded-lg transition-all duration-200 shadow-sm hover:shadow-md"
                                        >
                                            {uploadingFile ? (
                                                <>
                                                    <svg className="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                                                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                                    </svg>
                                                    Wysyłanie...
                                                </>
                                            ) : (
                                                <>
                                                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                                                    </svg>
                                                    Wyślij plik
                                                </>
                                            )}
                                        </button>
                                    )}
                                    <button
                                        type="submit"
                                        disabled={!commentText.trim()}
                                        className="flex items-center gap-2 px-4 py-2 bg-primary-500 hover:bg-primary-600 disabled:bg-gray-700 disabled:cursor-not-allowed text-white rounded-lg transition-all duration-200 shadow-sm hover:shadow-md"
                                    >
                                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
                                        </svg>
                                        Wyślij
                                    </button>
                                </div>
                            </div>
                        </form>
                    </div>
                )}
            </div>

            {/* ⭐ MODAL PODGLĄDU PLIKÓW */}
            {previewFile && (
                <FilePreview
                    fileId={previewFile.id}
                    fileName={previewFile.name}
                    contentType={previewFile.type}
                    onClose={handleClosePreview}
                />
            )}
        </MainLayout>
    );
};

export default TaskDetailsPage;