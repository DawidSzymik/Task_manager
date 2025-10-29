// frontend/src/components/FilePreview/FilePreview.tsx - FIXED WORKER
import React, { useState } from 'react';
import { Document, Page, pdfjs } from 'react-pdf';
import 'react-pdf/dist/Page/AnnotationLayer.css';
import 'react-pdf/dist/Page/TextLayer.css';

// ⭐ POPRAWIONA Konfiguracja PDF.js worker - używamy lokalnego z node_modules
pdfjs.GlobalWorkerOptions.workerSrc = new URL(
    'pdfjs-dist/build/pdf.worker.min.mjs',
    import.meta.url
).toString();

interface FilePreviewProps {
    fileId: number;
    fileName: string;
    contentType: string;
    onClose: () => void;
}

const FilePreview: React.FC<FilePreviewProps> = ({ fileId, fileName, contentType, onClose }) => {
    const [numPages, setNumPages] = useState<number>(0);
    const [pageNumber, setPageNumber] = useState<number>(1);
    const [scale, setScale] = useState<number>(1.0);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    const previewUrl = `/api/v1/files/${fileId}/preview`;
    const downloadUrl = `/api/v1/files/${fileId}/download`;

    const onDocumentLoadSuccess = ({ numPages }: { numPages: number }) => {
        setNumPages(numPages);
        setLoading(false);
    };

    const onDocumentLoadError = (error: Error) => {
        console.error('Error loading PDF:', error);
        setError('Nie udało się załadować pliku PDF');
        setLoading(false);
    };

    const goToPrevPage = () => setPageNumber((prev) => Math.max(prev - 1, 1));
    const goToNextPage = () => setPageNumber((prev) => Math.min(prev + 1, numPages));
    const zoomIn = () => setScale((prev) => Math.min(prev + 0.25, 3.0));
    const zoomOut = () => setScale((prev) => Math.max(prev - 0.25, 0.5));
    const resetZoom = () => setScale(1.0);

    const renderPDFPreview = () => (
        <div className="flex flex-col h-full">
            {/* PDF Controls */}
            <div className="flex justify-between items-center bg-gray-800 rounded-lg p-4 mb-4 flex-wrap gap-4">
                {/* Navigation */}
                <div className="flex items-center gap-3">
                    <button
                        onClick={goToPrevPage}
                        disabled={pageNumber <= 1}
                        className="px-4 py-2 bg-emerald-500 hover:bg-emerald-600 disabled:bg-gray-600 disabled:cursor-not-allowed text-white rounded-lg transition-colors"
                    >
                        ← Poprzednia
                    </button>
                    <span className="text-gray-300 font-medium min-w-[120px] text-center">
                        Strona {pageNumber} z {numPages}
                    </span>
                    <button
                        onClick={goToNextPage}
                        disabled={pageNumber >= numPages}
                        className="px-4 py-2 bg-emerald-500 hover:bg-emerald-600 disabled:bg-gray-600 disabled:cursor-not-allowed text-white rounded-lg transition-colors"
                    >
                        Następna →
                    </button>
                </div>

                {/* Zoom */}
                <div className="flex items-center gap-3">
                    <button
                        onClick={zoomOut}
                        disabled={scale <= 0.5}
                        className="px-3 py-2 bg-gray-700 hover:bg-gray-600 disabled:bg-gray-800 disabled:cursor-not-allowed text-white rounded-lg transition-colors"
                    >
                        🔍−
                    </button>
                    <span className="text-gray-300 font-medium min-w-[60px] text-center">
                        {Math.round(scale * 100)}%
                    </span>
                    <button
                        onClick={zoomIn}
                        disabled={scale >= 3.0}
                        className="px-3 py-2 bg-gray-700 hover:bg-gray-600 disabled:bg-gray-800 disabled:cursor-not-allowed text-white rounded-lg transition-colors"
                    >
                        🔍+
                    </button>
                    <button
                        onClick={resetZoom}
                        className="px-3 py-2 bg-gray-700 hover:bg-gray-600 text-white rounded-lg transition-colors"
                    >
                        Reset
                    </button>
                </div>
            </div>

            {/* PDF Viewer */}
            <div className="flex-1 overflow-auto bg-gray-950 rounded-lg p-4 flex justify-center">
                <Document
                    file={previewUrl}
                    onLoadSuccess={onDocumentLoadSuccess}
                    onLoadError={onDocumentLoadError}
                    loading={<div className="text-gray-400 text-lg p-10">Ładowanie PDF...</div>}
                    className="flex flex-col items-center"
                >
                    <Page
                        pageNumber={pageNumber}
                        scale={scale}
                        renderTextLayer={true}
                        renderAnnotationLayer={true}
                        className="shadow-2xl mb-4"
                    />
                </Document>
            </div>
        </div>
    );

    const renderImagePreview = () => (
        <div className="flex items-center justify-center h-full p-4">
            <img
                src={previewUrl}
                alt={fileName}
                className="max-w-full max-h-full object-contain rounded-lg shadow-2xl"
            />
        </div>
    );

    const renderExcelPlaceholder = () => (
        <div className="flex items-center justify-center h-full">
            <div className="text-center">
                <span className="text-8xl block mb-6">📊</span>
                <h3 className="text-2xl font-bold text-emerald-500 mb-2">Podgląd plików Excel</h3>
                <p className="text-gray-400 mb-4">Funkcja w przygotowaniu</p>
                <p className="text-white font-semibold mb-6">{fileName}</p>
                <a
                    href={downloadUrl}
                    className="inline-block px-6 py-3 bg-emerald-500 hover:bg-emerald-600 text-white font-semibold rounded-lg transition-colors"
                    download
                >
                    ⬇ Pobierz plik Excel
                </a>
            </div>
        </div>
    );

    const renderUnsupportedPreview = () => (
        <div className="flex items-center justify-center h-full">
            <div className="text-center">
                <span className="text-8xl block mb-6">📄</span>
                <h3 className="text-2xl font-bold text-emerald-500 mb-2">Podgląd niedostępny</h3>
                <p className="text-white font-semibold mb-6">{fileName}</p>
                <a
                    href={downloadUrl}
                    className="inline-block px-6 py-3 bg-emerald-500 hover:bg-emerald-600 text-white font-semibold rounded-lg transition-colors"
                    download
                >
                    ⬇ Pobierz plik
                </a>
            </div>
        </div>
    );

    const renderContent = () => {
        if (error) {
            return (
                <div className="flex items-center justify-center h-full">
                    <div className="text-center">
                        <p className="text-red-400 mb-4">{error}</p>
                        <a
                            href={downloadUrl}
                            className="inline-block px-6 py-3 bg-emerald-500 hover:bg-emerald-600 text-white font-semibold rounded-lg transition-colors"
                            download
                        >
                            ⬇ Pobierz plik
                        </a>
                    </div>
                </div>
            );
        }

        if (contentType === 'application/pdf') {
            return renderPDFPreview();
        }

        if (contentType.startsWith('image/')) {
            return renderImagePreview();
        }

        if (
            contentType === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' ||
            contentType === 'application/vnd.ms-excel'
        ) {
            return renderExcelPlaceholder();
        }

        return renderUnsupportedPreview();
    };

    return (
        <div
            className="fixed inset-0 bg-black bg-opacity-90 flex items-center justify-center z-50 p-5"
            onClick={onClose}
        >
            <div
                className="bg-gray-900 rounded-xl w-full h-full max-w-[95vw] max-h-[95vh] flex flex-col shadow-2xl"
                onClick={(e) => e.stopPropagation()}
            >
                {/* Header */}
                <div className="flex justify-between items-center p-5 border-b-2 border-gray-700 bg-gray-800 rounded-t-xl">
                    <h2 className="text-emerald-500 font-semibold text-lg truncate max-w-[60%]">
                        {fileName}
                    </h2>
                    <div className="flex gap-3">
                        <a
                            href={downloadUrl}
                            className="px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white font-medium rounded-lg transition-colors"
                            download
                        >
                            ⬇ Pobierz
                        </a>
                        <button
                            className="px-4 py-2 bg-red-500 hover:bg-red-600 text-white font-bold rounded-lg transition-colors text-lg"
                            onClick={onClose}
                        >
                            ✕
                        </button>
                    </div>
                </div>

                {/* Content */}
                <div className="flex-1 overflow-hidden p-5">
                    {loading && contentType === 'application/pdf' && (
                        <div className="flex items-center justify-center h-full">
                            <div className="text-gray-400 text-lg">Ładowanie PDF...</div>
                        </div>
                    )}
                    {renderContent()}
                </div>
            </div>
        </div>
    );
};

export default FilePreview;