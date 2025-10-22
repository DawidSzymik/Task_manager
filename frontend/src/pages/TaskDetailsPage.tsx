// frontend/src/pages/TaskDetailsPage.tsx - FINAL FIX
import React, { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import MainLayout from '../components/MainLayout';
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

    // frontend/src/pages/TaskDetailsPage.tsx

    const loadTaskData = async () => {
        try {
            setLoading(true);
            const taskId = parseInt(id!);
            const taskData = await taskService.getTaskById(taskId);
            console.log('✅ Task loaded:', taskData); // ← DODAJ
            setTask(taskData);

            if (taskData.project) {
                console.log('Loading project data for project:', taskData.project.id); // ← DODAJ

                const [members, commentsData, filesData, requestsData] = await Promise.all([
                    projectService.getProjectMembers(taskData.project.id).catch(err => {
                        console.error('❌ Members error:', err);
                        return []; // Zwróć pustą tablicę zamiast crashować
                    }),
                    commentService.getTaskComments(taskId).catch(err => {
                        console.error('❌ Comments error:', err);
                        return [];
                    }),
                    fileService.getTaskFiles(taskId).catch(err => {
                        console.error('❌ Files error:', err);
                        return [];
                    }),
                    statusRequestService.getTaskRequests(taskId).catch(err => {
                        console.error('❌ Requests error:', err);
                        return [];
                    }),
                ]);

                console.log('✅ Loaded data:', { members, commentsData, filesData, requestsData }); // ← DODAJ

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
                <div className="bg-gray-900 rounded-t-lg p-4 border-b border-gray-700">
                    <div className="flex items-start justify-between">
                        <div className="flex-1">
                            <div className="flex items-center gap-3 mb-2">
                                <h1 className="text-2xl font-bold text-white">{task.title}</h1>
                                {userRole === 'MEMBER' ? (
                                    <button
                                        onClick={() => setShowStatusRequestModal(true)}
                                        className={`px-2 py-1 rounded text-xs font-semibold text-white ${getStatusColor(task.status)} hover:opacity-80 transition`}
                                    >
                                        {getStatusLabel(task.status)} ✏️
                                    </button>
                                ) : (
                                    <span className={`px-2 py-1 rounded text-xs font-semibold text-white ${getStatusColor(task.status)}`}>
                            {getStatusLabel(task.status)}
                        </span>
                                )}
                                <span className={`px-2 py-1 rounded text-xs font-semibold text-white ${getPriorityColor(task.priority)}`}>
                        {task.priority}
                    </span>
                                {userRole === 'ADMIN' && statusRequests.length > 0 && (
                                    <button
                                        onClick={() => setShowPendingRequestsModal(true)}
                                        className="px-2 py-1 rounded text-xs font-semibold text-white bg-orange-500 hover:bg-orange-600 transition"
                                    >
                                        🔔 {statusRequests.length}
                                    </button>
                                )}
                            </div>
                            {task.description && <p className="text-gray-400 text-sm">{task.description}</p>}
                        </div>

                        {/* ✅ ZMIENIONO: Menu dla ADMIN i MEMBER */}
                        {(userRole === 'ADMIN' || userRole === 'MEMBER') && (
                            <div className="relative">
                                <button onClick={() => setShowActionsMenu(!showActionsMenu)} className="p-2 bg-gray-800 hover:bg-gray-700 rounded-lg">
                                    <svg className="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 5v.01M12 12v.01M12 19v.01M12 6a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2z" />
                                    </svg>
                                </button>
                                {showActionsMenu && (
                                    <div className="absolute right-0 mt-2 w-48 bg-gray-800 rounded-lg shadow-xl border border-gray-700 z-10">
                                        <button
                                            onClick={() => {
                                                setShowEditModal(true);
                                                setShowActionsMenu(false);
                                            }}
                                            className="w-full px-4 py-2 text-left text-gray-300 hover:bg-gray-700 rounded-t-lg"
                                        >
                                            ✏️ Edytuj zadanie
                                        </button>
                                        {userRole === 'ADMIN' && (
                                            <button
                                                onClick={() => {
                                                    handleDeleteTask();
                                                    setShowActionsMenu(false);
                                                }}
                                                className="w-full px-4 py-2 text-left text-red-400 hover:bg-gray-700 rounded-b-lg"
                                            >
                                                🗑️ Usuń zadanie
                                            </button>
                                        )}
                                    </div>
                                )}
                            </div>
                        )}
                    </div>
                </div>

                <div className="flex-1 bg-gray-900 overflow-y-auto p-4 space-y-3">
                    {chatItems.length === 0 ? (
                        <div className="text-center text-gray-500 py-12">
                            Brak komentarzy i plików. Rozpocznij rozmowę! 💬
                        </div>
                    ) : (
                    chatItems.map((item) => (
                        <div key={`${item.type}-${item.data.id}`}>
                            {item.type === 'comment' ? (
                                <div className="flex gap-3">
                                    <div className="w-8 h-8 bg-emerald-500 rounded-full flex items-center justify-center text-white font-semibold">
                                        {item.data.author.username.charAt(0).toUpperCase()}
                                    </div>
                                    <div className="flex-1">
                                        <div className="flex items-center gap-2 mb-1">
                                            <span className="text-sm font-semibold text-white">{item.data.author.username}</span>
                                            <span className="text-xs text-gray-500">{new Date(item.data.createdAt).toLocaleString('pl-PL')}</span>
                                        </div>
                                        {editingCommentId === item.data.id ? (
                                            <div className="space-y-2">
                                                <textarea value={editingCommentText} onChange={(e) => setEditingCommentText(e.target.value)} className="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white" rows={3} />
                                                <div className="flex gap-2">
                                                    <button onClick={() => handleUpdateComment(item.data.id)} className="px-3 py-1 bg-emerald-500 hover:bg-emerald-600 text-white rounded text-sm">Zapisz</button>
                                                    <button onClick={() => { setEditingCommentId(null); setEditingCommentText(''); }} className="px-3 py-1 bg-gray-700 hover:bg-gray-600 text-white rounded text-sm">Anuluj</button>
                                                </div>
                                            </div>
                                        ) : (
                                            <>
                                                <div className="bg-gray-800 rounded-lg px-4 py-2 inline-block max-w-2xl">
                                                    <p className="text-gray-300 whitespace-pre-wrap">{item.data.text}</p>
                                                </div>
                                                {item.data.canEdit && (
                                                    <div className="flex gap-2 mt-1">
                                                        <button onClick={() => { setEditingCommentId(item.data.id); setEditingCommentText(item.data.text); }} className="text-xs text-gray-500 hover:text-emerald-400">Edytuj</button>
                                                        {item.data.canDelete && <button onClick={() => handleDeleteComment(item.data.id)} className="text-xs text-gray-500 hover:text-red-400">Usuń</button>}
                                                    </div>
                                                )}
                                            </>
                                        )}
                                    </div>
                                </div>
                            ) : (
                                <div className="flex gap-3">
                                    <div className="w-8 h-8 bg-blue-500 rounded-full flex items-center justify-center">📎</div>
                                    <div className="flex-1">
                                        <div className="flex items-center gap-2 mb-1">
                                            <span className="text-sm font-semibold text-white">{item.data.uploadedBy.username}</span>
                                            <span className="text-xs text-gray-500">{new Date(item.data.uploadedAt).toLocaleString('pl-PL')}</span>
                                        </div>
                                        <div className="bg-gray-800 rounded-lg px-4 py-3 inline-flex items-center gap-3">
                                            <div>
                                                <p className="text-white font-medium">{item.data.originalName}</p>
                                                <p className="text-xs text-gray-500">{(item.data.fileSize / 1024).toFixed(1)} KB</p>
                                            </div>
                                            <div className="flex gap-2">
                                                <a href={fileService.getDownloadUrl(item.data.id)} download className="p-2 bg-emerald-500 hover:bg-emerald-600 rounded text-white">⬇</a>
                                                {item.data.canDelete && <button onClick={() => handleDeleteFile(item.data.id)} className="p-2 bg-red-500 hover:bg-red-600 rounded text-white">🗑</button>}
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            )}
                        </div>
                    )))}
                    <div ref={chatEndRef} />
                </div>

                {userRole !== 'VIEWER' && (
                    <div className="bg-gray-900 rounded-b-lg border-t border-gray-700 p-4">
                        <form onSubmit={handleAddComment} className="space-y-3">
                            <textarea value={commentText} onChange={(e) => setCommentText(e.target.value)} placeholder="Napisz komentarz..." className="w-full px-4 py-3 bg-gray-800 border border-gray-700 rounded-lg text-white" rows={3} />
                            <div className="flex justify-between">
                                <div className="flex gap-2">
                                    <input type="file" id="file-upload" onChange={handleFileSelect} className="hidden" />
                                    <label htmlFor="file-upload" className="px-4 py-2 bg-gray-800 hover:bg-gray-700 text-gray-300 rounded-lg cursor-pointer">📎 Załącz</label>
                                    {selectedFile && (
                                        <div className="flex items-center gap-2 px-3 py-1 bg-gray-800 rounded-lg">
                                            <span className="text-sm text-gray-300">{selectedFile.name}</span>
                                            <button type="button" onClick={() => setSelectedFile(null)} className="text-gray-500">✕</button>
                                        </div>
                                    )}
                                </div>
                                <div className="flex gap-2">
                                    {selectedFile && <button type="button" onClick={handleFileUpload} disabled={uploadingFile} className="px-4 py-2 bg-blue-500 hover:bg-blue-600 text-white rounded-lg">{uploadingFile ? 'Wysyłanie...' : 'Wyślij plik'}</button>}
                                    <button type="submit" disabled={!commentText.trim()} className="px-6 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-lg disabled:opacity-50">Wyślij</button>
                                </div>
                            </div>
                        </form>
                    </div>
                )}

                {showEditModal && (
                    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                        <div className="bg-gray-900 rounded-lg p-6 w-full max-w-2xl">
                            <h2 className="text-2xl font-bold text-white mb-4">Edytuj zadanie</h2>
                            <form onSubmit={handleUpdateTask} className="space-y-4">
                                <input type="text" value={editFormData.title} onChange={(e) => setEditFormData({ ...editFormData, title: e.target.value })} className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white" required />
                                <textarea value={editFormData.description} onChange={(e) => setEditFormData({ ...editFormData, description: e.target.value })} className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white" rows={4} />
                                <div className="grid grid-cols-2 gap-4">
                                    <select value={editFormData.status} onChange={(e) => setEditFormData({ ...editFormData, status: e.target.value as TaskStatus })} className="px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white">
                                        <option value="NEW">Nowe</option>
                                        <option value="IN_PROGRESS">W trakcie</option>
                                        <option value="COMPLETED">Ukończone</option>
                                        <option value="CANCELLED">Anulowane</option>
                                    </select>
                                    <select value={editFormData.priority} onChange={(e) => setEditFormData({ ...editFormData, priority: e.target.value as TaskPriority })} className="px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white">
                                        <option value="LOW">Niski</option>
                                        <option value="MEDIUM">Średni</option>
                                        <option value="HIGH">Wysoki</option>
                                        <option value="URGENT">Pilne</option>
                                    </select>
                                </div>
                                <div className="flex gap-3">
                                    <button type="button" onClick={() => setShowEditModal(false)} className="flex-1 px-4 py-2 bg-gray-800 hover:bg-gray-700 text-white rounded-lg">Anuluj</button>
                                    <button type="submit" className="flex-1 px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-lg">Zapisz</button>
                                </div>
                            </form>
                        </div>
                    </div>
                )}

                {showStatusRequestModal && (
                    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                        <div className="bg-gray-900 rounded-lg p-6 w-full max-w-md">
                            <h2 className="text-xl font-bold text-white mb-4">Prośba o zmianę statusu</h2>
                            <select value={selectedNewStatus} onChange={(e) => setSelectedNewStatus(e.target.value as TaskStatus)} className="w-full px-4 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white mb-4">
                                <option value="NEW">Nowe</option>
                                <option value="IN_PROGRESS">W trakcie</option>
                                <option value="COMPLETED">Ukończone</option>
                                <option value="CANCELLED">Anulowane</option>
                            </select>
                            <div className="flex gap-3">
                                <button onClick={() => setShowStatusRequestModal(false)} className="flex-1 px-4 py-2 bg-gray-800 text-white rounded-lg">Anuluj</button>
                                <button onClick={handleRequestStatusChange} className="flex-1 px-4 py-2 bg-emerald-500 text-white rounded-lg">Wyślij</button>
                            </div>
                        </div>
                    </div>
                )}

                {showPendingRequestsModal && (
                    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                        <div className="bg-gray-900 rounded-lg p-6 w-full max-w-2xl max-h-[80vh] overflow-y-auto">
                            <div className="flex justify-between mb-4">
                                <h2 className="text-xl font-bold text-white">Oczekujące prośby</h2>
                                <button onClick={() => setShowPendingRequestsModal(false)} className="text-gray-400">✕</button>
                            </div>
                            {statusRequests.map(req => (
                                <div key={req.id} className="bg-gray-800 rounded-lg p-4 mb-3">
                                    <p className="text-white mb-2">{req.requestedBy.username}: {getStatusLabel(req.currentStatus as TaskStatus)} → {getStatusLabel(req.requestedStatus as TaskStatus)}</p>
                                    {selectedRequestToReject === req.id ? (
                                        <div>
                                            <textarea value={rejectReason} onChange={(e) => setRejectReason(e.target.value)} placeholder="Powód..." className="w-full px-3 py-2 bg-gray-900 rounded text-white mb-2" rows={2} />
                                            <button onClick={handleRejectRequest} className="px-3 py-1 bg-red-500 text-white rounded mr-2">Potwierdź</button>
                                            <button onClick={() => setSelectedRequestToReject(null)} className="px-3 py-1 bg-gray-700 text-white rounded">Anuluj</button>
                                        </div>
                                    ) : (
                                        <div className="flex gap-2">
                                            <button onClick={() => handleApproveRequest(req.id)} className="flex-1 px-3 py-2 bg-green-500 text-white rounded">✓ Zatwierdź</button>
                                            <button onClick={() => setSelectedRequestToReject(req.id)} className="flex-1 px-3 py-2 bg-red-500 text-white rounded">✕ Odrzuć</button>
                                        </div>
                                    )}
                                </div>
                            ))}
                        </div>
                    </div>
                )}
            </div>
        </MainLayout>
    );
};

export default TaskDetailsPage;