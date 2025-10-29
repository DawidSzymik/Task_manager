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
    TaskStatus,
    TaskPriority,
    Comment,
    UploadedFile,
    StatusChangeRequest,
    ProjectRole
} from '../types';

const TaskDetailsPage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const chatEndRef = useRef<HTMLDivElement>(null);

    const [task, setTask] = useState<Task | null>(null);
    const [comments, setComments] = useState<Comment[]>([]);
    const [files, setFiles] = useState<UploadedFile[]>([]);
    const [statusRequests, setStatusRequests] = useState<StatusChangeRequest[]>([]);
    const [userRole, setUserRole] = useState<ProjectRole | null>(null);
    const [loading, setLoading] = useState(true);

    // UI State
    const [showActionsMenu, setShowActionsMenu] = useState(false);
    const [showEditModal, setShowEditModal] = useState(false);
    const [showStatusRequestModal, setShowStatusRequestModal] = useState(false);
    const [showPendingRequestsModal, setShowPendingRequestsModal] = useState(false);
    const [selectedNewStatus, setSelectedNewStatus] = useState<TaskStatus>('NEW');
    const [rejectReason, setRejectReason] = useState('');
    const [selectedRequestToReject, setSelectedRequestToReject] = useState<number | null>(null);

    // ⭐ NOWY STATE - Podgląd plików
    const [previewFile, setPreviewFile] = useState<{
        id: number;
        name: string;
        type: string;
    } | null>(null);

    const [editFormData, setEditFormData] = useState<UpdateTaskRequest>({
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

    // ... (wszystkie pozostałe handlery bez zmian)
    const handleUpdateTask = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            await taskService.updateTask(parseInt(id!), editFormData);
            setShowEditModal(false);
            await loadTaskData();
        } catch (error: any) {
            alert(error.message || 'Nie udało się zaktualizować zadania');
        }
    };

    const handleDeleteTask = async () => {
        if (!window.confirm('Czy na pewno chcesz usunąć to zadanie?')) return;
        try {
            await taskService.deleteTask(parseInt(id!));
            alert('Zadanie zostało usunięte');
            navigate(task?.project ? `/projects/${task.project.id}` : '/tasks');
        } catch (error: any) {
            alert(error.message || 'Nie udało się usunąć zadania');
        }
    };

    const handleRequestStatusChange = async () => {
        try {
            await statusRequestService.createRequest({
                taskId: parseInt(id!),
                newStatus: selectedNewStatus,
            });
            setShowStatusRequestModal(false);
            await loadTaskData();
            alert('Prośba o zmianę statusu została wysłana');
        } catch (error: any) {
            alert(error.message || 'Nie udało się wysłać prośby');
        }
    };

    const handleApproveRequest = async (requestId: number) => {
        try {
            await statusRequestService.approveRequest(requestId);
            await loadTaskData();
            alert('Prośba została zatwierdzona');
        } catch (error: any) {
            alert(error.message || 'Nie udało się zatwierdzić prośby');
        }
    };

    const handleRejectRequest = async () => {
        if (!selectedRequestToReject || !rejectReason.trim()) return;
        try {
            await statusRequestService.rejectRequest(selectedRequestToReject, rejectReason);
            setSelectedRequestToReject(null);
            setRejectReason('');
            await loadTaskData();
            alert('Prośba została odrzucona');
        } catch (error: any) {
            alert(error.message || 'Nie udało się odrzucić prośby');
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

    const getPriorityColor = (priority: TaskPriority) => {
        const colors: Record<TaskPriority, string> = {
            LOW: 'bg-blue-500',
            MEDIUM: 'bg-yellow-500',
            HIGH: 'bg-orange-500',
            URGENT: 'bg-red-500',
        };
        return colors[priority] || 'bg-gray-500';
    };

    const getStatusColor = (status: TaskStatus) => {
        const colors: Record<TaskStatus, string> = {
            NEW: 'bg-blue-500',
            IN_PROGRESS: 'bg-yellow-500',
            COMPLETED: 'bg-green-500',
            CANCELLED: 'bg-red-500',
        };
        return colors[status] || 'bg-gray-500';
    };

    const getStatusLabel = (status: TaskStatus) => {
        const labels: Record<TaskStatus, string> = {
            NEW: 'Nowe',
            IN_PROGRESS: 'W trakcie',
            COMPLETED: 'Ukończone',
            CANCELLED: 'Anulowane',
        };
        return labels[status] || status;
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
                {/* Header - bez zmian */}
                <div className="bg-gray-900 rounded-t-lg p-4 border-b border-gray-700">
                    <div className="flex items-start justify-between">
                        <div className="flex-1">
                            <div className="flex items-center gap-3 mb-2">
                                <h1 className="text-2xl font-bold text-white">{task.title}</h1>
                                {/* ... reszta headera bez zmian ... */}
                            </div>
                        </div>
                    </div>
                </div>

                {/* Chat container - ZAKTUALIZOWANA SEKCJA PLIKÓW */}
                <div className="flex-1 overflow-y-auto bg-gray-900 p-4 space-y-4">
                    {chatItems.map((item, idx) => (
                        <div key={`${item.type}-${idx}`} className="flex items-start gap-3">
                            {item.type === 'comment' ? (
                                // Komentarze bez zmian
                                <div className="flex gap-3 w-full">
                                    <div className="w-8 h-8 bg-emerald-500 rounded-full flex items-center justify-center">💬</div>
                                    <div className="flex-1">
                                        <div className="flex items-center gap-2 mb-1">
                                            <span className="text-sm font-semibold text-white">{item.data.author.username}</span>
                                            <span className="text-xs text-gray-500">{new Date(item.data.createdAt).toLocaleString('pl-PL')}</span>
                                        </div>
                                        {editingCommentId === item.data.id ? (
                                            <div className="space-y-2">
                                                <textarea value={editingCommentText} onChange={(e) => setEditingCommentText(e.target.value)} className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded text-white" rows={3} />
                                                <div className="flex gap-2">
                                                    <button onClick={() => handleUpdateComment(item.data.id)} className="px-3 py-1 bg-emerald-500 hover:bg-emerald-600 rounded text-white text-sm">Zapisz</button>
                                                    <button onClick={() => { setEditingCommentId(null); setEditingCommentText(''); }} className="px-3 py-1 bg-gray-700 hover:bg-gray-600 rounded text-white text-sm">Anuluj</button>
                                                </div>
                                            </div>
                                        ) : (
                                            <>
                                                <p className="text-gray-300 bg-gray-800 rounded-lg px-4 py-3">{item.data.text}</p>
                                                {item.data.canEdit && (
                                                    <div className="flex gap-2 mt-2">
                                                        <button onClick={() => { setEditingCommentId(item.data.id); setEditingCommentText(item.data.text); }} className="text-xs text-blue-400 hover:text-blue-300">Edytuj</button>
                                                        {item.data.canDelete && <button onClick={() => handleDeleteComment(item.data.id)} className="text-xs text-red-400 hover:text-red-300">Usuń</button>}
                                                    </div>
                                                )}
                                            </>
                                        )}
                                    </div>
                                </div>
                            ) : (
                                // ⭐ ZAKTUALIZOWANA SEKCJA PLIKÓW z przyciskiem podglądu
                                <div className="flex gap-3 w-full">
                                    <div className="w-8 h-8 bg-blue-500 rounded-full flex items-center justify-center">📎</div>
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
                                                            className="p-2 bg-emerald-500 hover:bg-emerald-600 rounded text-white transition-colors"
                                                            title="Podgląd pliku"
                                                        >
                                                            👁️
                                                        </button>
                                                    )}
                                                    {/* Przycisk pobierania */}
                                                    <a
                                                        href={fileService.getDownloadUrl(item.data.id)}
                                                        download
                                                        className="p-2 bg-blue-500 hover:bg-blue-600 rounded text-white transition-colors"
                                                        title="Pobierz plik"
                                                    >
                                                        ⬇️
                                                    </a>
                                                    {/* Przycisk usuwania */}
                                                    {item.data.canDelete && (
                                                        <button
                                                            onClick={() => handleDeleteFile(item.data.id)}
                                                            className="p-2 bg-red-500 hover:bg-red-600 rounded text-white transition-colors"
                                                            title="Usuń plik"
                                                        >
                                                            🗑️
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

                {/* Input section - bez zmian */}
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
                                        className="px-4 py-2 bg-gray-800 hover:bg-gray-700 text-gray-300 rounded-lg cursor-pointer"
                                    >
                                        📎 Załącz
                                    </label>
                                    {selectedFile && (
                                        <div className="flex items-center gap-2 px-3 py-1 bg-gray-800 rounded-lg">
                                            <span className="text-sm text-gray-300">{selectedFile.name}</span>
                                            <button
                                                type="button"
                                                onClick={() => setSelectedFile(null)}
                                                className="text-gray-500"
                                            >
                                                ✕
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
                                            className="px-4 py-2 bg-blue-500 hover:bg-blue-600 text-white rounded-lg"
                                        >
                                            {uploadingFile ? 'Wysyłanie...' : 'Wyślij plik'}
                                        </button>
                                    )}
                                    <button
                                        type="submit"
                                        disabled={!commentText.trim()}
                                        className="px-4 py-2 bg-emerald-500 hover:bg-emerald-600 disabled:bg-gray-700 disabled:cursor-not-allowed text-white rounded-lg"
                                    >
                                        Wyślij
                                    </button>
                                </div>
                            </div>
                        </form>
                    </div>
                )}
            </div>

            {/* ⭐ MODAL PODGLĄDU PLIKÓW - renderuje się gdy previewFile !== null */}
            {previewFile && (
                <FilePreview
                    fileId={previewFile.id}
                    fileName={previewFile.name}
                    contentType={previewFile.type}
                    onClose={handleClosePreview}
                />
            )}

            {/* Pozostałe modale bez zmian... */}
        </MainLayout>
    );
};

export default TaskDetailsPage;