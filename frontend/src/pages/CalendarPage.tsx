// src/pages/CalendarPage.tsx
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import MainLayout from '../components/MainLayout';
import taskService from '../services/taskService';
import type { Task } from '../types';

interface CalendarDay {
    date: Date;
    isCurrentMonth: boolean;
    tasks: Task[];
}

const CalendarPage: React.FC = () => {
    const navigate = useNavigate();
    const [currentDate, setCurrentDate] = useState(new Date());
    const [tasks, setTasks] = useState<Task[]>([]);
    const [calendarDays, setCalendarDays] = useState<CalendarDay[]>([]);
    const [loading, setLoading] = useState(true);
    const [selectedDay, setSelectedDay] = useState<Date | null>(null);

    useEffect(() => {
        loadTasks();
    }, []);

    useEffect(() => {
        generateCalendar();
    }, [currentDate, tasks]);

    const loadTasks = async () => {
        try {
            setLoading(true);
            const allTasks = await taskService.getAllTasks();
            setTasks(allTasks);
        } catch (error) {
            console.error('Failed to load tasks:', error);
        } finally {
            setLoading(false);
        }
    };

    const generateCalendar = () => {
        const year = currentDate.getFullYear();
        const month = currentDate.getMonth();

        const firstDay = new Date(year, month, 1);
        const lastDay = new Date(year, month + 1, 0);
        const prevLastDay = new Date(year, month, 0);

        const firstDayOfWeek = firstDay.getDay() === 0 ? 6 : firstDay.getDay() - 1;
        const lastDate = lastDay.getDate();
        const prevLastDate = prevLastDay.getDate();

        const days: CalendarDay[] = [];

        // Previous month days
        for (let i = firstDayOfWeek; i > 0; i--) {
            const date = new Date(year, month - 1, prevLastDate - i + 1);
            days.push({
                date,
                isCurrentMonth: false,
                tasks: getTasksForDate(date),
            });
        }

        // Current month days
        for (let i = 1; i <= lastDate; i++) {
            const date = new Date(year, month, i);
            days.push({
                date,
                isCurrentMonth: true,
                tasks: getTasksForDate(date),
            });
        }

        // Next month days
        const remainingDays = 42 - days.length; // 6 rows * 7 days
        for (let i = 1; i <= remainingDays; i++) {
            const date = new Date(year, month + 1, i);
            days.push({
                date,
                isCurrentMonth: false,
                tasks: getTasksForDate(date),
            });
        }

        setCalendarDays(days);
    };

    const getTasksForDate = (date: Date): Task[] => {
        return tasks.filter(task => {
            if (!task.deadline) return false;
            const taskDate = new Date(task.deadline);
            return (
                taskDate.getDate() === date.getDate() &&
                taskDate.getMonth() === date.getMonth() &&
                taskDate.getFullYear() === date.getFullYear()
            );
        });
    };

    const previousMonth = () => {
        setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() - 1));
    };

    const nextMonth = () => {
        setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() + 1));
    };

    const goToToday = () => {
        setCurrentDate(new Date());
    };

    const isToday = (date: Date): boolean => {
        const today = new Date();
        return (
            date.getDate() === today.getDate() &&
            date.getMonth() === today.getMonth() &&
            date.getFullYear() === today.getFullYear()
        );
    };

    const formatMonthYear = (): string => {
        const months = [
            'Styczeń', 'Luty', 'Marzec', 'Kwiecień', 'Maj', 'Czerwiec',
            'Lipiec', 'Sierpień', 'Wrzesień', 'Październik', 'Listopad', 'Grudzień'
        ];
        return `${months[currentDate.getMonth()]} ${currentDate.getFullYear()}`;
    };

    const getTasksForSelectedDay = (): Task[] => {
        if (!selectedDay) return [];
        return getTasksForDate(selectedDay);
    };

    const getPriorityColor = (priority: string): string => {
        const colors: Record<string, string> = {
            LOW: 'bg-blue-500',
            MEDIUM: 'bg-yellow-500',
            HIGH: 'bg-orange-500',
            URGENT: 'bg-red-500',
        };
        return colors[priority] || 'bg-gray-500';
    };

    const getStatusColor = (status: string): string => {
        const colors: Record<string, string> = {
            NEW: 'bg-blue-500',
            IN_PROGRESS: 'bg-yellow-500',
            COMPLETED: 'bg-green-500',
            CANCELLED: 'bg-red-500',
        };
        return colors[status] || 'bg-gray-500';
    };

    if (loading) {
        return (
            <MainLayout>
                <div className="flex items-center justify-center h-96">
                    <div className="text-gray-400">Ładowanie kalendarza...</div>
                </div>
            </MainLayout>
        );
    }

    return (
        <MainLayout>
            <div className="max-w-7xl mx-auto">
                {/* Header */}
                <div className="bg-gray-900 rounded-lg p-6 mb-6">
                    <div className="flex items-center justify-between mb-4">
                        <h1 className="text-3xl font-bold text-white">📅 Kalendarz Zadań</h1>
                        <button
                            onClick={goToToday}
                            className="px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-lg transition-colors"
                        >
                            Dzisiaj
                        </button>
                    </div>

                    {/* Month Navigation */}
                    <div className="flex items-center justify-between">
                        <button
                            onClick={previousMonth}
                            className="p-2 hover:bg-gray-800 rounded-lg transition-colors"
                        >
                            <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
                            </svg>
                        </button>
                        <h2 className="text-2xl font-bold text-white">{formatMonthYear()}</h2>
                        <button
                            onClick={nextMonth}
                            className="p-2 hover:bg-gray-800 rounded-lg transition-colors"
                        >
                            <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                            </svg>
                        </button>
                    </div>
                </div>

                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                    {/* Calendar */}
                    <div className="lg:col-span-2 bg-gray-900 rounded-lg p-6">
                        {/* Days of week */}
                        <div className="grid grid-cols-7 gap-2 mb-2">
                            {['Pon', 'Wt', 'Śr', 'Czw', 'Pt', 'Sob', 'Nie'].map(day => (
                                <div key={day} className="text-center text-gray-400 font-semibold py-2">
                                    {day}
                                </div>
                            ))}
                        </div>

                        {/* Calendar days */}
                        <div className="grid grid-cols-7 gap-2">
                            {calendarDays.map((day, index) => {
                                const isSelected = selectedDay &&
                                    day.date.getDate() === selectedDay.getDate() &&
                                    day.date.getMonth() === selectedDay.getMonth() &&
                                    day.date.getFullYear() === selectedDay.getFullYear();

                                return (
                                    <button
                                        key={index}
                                        onClick={() => setSelectedDay(day.date)}
                                        className={`
                                            min-h-[100px] p-2 rounded-lg border-2 transition-all
                                            ${day.isCurrentMonth ? 'bg-gray-800' : 'bg-gray-900 opacity-50'}
                                            ${isToday(day.date) ? 'border-emerald-500' : 'border-gray-700'}
                                            ${isSelected ? 'ring-2 ring-emerald-500' : ''}
                                            hover:border-emerald-500
                                        `}
                                    >
                                        <div className={`
                                            text-sm font-semibold mb-1
                                            ${day.isCurrentMonth ? 'text-white' : 'text-gray-600'}
                                            ${isToday(day.date) ? 'text-emerald-500' : ''}
                                        `}>
                                            {day.date.getDate()}
                                        </div>

                                        {/* Task indicators */}
                                        <div className="space-y-1">
                                            {day.tasks.slice(0, 3).map((task, taskIndex) => (
                                                <div
                                                    key={taskIndex}
                                                    className={`
                                                        text-xs px-1 py-0.5 rounded truncate
                                                        ${getPriorityColor(task.priority)} text-white
                                                    `}
                                                    title={task.title}
                                                >
                                                    {task.title}
                                                </div>
                                            ))}
                                            {day.tasks.length > 3 && (
                                                <div className="text-xs text-gray-400">
                                                    +{day.tasks.length - 3} więcej
                                                </div>
                                            )}
                                        </div>
                                    </button>
                                );
                            })}
                        </div>
                    </div>

                    {/* Selected day tasks */}
                    <div className="bg-gray-900 rounded-lg p-6">
                        <h3 className="text-xl font-bold text-white mb-4">
                            {selectedDay ? (
                                <>
                                    Zadania na {selectedDay.getDate()}{' '}
                                    {selectedDay.toLocaleDateString('pl-PL', { month: 'long' })}
                                </>
                            ) : (
                                'Wybierz dzień'
                            )}
                        </h3>

                        {selectedDay ? (
                            <div className="space-y-3">
                                {getTasksForSelectedDay().length > 0 ? (
                                    getTasksForSelectedDay().map(task => (
                                        <div
                                            key={task.id}
                                            onClick={() => navigate(`/tasks/${task.id}`)}
                                            className="p-4 bg-gray-800 rounded-lg hover:bg-gray-750 cursor-pointer transition-colors"
                                        >
                                            <div className="flex items-start justify-between mb-2">
                                                <h4 className="font-semibold text-white">{task.title}</h4>
                                                <span className={`
                                                    px-2 py-1 rounded text-xs text-white
                                                    ${getStatusColor(task.status)}
                                                `}>
                                                    {task.status}
                                                </span>
                                            </div>

                                            {task.description && (
                                                <p className="text-sm text-gray-400 mb-2 line-clamp-2">
                                                    {task.description}
                                                </p>
                                            )}

                                            <div className="flex items-center gap-2">
                                                <span className={`
                                                    px-2 py-1 rounded text-xs text-white
                                                    ${getPriorityColor(task.priority)}
                                                `}>
                                                    {task.priority}
                                                </span>
                                                {task.project?.name && (
                                                    <span className="text-xs text-gray-400">
                                                        📁 {task.project.name}
                                                    </span>
                                                )}
                                            </div>
                                        </div>
                                    ))
                                ) : (
                                    <div className="text-center text-gray-400 py-8">
                                        Brak zadań na ten dzień
                                    </div>
                                )}
                            </div>
                        ) : (
                            <div className="text-center text-gray-400 py-8">
                                Kliknij na dzień w kalendarzu, aby zobaczyć zadania
                            </div>
                        )}
                    </div>
                </div>

                {/* Legend */}
                <div className="bg-gray-900 rounded-lg p-6 mt-6">
                    <h3 className="text-lg font-semibold text-white mb-3">Legenda:</h3>
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                        <div className="flex items-center gap-2">
                            <div className="w-4 h-4 bg-blue-500 rounded"></div>
                            <span className="text-gray-300">Niski priorytet</span>
                        </div>
                        <div className="flex items-center gap-2">
                            <div className="w-4 h-4 bg-yellow-500 rounded"></div>
                            <span className="text-gray-300">Średni priorytet</span>
                        </div>
                        <div className="flex items-center gap-2">
                            <div className="w-4 h-4 bg-orange-500 rounded"></div>
                            <span className="text-gray-300">Wysoki priorytet</span>
                        </div>
                        <div className="flex items-center gap-2">
                            <div className="w-4 h-4 bg-red-500 rounded"></div>
                            <span className="text-gray-300">Pilne</span>
                        </div>
                    </div>
                </div>
            </div>
        </MainLayout>
    );
};

export default CalendarPage;