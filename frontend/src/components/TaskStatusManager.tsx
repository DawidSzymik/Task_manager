// frontend/src/components/TaskStatusManager.tsx
import React, { useState } from 'react';
import type { Task, TaskStatus, ProjectRole } from '../types';
import statusRequestService from '../services/statusRequestService';

interface TaskStatusManagerProps {
    task: Task;
    userRole: ProjectRole;
    onStatusChanged: () => void;
}

const TaskStatusManager: React.FC<TaskStatusManagerProps> = ({ task, userRole, onStatusChanged }) => {
    const [selectedStatus, setSelectedStatus] = useState<TaskStatus>(task.status);
    const [isChanging, setIsChanging] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState<string | null>(null);

    const statusOptions: { value: TaskStatus; label: string; color: string }[] = [
        { value: 'NEW', label: 'Nowe', color: 'bg-gray-500' },
        { value: 'IN_PROGRESS', label: 'W trakcie', color: 'bg-blue-500' },
        { value: 'COMPLETED', label: 'Ukończone', color: 'bg-green-500' },
        { value: 'CANCELLED', label: 'Anulowane', color: 'bg-red-500' },
    ];

    const handleStatusChange = async () => {
        if (selectedStatus === task.status) {
            setError('Wybierz inny status');
            return;
        }

        setIsChanging(true);
        setError(null);
        setSuccess(null);

        try {
            await statusRequestService.createRequest({
                taskId: task.id,
                newStatus: selectedStatus,
            });

            if (userRole === 'ADMIN') {
                setSuccess('Status zmieniony!');
            } else {
                setSuccess('Prośba o zmianę statusu wysłana do administratora');
            }

            setTimeout(() => {
                setSuccess(null);
                onStatusChanged();
            }, 2000);
        } catch (err: any) {
            setError(err.message || 'Nie udało się zmienić statusu');
        } finally {
            setIsChanging(false);
        }
    };

    const currentStatusOption = statusOptions.find((opt) => opt.value === task.status);
    const canChangeStatus = userRole === 'ADMIN' || userRole === 'MEMBER';

    if (userRole === 'VIEWER') {
        return (
            <div className="flex items-center gap-2">
                <span
                    className={`px-3 py-1 rounded-full text-xs font-medium text-white ${
                        currentStatusOption?.color || 'bg-gray-500'
                    }`}
                >
                    {currentStatusOption?.label || task.status}
                </span>
            </div>
        );
    }

    return (
        <div className="space-y-2">
            <div className="flex items-center gap-2">
                <select
                    value={selectedStatus}
                    onChange={(e) => setSelectedStatus(e.target.value as TaskStatus)}
                    disabled={isChanging || !canChangeStatus}
                    className="px-3 py-1.5 bg-gray-800 border border-gray-700 text-white rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
                >
                    {statusOptions.map((option) => (
                        <option key={option.value} value={option.value}>
                            {option.label}
                        </option>
                    ))}
                </select>

                {selectedStatus !== task.status && (
                    <button
                        onClick={handleStatusChange}
                        disabled={isChanging}
                        className="px-3 py-1.5 bg-emerald-500 hover:bg-emerald-600 disabled:bg-gray-600 text-white rounded-lg text-xs font-medium transition"
                    >
                        {isChanging ? (
                            <span className="flex items-center gap-1">
                                <svg className="animate-spin h-3 w-3" fill="none" viewBox="0 0 24 24">
                                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                    <path
                                        className="opacity-75"
                                        fill="currentColor"
                                        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                                    />
                                </svg>
                                Zmiana...
                            </span>
                        ) : userRole === 'ADMIN' ? (
                            'Zmień'
                        ) : (
                            'Poproś o zmianę'
                        )}
                    </button>
                )}
            </div>

            {error && (
                <p className="text-red-500 text-xs">{error}</p>
            )}

            {success && (
                <p className="text-green-500 text-xs">{success}</p>
            )}
        </div>
    );
};

export default TaskStatusManager;