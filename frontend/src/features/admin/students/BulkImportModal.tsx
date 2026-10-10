import React, { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { Upload, Download, CheckCircle, AlertCircle, FileText, AlertTriangle } from 'lucide-react';
import apiClient from '../../../lib/api/client';
import { API } from '../../../lib/api/endpoints';
import { BulkImportResponse } from '../../../types/student';
import { ApiResponse } from '../../../types/api';
import { Modal } from '../../../components/ui/Modal';
import { Button } from '../../../components/ui/Button';

interface BulkImportModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export function BulkImportModal({ isOpen, onClose, onSuccess }: BulkImportModalProps) {
  const qc = useQueryClient();
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [result, setResult] = useState<BulkImportResponse | null>(null);

  const downloadTemplate = () => {
    window.open(`${import.meta.env.VITE_API_BASE_URL || '/api'}${API.admin.students.template}`, '_blank');
  };

  const importMutation = useMutation({
    mutationFn: async (file: File) => {
      const formData = new FormData();
      formData.append('file', file);
      const res = await apiClient.post<ApiResponse<BulkImportResponse>>(API.admin.students.import, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      return res.data.data;
    },
    onSuccess: (data) => {
      setResult(data);
      qc.invalidateQueries({ queryKey: ['admin-students'] });
      if (data.successfulCount > 0) {
        toast.success(`Successfully imported ${data.successfulCount} student(s)!`);
        onSuccess();
      }
      if (data.failedCount > 0) {
        toast.error(`${data.failedCount} row(s) had errors and were skipped.`);
      }
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Failed to process CSV import.');
    },
  });

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      if (!file.name.toLowerCase().endsWith('.csv')) {
        toast.error('Please select a valid .csv file');
        return;
      }
      setSelectedFile(file);
      setResult(null);
    }
  };

  const handleUpload = () => {
    if (!selectedFile) {
      toast.error('Please select a CSV file first');
      return;
    }
    importMutation.mutate(selectedFile);
  };

  const handleReset = () => {
    setSelectedFile(null);
    setResult(null);
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Bulk Import Students (CSV)"
      maxWidth="lg"
      footer={
        <div className="flex items-center justify-between w-full">
          <Button variant="outline" size="sm" onClick={downloadTemplate} leftIcon={<Download className="w-3.5 h-3.5" />}>
            Sample CSV Template
          </Button>
          <div className="flex gap-2">
            <Button variant="secondary" size="sm" onClick={onClose}>
              {result ? 'Done' : 'Cancel'}
            </Button>
            {!result && (
              <Button
                size="sm"
                isLoading={importMutation.isPending}
                disabled={!selectedFile}
                onClick={handleUpload}
                leftIcon={<Upload className="w-3.5 h-3.5" />}
              >
                Upload & Process
              </Button>
            )}
          </div>
        </div>
      }
    >
      <div className="space-y-4">
        {/* Instructions banner */}
        <div className="rounded-lg border border-indigo-100 bg-indigo-50/70 p-3 text-xs text-indigo-900 space-y-1">
          <p className="font-semibold flex items-center gap-1.5 text-indigo-950">
            <FileText className="w-4 h-4 text-indigo-600 shrink-0" />
            CSV Import Guidelines
          </p>
          <ul className="list-disc list-inside space-y-0.5 text-indigo-800 ml-1">
            <li>CSV file must contain: <strong>Name, Email, Branch</strong>.</li>
            <li>Optional columns: <strong>Phone, GraduationYear, CGPA, Backlogs, Skills, Password</strong>.</li>
            <li>Duplicate emails and invalid rows will be skipped and reported below without failing the entire batch.</li>
          </ul>
        </div>

        {/* File Dropzone / Selector */}
        {!result && (
          <label className="flex cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed border-slate-300 bg-slate-50 p-6 text-center hover:border-indigo-400 hover:bg-indigo-50/50 transition-colors">
            <Upload className="h-8 w-8 text-indigo-500 mb-2" />
            <p className="text-sm font-medium text-slate-800">
              {selectedFile ? selectedFile.name : 'Click or drag a CSV file to upload'}
            </p>
            <p className="text-xs text-slate-500 mt-1">
              {selectedFile ? `${(selectedFile.size / 1024).toFixed(1)} KB` : 'Maximum file size 10MB'}
            </p>
            <input type="file" accept=".csv" className="sr-only" onChange={handleFileChange} />
          </label>
        )}

        {/* Import Results Summary */}
        {result && (
          <div className="space-y-4">
            <div className="grid grid-cols-3 gap-3 text-center">
              <div className="rounded-lg border border-slate-200 bg-slate-50 p-3">
                <p className="text-xs font-semibold text-slate-500 uppercase">Processed</p>
                <p className="text-xl font-bold text-slate-900 mt-1">{result.totalRows}</p>
              </div>
              <div className="rounded-lg border border-emerald-200 bg-emerald-50 p-3">
                <p className="text-xs font-semibold text-emerald-700 uppercase flex items-center justify-center gap-1">
                  <CheckCircle className="w-3.5 h-3.5" /> Successful
                </p>
                <p className="text-xl font-bold text-emerald-800 mt-1">{result.successfulCount}</p>
              </div>
              <div className="rounded-lg border border-rose-200 bg-rose-50 p-3">
                <p className="text-xs font-semibold text-rose-700 uppercase flex items-center justify-center gap-1">
                  <AlertCircle className="w-3.5 h-3.5" /> Failed / Skipped
                </p>
                <p className="text-xl font-bold text-rose-800 mt-1">{result.failedCount}</p>
              </div>
            </div>

            {/* Error table if any rows failed */}
            {result.errors.length > 0 && (
              <div className="space-y-2">
                <p className="text-xs font-semibold text-slate-700 uppercase tracking-wider flex items-center gap-1">
                  <AlertTriangle className="w-3.5 h-3.5 text-amber-500" />
                  Skipped Row Details ({result.errors.length})
                </p>
                <div className="max-h-48 overflow-y-auto rounded-lg border border-rose-200 bg-rose-50/40">
                  <table className="w-full text-xs">
                    <thead className="bg-rose-100/60 border-b border-rose-200 text-rose-900 font-semibold sticky top-0">
                      <tr>
                        <th className="p-2 text-left w-16">Row #</th>
                        <th className="p-2 text-left">Email</th>
                        <th className="p-2 text-left">Reason</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-rose-200/60 text-slate-700">
                      {result.errors.map((err, idx) => (
                        <tr key={idx} className="hover:bg-rose-100/30">
                          <td className="p-2 font-mono font-bold text-rose-700">Line {err.rowNumber}</td>
                          <td className="p-2 font-medium text-slate-900">{err.email}</td>
                          <td className="p-2 text-rose-800">{err.message}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            <div className="flex justify-end">
              <button
                onClick={handleReset}
                className="text-xs text-indigo-600 hover:text-indigo-800 font-medium underline"
              >
                Upload another file
              </button>
            </div>
          </div>
        )}
      </div>
    </Modal>
  );
}
