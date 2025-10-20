// src/App.tsx
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import Dashboard from './pages/Dashboard';
import TeamDetailsPage from "./pages/TeamDetailsPage.tsx";
import TeamsPage from "./pages/TeamPage.tsx";

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

                    {/* Placeholder routes - będziemy je dodawać w kolejnych krokach */}
                    <Route
                        path="/projects"
                        element={
                            <ProtectedRoute>
                                <div className="min-h-screen bg-gray-800 flex items-center justify-center">
                                    <div className="text-center">
                                        <h1 className="text-3xl font-bold text-white mb-4">Projekty</h1>
                                        <p className="text-gray-400">Strona w budowie - będzie dostępna wkrótce!</p>
                                    </div>
                                </div>
                            </ProtectedRoute>
                        }
                    />

                    <Route
                        path="/tasks"
                        element={
                            <ProtectedRoute>
                                <div className="min-h-screen bg-gray-800 flex items-center justify-center">
                                    <div className="text-center">
                                        <h1 className="text-3xl font-bold text-white mb-4">Zadania</h1>
                                        <p className="text-gray-400">Strona w budowie - będzie dostępna wkrótce!</p>
                                    </div>
                                </div>
                            </ProtectedRoute>
                        }
                    />

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
                </Routes>
            </Router>
        </AuthProvider>
    );
}

export default App;