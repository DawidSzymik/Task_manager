import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import authService, { type ApiError } from '../services/authService';

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
    const [formData, setFormData] = useState<FormData>({
        username: '',
        password: '',
    });
    const [errors, setErrors] = useState<FormErrors>({});
    const [isLoading, setIsLoading] = useState(false);
    const [successMessage, setSuccessMessage] = useState<string | null>(null);

    useEffect(() => {
        // Redirect if already logged in
        if (authService.isAuthenticated()) {
            navigate('/dashboard', { replace: true });
            return;
        }

        // Show success message from registration
        if (location.state?.message) {
            setSuccessMessage(location.state.message);
            // Clear the message from location state
            window.history.replaceState({}, document.title);
        }
    }, [location, navigate]);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
        // Clear errors when user types
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
            const response = await authService.login(formData);

            console.log('Login successful:', response);

            // Redirect to dashboard
            navigate('/dashboard');

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

    const handleKeyPress = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter') {
            handleSubmit();
        }
    };

    return (
        <div className="min-h-screen bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900 flex items-center justify-center p-4">
            <div className="max-w-md w-full">
                <div className="text-center mb-8">
                    <div className="inline-block bg-gray-800 p-4 rounded-2xl shadow-lg mb-4">
                        <div className="w-16 h-16 bg-emerald-500 rounded-xl flex items-center justify-center">
                            <span className="text-3xl font-bold text-white">TM</span>
                        </div>
                    </div>
                    <h1 className="text-3xl font-bold text-white mb-2">Witaj ponownie!</h1>
                    <p className="text-gray-400">Zaloguj się do swojego konta</p>
                </div>

                <div className="bg-gray-800 rounded-2xl shadow-2xl p-8 border border-gray-700">
                    {/* Success Message */}
                    {successMessage && (
                        <div className="mb-4 p-3 bg-emerald-500 bg-opacity-10 border border-emerald-500 rounded-lg">
                            <p className="text-emerald-500 text-sm">{successMessage}</p>
                        </div>
                    )}

                    {/* General Error Message */}
                    {errors.general && (
                        <div className="mb-4 p-3 bg-red-500 bg-opacity-10 border border-red-500 rounded-lg">
                            <p className="text-red-500 text-sm">{errors.general}</p>
                        </div>
                    )}

                    <div className="space-y-6">
                        <div>
                            <label htmlFor="username" className="block text-sm font-medium text-gray-300 mb-2">
                                Nazwa użytkownika
                            </label>
                            <input
                                type="text"
                                id="username"
                                name="username"
                                value={formData.username}
                                onChange={handleChange}
                                onKeyPress={handleKeyPress}
                                className={`w-full px-4 py-3 bg-gray-900 border ${
                                    errors.username ? 'border-red-500' : 'border-gray-700'
                                } rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent transition`}
                                placeholder="Wpisz swoją nazwę użytkownika"
                                disabled={isLoading}
                            />
                            {errors.username && <p className="mt-1 text-sm text-red-500">{errors.username}</p>}
                        </div>

                        <div>
                            <label htmlFor="password" className="block text-sm font-medium text-gray-300 mb-2">
                                Hasło
                            </label>
                            <input
                                type="password"
                                id="password"
                                name="password"
                                value={formData.password}
                                onChange={handleChange}
                                onKeyPress={handleKeyPress}
                                className={`w-full px-4 py-3 bg-gray-900 border ${
                                    errors.password ? 'border-red-500' : 'border-gray-700'
                                } rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent transition`}
                                placeholder="Wpisz swoje hasło"
                                disabled={isLoading}
                            />
                            {errors.password && <p className="mt-1 text-sm text-red-500">{errors.password}</p>}
                        </div>

                        <div className="flex items-center justify-between">
                            <label className="flex items-center">
                                <input
                                    type="checkbox"
                                    className="w-4 h-4 text-emerald-500 bg-gray-900 border-gray-700 rounded focus:ring-emerald-500 focus:ring-2"
                                    disabled={isLoading}
                                />
                                <span className="ml-2 text-sm text-gray-400">Zapamiętaj mnie</span>
                            </label>
                            <button
                                className="text-sm text-emerald-500 hover:text-emerald-400 transition"
                                disabled={isLoading}
                            >
                                Zapomniałeś hasła?
                            </button>
                        </div>

                        <button
                            onClick={handleSubmit}
                            disabled={isLoading}
                            className="w-full bg-emerald-500 hover:bg-emerald-600 text-white font-semibold py-3 px-4 rounded-lg transition duration-200 transform hover:scale-105 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:scale-100"
                        >
                            {isLoading ? (
                                <span className="flex items-center justify-center">
                  <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  Logowanie...
                </span>
                            ) : (
                                'Zaloguj się'
                            )}
                        </button>
                    </div>

                    <div className="mt-6 relative">
                        <div className="absolute inset-0 flex items-center">
                            <div className="w-full border-t border-gray-700"></div>
                        </div>
                        <div className="relative flex justify-center text-sm">
                            <span className="px-2 bg-gray-800 text-gray-400">lub kontynuuj z</span>
                        </div>
                    </div>

                    <div className="mt-6 grid grid-cols-2 gap-3">
                        <button
                            className="flex items-center justify-center px-4 py-2 border border-gray-700 rounded-lg hover:bg-gray-700 transition"
                            disabled={isLoading}
                        >
                            <svg className="w-5 h-5 mr-2" viewBox="0 0 24 24">
                                <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                                <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                                <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                                <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                            </svg>
                            <span className="text-sm text-gray-300">Google</span>
                        </button>
                        <button
                            className="flex items-center justify-center px-4 py-2 border border-gray-700 rounded-lg hover:bg-gray-700 transition"
                            disabled={isLoading}
                        >
                            <svg className="w-5 h-5 mr-2" fill="#1877F2" viewBox="0 0 24 24">
                                <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/>
                            </svg>
                            <span className="text-sm text-gray-300">Facebook</span>
                        </button>
                    </div>
                </div>

                <p className="mt-6 text-center text-gray-400">
                    Nie masz konta?{' '}
                    <button
                        onClick={() => navigate('/register')}
                        className="text-emerald-500 hover:text-emerald-400 font-semibold transition"
                        disabled={isLoading}
                    >
                        Zarejestruj się
                    </button>
                </p>
            </div>
        </div>
    );
};

export default LoginPage;