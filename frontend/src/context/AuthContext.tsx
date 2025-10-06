import { createContext, useContext, useState, useEffect } from 'react';
import type { ReactNode } from 'react';import authService from '../services/authService';
import type { UserDto } from '../services/authService';

interface AuthContextType {
    user: UserDto | null;
    isAuthenticated: boolean;
    isLoading: boolean;
    login: (username: string, password: string) => Promise<void>;
    logout: () => Promise<void>;
    refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
    children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
    const [user, setUser] = useState<UserDto | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        // Load user from localStorage on mount
        const loadUser = () => {
            const savedUser = authService.getCurrentUser();
            if (savedUser) {
                setUser(savedUser);
            }
            setIsLoading(false);
        };

        loadUser();
    }, []);

    const login = async (username: string, password: string) => {
        const response = await authService.login({ username, password });
        if (response.data?.user) {
            setUser(response.data.user);
        }
    };

    const logout = async () => {
        await authService.logout();
        setUser(null);
    };

    const refreshUser = async () => {
        try {
            const freshUser = await authService.getProfile();
            setUser(freshUser);
        } catch (error) {
            console.error('Failed to refresh user:', error);
            // If refresh fails, clear user
            setUser(null);
        }
    };

    const value: AuthContextType = {
        user,
        isAuthenticated: !!user,
        isLoading,
        login,
        logout,
        refreshUser,
    };

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

// Custom hook to use auth context
export const useAuth = (): AuthContextType => {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};