// frontend/src/components/MainLayout.tsx - ZMODYFIKOWANA WERSJA
import React from 'react';
import Navbar from './Navbar';
import Sidebar from './Sidebar';
import AIAssistant from './AIAssistant'; // ✅ DODAJ IMPORT

interface MainLayoutProps {
    children: React.ReactNode;
}

const MainLayout: React.FC<MainLayoutProps> = ({ children }) => {
    return (
        <div className="min-h-screen bg-gray-800">
            <Navbar />
            <div className="flex">
                <Sidebar />
                <main className="flex-1 p-6">
                    {children}
                </main>
            </div>

            {/* ✅ DODAJ KOMPONENT AI ASSISTANT */}
            <AIAssistant />
        </div>
    );
};

export default MainLayout;