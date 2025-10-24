// frontend/src/pages/LoginPage.tsx - FINAL FIX
import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import type { ApiError } from '../services/authService';

interface FormData {
    username: string;
    password: string;
}

interface FormErrors {
    username?: string;
    password?: string;
    general?: string;
}

const LoginPage: React.FC = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const { login, isAuthenticated } = useAuth(); // ✅ Używamy login z AuthContext!

    const [formData, setFormData] = useState<FormData>({
        username: '',
        password: '',
    });
    const [errors, setErrors] = useState<FormErrors>({});
    const [isLoading, setIsLoading] = useState(false);
    const [successMessage, setSuccessMessage] = useState<string | null>(null);

    // ✅ Sprawdzamy auth tylko raz przy montażu
    useEffect(() => {
        if (isAuthenticated) {
            navigate('/dashboard', { replace: true });
            return;
        }

        // Show success message from registration
        if (location.state?.message) {
            setSuccessMessage(location.state.message);
            window.history.replaceState({}, document.title);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []); // Pusty array - tylko przy montażu!

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
        if (errors[name as keyof FormErrors]) {
            setErrors(prev => ({ ...prev, [name]: undefined, general: undefined }));
        }
    };

    const validateForm = (): boolean => {
        const newErrors: FormErrors = {};
        if (!formData.username.trim()) newErrors.username = 'Nazwa użytkownika jest wymagana';
        if (!formData.password) newErrors.password = 'Hasło jest wymagane';
        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleSubmit = async () => {
        if (!validateForm()) return;

        setIsLoading(true);
        setErrors({});

        try {
            // ✅ Używamy login() z AuthContext - to zaktualizuje state!
            await login(formData.username, formData.password);

            console.log('Login successful!');

            // ✅ Nawigujemy DOPIERO po zaktualizowaniu Context
            navigate('/dashboard', { replace: true });

        } catch (error) {
            console.error('Login failed:', error);
            const apiError = error as ApiError;

            setErrors({
                general: apiError.message || 'Logowanie nie powiodło się. Sprawdź dane i spróbuj ponownie.',
            });
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900 flex items-center justify-center px-4 py-12">
            <div className="w-full max-w-md">
                {/* Logo */}
                <div className="text-center mb-8">
                    <h1 className="text-4xl font-bold mb-2">
                        <span className="text-emerald-500">TASK</span>
                        <span className="text-white">MANAGER</span>
                    </h1>
                    <p className="text-gray-400">Zaloguj się do swojego konta</p>
                </div>

                {/* Success Message */}
                {successMessage && (
                    <div className="mb-6 p-4 bg-emerald-500/10 border border-emerald-500/50 rounded-lg">
                        <p className="text-emerald-400 text-sm text-center">{successMessage}</p>
                    </div>
                )}

                {/* Login Form */}
                <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-8 border border-gray-700 shadow-2xl">
                    {/* General Error */}
                    {errors.general && (
                        <div className="mb-6 p-4 bg-red-500/10 border border-red-500/50 rounded-lg">
                            <p className="text-red-400 text-sm">{errors.general}</p>
                        </div>
                    )}

                    <form onSubmit={(e) => { e.preventDefault(); handleSubmit(); }} className="space-y-6">
                        {/* Username */}
                        <div>
                            <label className="block text-sm font-medium text-gray-300 mb-2">
                                Nazwa użytkownika
                            </label>
                            <input
                                type="text"
                                name="username"
                                value={formData.username}
                                onChange={handleChange}
                                disabled={isLoading}
                                className={`w-full px-4 py-3 bg-gray-900/50 border ${
                                    errors.username ? 'border-red-500' : 'border-gray-600'
                                } rounded-lg text-white placeholder-gray-400 
                                focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 
                                transition disabled:opacity-50 disabled:cursor-not-allowed`}
                                placeholder="Wprowadź nazwę użytkownika"
                                autoFocus
                            />
                            {errors.username && (
                                <p className="mt-2 text-sm text-red-400">{errors.username}</p>
                            )}
                        </div>

                        {/* Password */}
                        <div>
                            <label className="block text-sm font-medium text-gray-300 mb-2">
                                Hasło
                            </label>
                            <input
                                type="password"
                                name="password"
                                value={formData.password}
                                onChange={handleChange}
                                disabled={isLoading}
                                className={`w-full px-4 py-3 bg-gray-900/50 border ${
                                    errors.password ? 'border-red-500' : 'border-gray-600'
                                } rounded-lg text-white placeholder-gray-400 
                                focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 
                                transition disabled:opacity-50 disabled:cursor-not-allowed`}
                                placeholder="Wprowadź hasło"
                            />
                            {errors.password && (
                                <p className="mt-2 text-sm text-red-400">{errors.password}</p>
                            )}
                        </div>

                        {/* Submit Button */}
                        <button
                            type="submit"
                            disabled={isLoading}
                            className="w-full bg-gradient-to-r from-emerald-500 to-emerald-600
                            hover:from-emerald-600 hover:to-emerald-700 text-white font-semibold
                            py-3 px-4 rounded-lg transition duration-200
                            disabled:opacity-50 disabled:cursor-not-allowed
                            flex items-center justify-center gap-2"
                        >
                            {isLoading ? (
                                <>
                                    <svg className="animate-spin h-5 w-5" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                    </svg>
                                    <span>Logowanie...</span>
                                </>
                            ) : (
                                'Zaloguj się'
                            )}
                        </button>
                    </form>

                    {/* Register Link */}
                    <div className="mt-6 text-center">
                        <p className="text-gray-400 text-sm">
                            Nie masz konta?{' '}
                            <button
                                onClick={() => navigate('/register')}
                                disabled={isLoading}
                                className="text-emerald-400 hover:text-emerald-300 font-medium transition disabled:opacity-50"
                            >
                                Zarejestruj się
                            </button>
                        </p>
                    </div>
                </div>

                {/* Footer */}
                <div className="mt-8 text-center">
                    <p className="text-gray-500 text-sm">
                        TaskManager © 2024. Wszystkie prawa zastrzeżone.
                    </p>
                </div>
            </div>
        </div>
    );
};

export default LoginPage;