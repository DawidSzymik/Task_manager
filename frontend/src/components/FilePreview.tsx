import React, { useState, useEffect } from 'react';
import * as XLSX from 'xlsx';
import fileService from "../services/fileService.ts";

interface FilePreviewProps {
    fileId: number;
    fileName: string;
    contentType: string;
    onClose: () => void;
}

interface ExcelSheet {
    name: string;
    data: any[][];
}

const FilePreview: React.FC<FilePreviewProps> = ({ fileId, fileName, contentType, onClose }) => {
    const [excelSheets, setExcelSheets] = useState<ExcelSheet[]>([]);
    const [currentSheetIndex, setCurrentSheetIndex] = useState<number>(0);
    const [excelLoading, setExcelLoading] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);

    const previewUrl = fileService.getPreviewUrl(fileId);
    const downloadUrl = fileService.getDownloadUrl(fileId);

    const isExcelFile = contentType === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' || contentType === 'application/vnd.ms-excel';

    useEffect(() => {
        if (isExcelFile) {
            loadExcelFile();
        }
    }, [fileId, isExcelFile]);

    const loadExcelFile = async () => {
        try {
            setExcelLoading(true);
            setError(null);
            const response = await fetch(previewUrl, { credentials: 'include' });
            if (!response.ok) throw new Error('Nie udało się załadować pliku Excel');
            const arrayBuffer = await response.arrayBuffer();
            const workbook = XLSX.read(arrayBuffer, { type: 'array' });
            const sheets: ExcelSheet[] = workbook.SheetNames.map(sheetName => {
                const worksheet = workbook.Sheets[sheetName];
                const data = XLSX.utils.sheet_to_json(worksheet, { header: 1, defval: '' });
                return { name: sheetName, data: data as any[][] };
            });
            setExcelSheets(sheets);
            setExcelLoading(false);
        } catch (err) {
            console.error('Error loading Excel file:', err);
            setError('Nie udało się załadować pliku Excel');
            setExcelLoading(false);
        }
    };

    const goToPrevSheet = () => setCurrentSheetIndex(prev => Math.max(prev - 1, 0));
    const goToNextSheet = () => setCurrentSheetIndex(prev => Math.min(prev + 1, excelSheets.length - 1));

    const getColumnName = (index: number): string => {
        let name = '';
        let num = index;
        while (num >= 0) {
            name = String.fromCharCode(65 + (num % 26)) + name;
            num = Math.floor(num / 26) - 1;
        }
        return name;
    };

    if (error) {
        return (
            <div className="fixed inset-0 bg-black bg-opacity-90 flex items-center justify-center z-50 p-5" onClick={onClose}>
                <div className="bg-gray-900 rounded-xl p-8 max-w-md" onClick={e => e.stopPropagation()}>
                    <p className="text-red-400 mb-4">{error}</p>
                    <div className="flex gap-3">
                        <a href={downloadUrl} className="px-6 py-3 bg-primary-500 hover:bg-primary-600 text-white rounded-lg" download>Pobierz</a>
                        <button onClick={onClose} className="px-6 py-3 bg-red-500 hover:bg-red-600 text-white rounded-lg">Zamknij</button>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="fixed inset-0 bg-black bg-opacity-90 flex items-center justify-center z-50 p-5" onClick={onClose}>
            <div className="bg-gray-900 rounded-xl w-full h-full max-w-[95vw] max-h-[95vh] flex flex-col shadow-2xl" onClick={e => e.stopPropagation()}>
                <div className="flex justify-between items-center p-5 border-b-2 border-gray-700 bg-gray-800 rounded-t-xl">
                    <h2 className="text-primary-500 font-semibold text-lg truncate max-w-[60%]">{fileName}</h2>
                    <div className="flex gap-3">
                        <a href={downloadUrl} className="px-4 py-2 bg-primary-500 hover:bg-primary-600 text-white font-medium rounded-lg" download>⬇ Pobierz</a>
                        <button onClick={onClose} className="px-4 py-2 bg-red-500 hover:bg-red-600 text-white font-bold rounded-lg text-lg">✕</button>
                    </div>
                </div>
                <div className="flex-1 overflow-hidden p-5">
                    {/* ✅ SEKCJA PDF - PROSTY IFRAME */}
                    {contentType === 'application/pdf' && (
                        <div className="flex flex-col h-full">
                            <div className="flex-1 overflow-hidden bg-gray-950 rounded-lg">
                                <iframe
                                    src={previewUrl}
                                    className="w-full h-full border-0"
                                    title={fileName}
                                    onLoad={() => console.log('✅ PDF loaded in iframe!')}
                                    onError={() => setError('Nie udało się załadować PDF')}
                                />
                            </div>
                        </div>
                    )}

                    {/* SEKCJA OBRAZY */}
                    {contentType.startsWith('image/') && (
                        <div className="flex items-center justify-center h-full p-4">
                            <img
                                src={previewUrl}
                                alt={fileName}
                                className="max-w-full max-h-full object-contain rounded-lg shadow-2xl"
                            />
                        </div>
                    )}

                    {/* SEKCJA EXCEL */}
                    {isExcelFile && !excelLoading && excelSheets.length > 0 && (
                        <div className="flex flex-col h-full">
                            <div className="flex justify-between items-center bg-gray-800 rounded-lg p-4 mb-4 gap-4">
                                {excelSheets.length > 1 && (
                                    <div className="flex items-center gap-3 bg-gray-700 rounded-lg px-4 py-2">
                                        <button onClick={goToPrevSheet} disabled={currentSheetIndex <= 0} className="px-3 py-1 bg-primary-500 hover:bg-primary-600 disabled:bg-gray-600 text-white rounded">◀</button>
                                        <span className="text-white font-medium">{excelSheets[currentSheetIndex].name} ({currentSheetIndex + 1} z {excelSheets.length})</span>
                                        <button onClick={goToNextSheet} disabled={currentSheetIndex >= excelSheets.length - 1} className="px-3 py-1 bg-primary-500 hover:bg-primary-600 disabled:bg-gray-600 text-white rounded">▶</button>
                                    </div>
                                )}
                                <div className="text-white text-sm bg-gray-700 rounded-lg px-4 py-2">Wierszy: {excelSheets[currentSheetIndex].data.length}</div>
                            </div>
                            <div className="flex-1 overflow-auto bg-white rounded-lg">
                                <table className="min-w-full border-collapse">
                                    <thead className="sticky top-0 z-10">
                                    <tr>
                                        <th className="bg-gray-200 border border-gray-400 px-3 py-2 text-xs font-semibold text-gray-700 text-center min-w-[50px]">#</th>
                                        {excelSheets[currentSheetIndex].data[0] && Array.isArray(excelSheets[currentSheetIndex].data[0]) && excelSheets[currentSheetIndex].data[0].map((_, colIndex) => (
                                            <th key={colIndex} className="bg-gray-200 border border-gray-400 px-3 py-2 text-xs font-semibold text-gray-700 text-center min-w-[100px]">
                                                {getColumnName(colIndex)}
                                            </th>
                                        ))}
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {excelSheets[currentSheetIndex].data.map((row, rowIndex) => (
                                        <tr key={rowIndex} className="hover:bg-blue-50">
                                            <td className="bg-gray-100 border border-gray-300 px-3 py-2 text-xs font-semibold text-gray-600 text-center">
                                                {rowIndex + 1}
                                            </td>
                                            {Array.isArray(row) && row.map((cell, cellIndex) => (
                                                <td key={cellIndex} className="bg-white border border-gray-300 px-3 py-2 text-sm text-gray-900">
                                                    {cell != null && cell !== '' ? String(cell) : ''}
                                                </td>
                                            ))}
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    )}

                    {isExcelFile && excelLoading && (
                        <div className="flex items-center justify-center h-full">
                            <div className="text-gray-400 text-lg">Ładowanie Excel...</div>
                        </div>
                    )}

                    {/* BRAK PODGLĄDU */}
                    {!contentType.startsWith('image/') && contentType !== 'application/pdf' && !isExcelFile && (
                        <div className="flex items-center justify-center h-full">
                            <div className="text-center">
                                <span className="text-8xl block mb-6">📄</span>
                                <h3 className="text-2xl font-bold text-primary-500 mb-2">Podgląd niedostępny</h3>
                                <p className="text-white font-semibold mb-6">{fileName}</p>
                                <a href={downloadUrl} className="inline-block px-6 py-3 bg-primary-500 hover:bg-primary-600 text-white font-semibold rounded-lg" download>⬇ Pobierz plik</a>
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default FilePreview;
