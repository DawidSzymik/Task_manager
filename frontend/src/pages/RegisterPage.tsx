// src/pages/RegisterPage.tsx
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import authService, { type ApiError } from '../services/authService';

interface FormData {
    username: string;
    email: string;
    password: string;
    confirmPassword: string;
    fullName: string;
}

interface FormErrors {
    username?: string;
    email?: string;
    password?: string;
    confirmPassword?: string;
    fullName?: string;
    general?: string;
}

const RegisterPage: React.FC = () => {
    const navigate = useNavigate();
    const [formData, setFormData] = useState<FormData>({
        username: '',
        email: '',
        password: '',
        confirmPassword: '',
        fullName: '',
    });
    const [errors, setErrors] = useState<FormErrors>({});
    const [isLoading, setIsLoading] = useState(false);
    const [acceptTerms, setAcceptTerms] = useState(false);

    useEffect(() => {
        if (authService.isAuthenticated()) {
            navigate('/dashboard', { replace: true });
        }
    }, [navigate]);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
        if (errors[name as keyof FormErrors]) {
            setErrors(prev => ({ ...prev, [name]: undefined, general: undefined }));
        }
    };

    const validateForm = (): boolean => {
        const newErrors: FormErrors = {};

        if (!formData.username.trim()) {
            newErrors.username = 'Nazwa użytkownika jest wymagana';
        } else if (formData.username.length < 3) {
            newErrors.username = 'Nazwa musi mieć minimum 3 znaki';
        }

        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!formData.email) {
            newErrors.email = 'Email jest wymagany';
        } else if (!emailRegex.test(formData.email)) {
            newErrors.email = 'Nieprawidłowy format email';
        }

        if (!formData.password) {
            newErrors.password = 'Hasło jest wymagane';
        } else if (formData.password.length < 6) {
            newErrors.password = 'Hasło musi mieć minimum 6 znaków';
        }

        if (formData.password !== formData.confirmPassword) {
            newErrors.confirmPassword = 'Hasła nie są identyczne';
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleSubmit = async () => {
        if (!validateForm()) return;

        if (!acceptTerms) {
            setErrors({ general: 'Musisz zaakceptować regulamin' });
            return;
        }

        setIsLoading(true);
        setErrors({});

        try {
            // IMPORTANT: Send confirmPassword to backend!
            const response = await authService.register(formData);

            console.log('Registration successful:', response);

            navigate('/login', {
                state: { message: 'Rejestracja zakończona pomyślnie! Możesz się teraz zalogować.' }
            });

        } catch (error) {
            console.error('Registration failed:', error);
            const apiError = error as ApiError;

            setErrors({
                general: apiError.message || 'Rejestracja nie powiodła się. Spróbuj ponownie.',
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
                    <h1 className="text-3xl font-bold text-white mb-2">Dołącz do nas!</h1>
                    <p className="text-gray-400">Stwórz darmowe konto już teraz</p>
                </div>

                <div className="bg-gray-800 rounded-2xl shadow-2xl p-8 border border-gray-700">
                    {errors.general && (
                        <div className="mb-4 p-3 bg-red-500 bg-opacity-10 border border-red-500 rounded-lg">
                            <p className="text-red-500 text-sm">{errors.general}</p>
                        </div>
                    )}

                    <div className="space-y-5">
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
                                placeholder="np. jankowalski"
                                disabled={isLoading}
                            />
                            {errors.username && <p className="mt-1 text-sm text-red-500">{errors.username}</p>}
                        </div>

                        <div>
                            <label htmlFor="fullName" className="block text-sm font-medium text-gray-300 mb-2">
                                Pełne imię i nazwisko <span className="text-gray-500">(opcjonalnie)</span>
                            </label>
                            <input
                                type="text"
                                id="fullName"
                                name="fullName"
                                value={formData.fullName}
                                onChange={handleChange}
                                onKeyPress={handleKeyPress}
                                className="w-full px-4 py-3 bg-gray-900 border border-gray-700 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent transition"
                                placeholder="Jan Kowalski"
                                disabled={isLoading}
                            />
                        </div>

                        <div>
                            <label htmlFor="email" className="block text-sm font-medium text-gray-300 mb-2">
                                Adres email
                            </label>
                            <input
                                type="email"
                                id="email"
                                name="email"
                                value={formData.email}
                                onChange={handleChange}
                                onKeyPress={handleKeyPress}
                                className={`w-full px-4 py-3 bg-gray-900 border ${
                                    errors.email ? 'border-red-500' : 'border-gray-700'
                                } rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent transition`}
                                placeholder="twoj@email.com"
                                disabled={isLoading}
                            />
                            {errors.email && <p className="mt-1 text-sm text-red-500">{errors.email}</p>}
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
                                placeholder="Minimum 6 znaków"
                                disabled={isLoading}
                            />
                            {errors.password && <p className="mt-1 text-sm text-red-500">{errors.password}</p>}
                        </div>

                        <div>
                            <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-300 mb-2">
                                Potwierdź hasło
                            </label>
                            <input
                                type="password"
                                id="confirmPassword"
                                name="confirmPassword"
                                value={formData.confirmPassword}
                                onChange={handleChange}
                                onKeyPress={handleKeyPress}
                                className={`w-full px-4 py-3 bg-gray-900 border ${
                                    errors.confirmPassword ? 'border-red-500' : 'border-gray-700'
                                } rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent transition`}
                                placeholder="Powtórz hasło"
                                disabled={isLoading}
                            />
                            {errors.confirmPassword && <p className="mt-1 text-sm text-red-500">{errors.confirmPassword}</p>}
                        </div>

                        <div className="flex items-start">
                            <input
                                type="checkbox"
                                id="terms"
                                checked={acceptTerms}
                                onChange={(e) => setAcceptTerms(e.target.checked)}
                                className="w-4 h-4 mt-1 text-emerald-500 bg-gray-900 border-gray-700 rounded focus:ring-emerald-500 focus:ring-2"
                                disabled={isLoading}
                            />
                            <label htmlFor="terms" className="ml-2 text-sm text-gray-400">
                                Akceptuję{' '}
                                <span className="text-emerald-500 hover:text-emerald-400 transition cursor-pointer">
                                    regulamin
                                </span>
                                {' '}i{' '}
                                <span className="text-emerald-500 hover:text-emerald-400 transition cursor-pointer">
                                    politykę prywatności
                                </span>
                            </label>
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
                                    Tworzenie konta...
                                </span>
                            ) : (
                                'Utwórz konto'
                            )}
                        </button>
                    </div>

                    <div className="mt-6 text-center">
                        <p className="text-gray-400 text-sm">
                            Masz już konto?{' '}
                            <button
                                onClick={() => navigate('/login')}
                                className="text-emerald-500 hover:text-emerald-400 font-semibold transition"
                                disabled={isLoading}
                            >
                                Zaloguj się
                            </button>
                        </p>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default RegisterPage;