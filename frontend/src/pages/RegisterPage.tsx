import React, { useState } from 'react';

interface FormData {
    username: string;
    email: string;
    password: string;
    confirmPassword?: string;
}

interface FormErrors {
    username?: string;
    email?: string;
    password?: string;
    confirmPassword?: string;
}

const LoginPage: React.FC<{ onToggle: () => void }> = ({ onToggle }) => {
    const [formData, setFormData] = useState<FormData>({
        username: '',
        email: '',
        password: '',
    });
    const [errors, setErrors] = useState<FormErrors>({});
    const [isLoading, setIsLoading] = useState(false);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
        if (errors[name as keyof FormErrors]) {
            setErrors(prev => ({ ...prev, [name]: undefined }));
        }
    };

    const validateForm = (): boolean => {
        const newErrors: FormErrors = {};
        if (!formData.username.trim()) newErrors.username = 'Nazwa użytkownika jest wymagana';
        if (!formData.password) newErrors.password = 'Hasło jest wymagane';
        else if (formData.password.length < 6) newErrors.password = 'Hasło musi mieć minimum 6 znaków';
        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleSubmit = () => {
        if (!validateForm()) return;
        setIsLoading(true);
        setTimeout(() => {
            console.log('Login:', formData);
            alert('Logowanie powiodło się! (Podłącz swoje API)');
            setIsLoading(false);
        }, 1500);
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
                                className={`w-full px-4 py-3 bg-gray-900 border ${
                                    errors.username ? 'border-red-500' : 'border-gray-700'
                                } rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent transition`}
                                placeholder="Wpisz swoją nazwę użytkownika"
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
                                className={`w-full px-4 py-3 bg-gray-900 border ${
                                    errors.password ? 'border-red-500' : 'border-gray-700'
                                } rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent transition`}
                                placeholder="Wpisz swoje hasło"
                            />
                            {errors.password && <p className="mt-1 text-sm text-red-500">{errors.password}</p>}
                        </div>

                        <div className="flex items-center justify-between">
                            <label className="flex items-center">
                                <input
                                    type="checkbox"
                                    className="w-4 h-4 text-emerald-500 bg-gray-900 border-gray-700 rounded focus:ring-emerald-500 focus:ring-2"
                                />
                                <span className="ml-2 text-sm text-gray-400">Zapamiętaj mnie</span>
                            </label>
                            <button className="text-sm text-emerald-500 hover:text-emerald-400 transition">
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
                        <button className="flex items-center justify-center px-4 py-2 border border-gray-700 rounded-lg hover:bg-gray-700 transition">
                            <svg className="w-5 h-5 mr-2" viewBox="0 0 24 24">
                                <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                                <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                                <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                                <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                            </svg>
                            <span className="text-sm text-gray-300">Google</span>
                        </button>
                        <button className="flex items-center justify-center px-4 py-2 border border-gray-700 rounded-lg hover:bg-gray-700 transition">
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
                        onClick={onToggle}
                        className="text-emerald-500 hover:text-emerald-400 font-semibold transition"
                    >
                        Zarejestruj się
                    </button>
                </p>
            </div>
        </div>
    );
};

const RegisterPage: React.FC<{ onToggle: () => void }> = ({ onToggle }) => {
    const [formData, setFormData] = useState<FormData>({
        username: '',
        email: '',
        password: '',
        confirmPassword: '',
    });
    const [errors, setErrors] = useState<FormErrors>({});
    const [isLoading, setIsLoading] = useState(false);
    const [acceptTerms, setAcceptTerms] = useState(false);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
        if (errors[name as keyof FormErrors]) {
            setErrors(prev => ({ ...prev, [name]: undefined }));
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
        } else if (formData.password.length < 8) {
            newErrors.password = 'Hasło musi mieć minimum 8 znaków';
        }

        if (formData.password !== formData.confirmPassword) {
            newErrors.confirmPassword = 'Hasła nie są identyczne';
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleSubmit = () => {
        if (!validateForm()) return;
        if (!acceptTerms) {
            alert('Musisz zaakceptować regulamin');
            return;
        }

        setIsLoading(true);
        setTimeout(() => {
            console.log('Register:', formData);
            alert('Rejestracja powiodła się! (Podłącz swoje API)');
            setIsLoading(false);
        }, 1500);
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
                    <div className="space-y-5">
                        <div>
                            <label htmlFor="reg-username" className="block text-sm font-medium text-gray-300 mb-2">
                                Nazwa użytkownika
                            </label>
                            <input
                                type="text"
                                id="reg-username"
                                name="username"
                                value={formData.username}
                                onChange={handleChange}
                                className={`w-full px-4 py-3 bg-gray-900 border ${
                                    errors.username ? 'border-red-500' : 'border-gray-700'
                                } rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent transition`}
                                placeholder="Wybierz nazwę użytkownika"
                            />
                            {errors.username && <p className="mt-1 text-sm text-red-500">{errors.username}</p>}
                        </div>

                        <div>
                            <label htmlFor="reg-email" className="block text-sm font-medium text-gray-300 mb-2">
                                Adres email
                            </label>
                            <input
                                type="email"
                                id="reg-email"
                                name="email"
                                value={formData.email}
                                onChange={handleChange}
                                className={`w-full px-4 py-3 bg-gray-900 border ${
                                    errors.email ? 'border-red-500' : 'border-gray-700'
                                } rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent transition`}
                                placeholder="twoj@email.com"
                            />
                            {errors.email && <p className="mt-1 text-sm text-red-500">{errors.email}</p>}
                        </div>

                        <div>
                            <label htmlFor="reg-password" className="block text-sm font-medium text-gray-300 mb-2">
                                Hasło
                            </label>
                            <input
                                type="password"
                                id="reg-password"
                                name="password"
                                value={formData.password}
                                onChange={handleChange}
                                className={`w-full px-4 py-3 bg-gray-900 border ${
                                    errors.password ? 'border-red-500' : 'border-gray-700'
                                } rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent transition`}
                                placeholder="Minimum 8 znaków"
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
                                className={`w-full px-4 py-3 bg-gray-900 border ${
                                    errors.confirmPassword ? 'border-red-500' : 'border-gray-700'
                                } rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent transition`}
                                placeholder="Powtórz hasło"
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
                            />
                            <label htmlFor="terms" className="ml-2 text-sm text-gray-400">
                                Akceptuję{' '}
                                <button className="text-emerald-500 hover:text-emerald-400 transition">
                                    regulamin
                                </button>
                                {' '}i{' '}
                                <button className="text-emerald-500 hover:text-emerald-400 transition">
                                    politykę prywatności
                                </button>
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
                  Rejestracja...
                </span>
                            ) : (
                                'Utwórz konto'
                            )}
                        </button>
                    </div>

                    <div className="mt-6 relative">
                        <div className="absolute inset-0 flex items-center">
                            <div className="w-full border-t border-gray-700"></div>
                        </div>
                        <div className="relative flex justify-center text-sm">
                            <span className="px-2 bg-gray-800 text-gray-400">lub zarejestruj się z</span>
                        </div>
                    </div>

                    <div className="mt-6 grid grid-cols-2 gap-3">
                        <button className="flex items-center justify-center px-4 py-2 border border-gray-700 rounded-lg hover:bg-gray-700 transition">
                            <svg className="w-5 h-5 mr-2" viewBox="0 0 24 24">
                                <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                                <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                                <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                                <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                            </svg>
                            <span className="text-sm text-gray-300">Google</span>
                        </button>
                        <button className="flex items-center justify-center px-4 py-2 border border-gray-700 rounded-lg hover:bg-gray-700 transition">
                            <svg className="w-5 h-5 mr-2" fill="#1877F2" viewBox="0 0 24 24">
                                <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/>
                            </svg>
                            <span className="text-sm text-gray-300">Facebook</span>
                        </button>
                    </div>
                </div>

                <p className="mt-6 text-center text-gray-400">
                    Masz już konto?{' '}
                    <button
                        onClick={onToggle}
                        className="text-emerald-500 hover:text-emerald-400 font-semibold transition"
                    >
                        Zaloguj się
                    </button>
                </p>
            </div>
        </div>
    );
};

const App: React.FC = () => {
    const [isLogin, setIsLogin] = useState(true);

    return isLogin ? (
        <LoginPage onToggle={() => setIsLogin(false)} />
    ) : (
        <RegisterPage onToggle={() => setIsLogin(true)} />
    );
};

export default App;