// frontend/src/pages/RegisterPage.tsx
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

    // ✅ FIX: Uruchamiamy tylko RAZ przy montażu
    useEffect(() => {
        if (authService.isAuthenticated()) {
            navigate('/dashboard', { replace: true });
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []); // ✅ Pusty array - uruchom tylko raz przy montażu!

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
            await authService.register({
                username: formData.username,
                email: formData.email,
                password: formData.password,
                confirmPassword: formData.confirmPassword,
                fullName: formData.fullName,
            });

            // Navigate to login with success message
            navigate('/login', {
                state: { message: 'Rejestracja zakończona sukcesem! Możesz się teraz zalogować.' },
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

    return (
        <div className="min-h-screen bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900 flex items-center justify-center px-4 py-12">
            <div className="w-full max-w-md">
                {/* Logo */}
                <div className="text-center mb-8">
                    <h1 className="text-4xl font-bold mb-2">
                        <span className="text-emerald-500">TASK</span>
                        <span className="text-white">MANAGER</span>
                    </h1>
                    <p className="text-gray-400">Utwórz nowe konto</p>
                </div>

                {/* Register Form */}
                <div className="bg-gray-800/50 backdrop-blur-sm rounded-xl p-8 border border-gray-700 shadow-2xl">
                    {/* General Error */}
                    {errors.general && (
                        <div className="mb-6 p-4 bg-red-500/10 border border-red-500/50 rounded-lg">
                            <p className="text-red-400 text-sm">{errors.general}</p>
                        </div>
                    )}

                    <form onSubmit={(e) => { e.preventDefault(); handleSubmit(); }} className="space-y-5">
                        {/* Full Name */}
                        <div>
                            <label className="block text-sm font-medium text-gray-300 mb-2">
                                Imię i nazwisko
                            </label>
                            <input
                                type="text"
                                name="fullName"
                                value={formData.fullName}
                                onChange={handleChange}
                                disabled={isLoading}
                                className={`w-full px-4 py-3 bg-gray-900/50 border ${
                                    errors.fullName ? 'border-red-500' : 'border-gray-600'
                                } rounded-lg text-white placeholder-gray-400 
                                focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 
                                transition disabled:opacity-50`}
                                placeholder="Jan Kowalski"
                                autoFocus
                            />
                            {errors.fullName && (
                                <p className="mt-2 text-sm text-red-400">{errors.fullName}</p>
                            )}
                        </div>

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
                                transition disabled:opacity-50`}
                                placeholder="jankowalski"
                            />
                            {errors.username && (
                                <p className="mt-2 text-sm text-red-400">{errors.username}</p>
                            )}
                        </div>

                        {/* Email */}
                        <div>
                            <label className="block text-sm font-medium text-gray-300 mb-2">
                                Email
                            </label>
                            <input
                                type="email"
                                name="email"
                                value={formData.email}
                                onChange={handleChange}
                                disabled={isLoading}
                                className={`w-full px-4 py-3 bg-gray-900/50 border ${
                                    errors.email ? 'border-red-500' : 'border-gray-600'
                                } rounded-lg text-white placeholder-gray-400 
                                focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 
                                transition disabled:opacity-50`}
                                placeholder="jan@przykład.pl"
                            />
                            {errors.email && (
                                <p className="mt-2 text-sm text-red-400">{errors.email}</p>
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
                                transition disabled:opacity-50`}
                                placeholder="Minimum 6 znaków"
                            />
                            {errors.password && (
                                <p className="mt-2 text-sm text-red-400">{errors.password}</p>
                            )}
                        </div>

                        {/* Confirm Password */}
                        <div>
                            <label className="block text-sm font-medium text-gray-300 mb-2">
                                Potwierdź hasło
                            </label>
                            <input
                                type="password"
                                name="confirmPassword"
                                value={formData.confirmPassword}
                                onChange={handleChange}
                                disabled={isLoading}
                                className={`w-full px-4 py-3 bg-gray-900/50 border ${
                                    errors.confirmPassword ? 'border-red-500' : 'border-gray-600'
                                } rounded-lg text-white placeholder-gray-400 
                                focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 
                                transition disabled:opacity-50`}
                                placeholder="Powtórz hasło"
                            />
                            {errors.confirmPassword && (
                                <p className="mt-2 text-sm text-red-400">{errors.confirmPassword}</p>
                            )}
                        </div>

                        {/* Terms Checkbox */}
                        <div className="flex items-start">
                            <input
                                type="checkbox"
                                id="acceptTerms"
                                checked={acceptTerms}
                                onChange={(e) => setAcceptTerms(e.target.checked)}
                                disabled={isLoading}
                                className="mt-1 w-4 h-4 text-emerald-500 bg-gray-900 border-gray-600
                                rounded focus:ring-emerald-500 focus:ring-2 disabled:opacity-50"
                            />
                            <label htmlFor="acceptTerms" className="ml-2 text-sm text-gray-400">
                                Akceptuję{' '}
                                <a href="#" className="text-emerald-400 hover:text-emerald-300">
                                    regulamin
                                </a>
                                {' '}i{' '}
                                <a href="#" className="text-emerald-400 hover:text-emerald-300">
                                    politykę prywatności
                                </a>
                            </label>
                        </div>

                        {/* Submit Button */}
                        <button
                            type="submit"
                            disabled={isLoading || !acceptTerms}
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
                                    <span>Rejestracja...</span>
                                </>
                            ) : (
                                'Zarejestruj się'
                            )}
                        </button>
                    </form>

                    {/* Login Link */}
                    <div className="mt-6 text-center">
                        <p className="text-gray-400 text-sm">
                            Masz już konto?{' '}
                            <button
                                onClick={() => navigate('/login')}
                                disabled={isLoading}
                                className="text-emerald-400 hover:text-emerald-300 font-medium transition disabled:opacity-50"
                            >
                                Zaloguj się
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

export default RegisterPage;