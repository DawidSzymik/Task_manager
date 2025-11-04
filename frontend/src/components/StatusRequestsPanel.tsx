// frontend/src/components/StatusRequestsPanel.tsx
import React, { useState, useEffect } from 'react';
import type { StatusChangeRequest } from '../types';
import statusRequestService from '../services/statusRequestService';

interface StatusRequestsPanelProps {
    projectId: number;
    onRequestHandled: () => void;
}

const StatusRequestsPanel: React.FC<StatusRequestsPanelProps> = ({ projectId, onRequestHandled }) => {
    const [requests, setRequests] = useState<StatusChangeRequest[]>([]);
    const [loading, setLoading] = useState(true);
    const [actionInProgress, setActionInProgress] = useState<number | null>(null);
    const [rejectReason, setRejectReason] = useState<{ [key: number]: string }>({});
    const [showRejectModal, setShowRejectModal] = useState<number | null>(null);

    useEffect(() => {
        loadRequests();
    }, [projectId]);

    const loadRequests = async () => {
        try {
            setLoading(true);
            const data = await statusRequestService.getProjectRequests(projectId);
            setRequests(data);
        } catch (err) {
            console.error('Failed to load requests:', err);
        } finally {
            setLoading(false);
        }
    };

    const handleApprove = async (requestId: number) => {
        setActionInProgress(requestId);
        try {
            await statusRequestService.approveRequest(requestId);
            await loadRequests();
            onRequestHandled();
        } catch (err: any) {
            alert(err.message || 'Nie udało się zatwierdzić prośby');
        } finally {
            setActionInProgress(null);
        }
    };

    const handleReject = async (requestId: number) => {
        const reason = rejectReason[requestId] || '';
        if (!reason.trim()) {
            alert('Podaj powód odrzucenia');
            return;
        }

        setActionInProgress(requestId);
        try {
            await statusRequestService.rejectRequest(requestId, reason);
            setShowRejectModal(null);
            setRejectReason({ ...rejectReason, [requestId]: '' });
            await loadRequests();
            onRequestHandled();
        } catch (err: any) {
            alert(err.message || 'Nie udało się odrzucić prośby');
        } finally {
            setActionInProgress(null);
        }
    };

    const getStatusLabel = (status: string) => {
        const labels: { [key: string]: string } = {
            NEW: 'Nowe',
            IN_PROGRESS: 'W trakcie',
            COMPLETED: 'Ukończone',
            CANCELLED: 'Anulowane',
        };
        return labels[status] || status;
    };

    const getStatusColor = (status: string) => {
        const colors: { [key: string]: string } = {
            NEW: 'text-gray-400',
            IN_PROGRESS: 'text-blue-400',
            COMPLETED: 'text-green-400',
            CANCELLED: 'text-red-400',
        };
        return colors[status] || 'text-gray-400';
    };

    if (loading) {
        return (
            <div className="bg-gray-900 border border-gray-800 rounded-lg p-6">
                <div className="flex items-center justify-center py-8">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-500" />
                </div>
            </div>
        );
    }

    const pendingRequests = requests.filter((r) => r.status === 'PENDING');

    if (pendingRequests.length === 0) {
        return (
            <div className="bg-gray-900 border border-gray-800 rounded-lg p-6">
                <h3 className="text-lg font-semibold text-white mb-4">Oczekujące prośby o zmianę statusu</h3>
                <p className="text-gray-400 text-sm text-center py-4">Brak oczekujących prośb</p>
            </div>
        );
    }

    return (
        <div className="bg-gray-900 border border-gray-800 rounded-lg p-6">
            <h3 className="text-lg font-semibold text-white mb-4">
                Oczekujące prośby o zmianę statusu ({pendingRequests.length})
            </h3>

            <div className="space-y-3">
                {pendingRequests.map((request) => (
                    <div key={request.id} className="bg-gray-800 border border-gray-700 rounded-lg p-4">
                        <div className="flex items-start justify-between gap-4">
                            <div className="flex-1">
                                <h4 className="text-white font-medium mb-1">{request.task.title}</h4>
                                <div className="flex items-center gap-3 text-sm text-gray-400">
                                    <span>
                                        {request.requestedBy.username}
                                    </span>
                                    <span>•</span>
                                    <span className={getStatusColor(request.currentStatus)}>
                                        {getStatusLabel(request.currentStatus)}
                                    </span>
                                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7l5 5m0 0l-5 5m5-5H6" />
                                    </svg>
                                    <span className={getStatusColor(request.requestedStatus)}>
                                        {getStatusLabel(request.requestedStatus)}
                                    </span>
                                </div>
                                <p className="text-xs text-gray-500 mt-1">
                                    {new Date(request.createdAt).toLocaleString('pl-PL')}
                                </p>
                            </div>

                            <div className="flex items-center gap-2">
                                <button
                                    onClick={() => handleApprove(request.id)}
                                    disabled={actionInProgress === request.id}
                                    className="px-3 py-1.5 bg-green-600 hover:bg-green-700 disabled:bg-gray-600 text-white rounded text-sm font-medium transition"
                                >
                                    {actionInProgress === request.id ? 'Zatwierdzanie...' : 'Zatwierdź'}
                                </button>
                                <button
                                    onClick={() => setShowRejectModal(request.id)}
                                    disabled={actionInProgress === request.id}
                                    className="px-3 py-1.5 bg-red-600 hover:bg-red-700 disabled:bg-gray-600 text-white rounded text-sm font-medium transition"
                                >
                                    Odrzuć
                                </button>
                            </div>
                        </div>

                        {/* Reject Modal */}
                        {showRejectModal === request.id && (
                            <div className="mt-3 pt-3 border-t border-gray-700">
                                <label className="block text-sm text-gray-300 mb-2">Powód odrzucenia:</label>
                                <textarea
                                    value={rejectReason[request.id] || ''}
                                    onChange={(e) =>
                                        setRejectReason({ ...rejectReason, [request.id]: e.target.value })
                                    }
                                    placeholder="Wpisz powód odrzucenia..."
                                    className="w-full px-3 py-2 bg-gray-900 border border-gray-700 text-white rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-red-500"
                                    rows={3}
                                />
                                <div className="flex items-center gap-2 mt-2">
                                    <button
                                        onClick={() => handleReject(request.id)}
                                        disabled={actionInProgress === request.id}
                                        className="px-3 py-1.5 bg-red-600 hover:bg-red-700 disabled:bg-gray-600 text-white rounded text-sm font-medium transition"
                                    >
                                        {actionInProgress === request.id ? 'Odrzucanie...' : 'Potwierdź odrzucenie'}
                                    </button>
                                    <button
                                        onClick={() => {
                                            setShowRejectModal(null);
                                            setRejectReason({ ...rejectReason, [request.id]: '' });
                                        }}
                                        disabled={actionInProgress === request.id}
                                        className="px-3 py-1.5 bg-gray-700 hover:bg-gray-600 disabled:bg-gray-600 text-white rounded text-sm font-medium transition"
                                    >
                                        Anuluj
                                    </button>
                                </div>
                            </div>
                        )}
                    </div>
                ))}
            </div>
        </div>
    );
};

export default StatusRequestsPanel;