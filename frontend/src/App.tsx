// src/App.tsx
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import Dashboard from './pages/Dashboard';
import TeamDetailsPage from "./pages/TeamDetailsPage.tsx";
import TeamsPage from "./pages/TeamPage.tsx";
import ProjectsPage from "./pages/ProjectsPage.tsx";
import ProjectDetailsPage from "./pages/ProjectDetailsPage.tsx";
import TasksPage from "./pages/TasksPage.tsx";
import TaskDetailsPage from "./pages/TaskDetailsPage.tsx";
import CalendarPage from "./pages/CalendarPage.tsx";
import ReportsPage from "./pages/ReportsPage.tsx";
import ProfilePage from "./pages/ProfilePage.tsx";
import NotificationsPage from "./pages/NotificationsPage.tsx";
import AdminDashboardPage from "./pages/admin/AdminDashboardPage.tsx";
import AdminUsersPage from "./pages/admin/AdminUsersPage.tsx";
import AdminProjectsPage from "./pages/admin/AdminProjectsPage.tsx";
import AdminTasksPage from "./pages/admin/AdminTasksPage.tsx";

function App() {
    return (
        <AuthProvider>
            <Router>
                <Routes>
                    <Route path="/" element={<Navigate to="/dashboard" replace />} />
                    <Route path="/login" element={<LoginPage />} />
                    <Route path="/register" element={<RegisterPage />} />

                    {/* Protected Routes */}
                    <Route
                        path="/dashboard"
                        element={
                            <ProtectedRoute>
                                <Dashboard />
                            </ProtectedRoute>
                        }
                    />

                    {/* Projects Routes */}
                    <Route
                        path="/projects"
                        element={
                            <ProtectedRoute>
                                <ProjectsPage />
                            </ProtectedRoute>
                        }
                    />

                    <Route
                        path="/projects/:id"
                        element={
                            <ProtectedRoute>
                                <ProjectDetailsPage />
                            </ProtectedRoute>
                        }
                    />

                    {/* Tasks Routes */}
                    <Route
                        path="/tasks"
                        element={
                            <ProtectedRoute>
                                <TasksPage />
                            </ProtectedRoute>
                        }
                    />

                    <Route
                        path="/tasks/:id"
                        element={
                            <ProtectedRoute>
                                <TaskDetailsPage />
                            </ProtectedRoute>
                        }
                    />

                    {/* Teams Routes */}
                    <Route
                        path="/teams"
                        element={
                            <ProtectedRoute>
                                <TeamsPage />
                            </ProtectedRoute>
                        }
                    />

                    <Route
                        path="/teams/:id"
                        element={
                            <ProtectedRoute>
                                <TeamDetailsPage />
                            </ProtectedRoute>
                        }
                    />

                    {/* Calendar Route */}
                    <Route
                        path="/calendar"
                        element={
                            <ProtectedRoute>
                                <CalendarPage />
                            </ProtectedRoute>
                        }
                    />

                    {/* Reports Route */}
                    <Route
                        path="/reports"
                        element={
                            <ProtectedRoute>
                                <ReportsPage />
                            </ProtectedRoute>
                        }
                    />

                    {/* Profile Route */}
                    <Route
                        path="/profile"
                        element={
                            <ProtectedRoute>
                                <ProfilePage />
                            </ProtectedRoute>
                        }
                    />

                    {/* Notifications Route - NOWA TRASA */}
                    <Route
                        path="/notifications"
                        element={
                            <ProtectedRoute>
                                <NotificationsPage />
                            </ProtectedRoute>
                        }
                    />

                    {/* Admin Routes */}
                    <Route
                        path="/admin"
                        element={
                            <ProtectedRoute>
                                <AdminDashboardPage />
                            </ProtectedRoute>
                        }
                    />

                    <Route
                        path="/admin/users"
                        element={
                            <ProtectedRoute>
                                <AdminUsersPage />
                            </ProtectedRoute>
                        }
                    />

                    <Route
                        path="/admin/projects"
                        element={
                            <ProtectedRoute>
                                <AdminProjectsPage />
                            </ProtectedRoute>
                        }
                    />

                    <Route
                        path="/admin/tasks"
                        element={
                            <ProtectedRoute>
                                <AdminTasksPage />
                            </ProtectedRoute>
                        }
                    />

                    {/* 404 Route */}
                    <Route path="*" element={<Navigate to="/dashboard" replace />} />
                </Routes>
            </Router>
        </AuthProvider>
    );
}

export default App;