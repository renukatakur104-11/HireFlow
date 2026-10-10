import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Download, Upload, Search, FileText } from 'lucide-react';
import apiClient from '../../../lib/api/client';
import { API } from '../../../lib/api/endpoints';
import { StudentProfile } from '../../../types/student';
import { ApiResponse } from '../../../types/api';
import { Input } from '../../../components/ui/Input';
import { Button } from '../../../components/ui/Button';
import { Badge } from '../../../components/ui/Badge';
import { Skeleton } from '../../../components/ui/Skeleton';
import { BulkImportModal } from './BulkImportModal';
import toast from 'react-hot-toast';

export function AdminStudentsPage() {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [branch, setBranch] = useState('');
  const [isImportModalOpen, setIsImportModalOpen] = useState(false);

  const { data, isLoading } = useQuery<ApiResponse<StudentProfile[]>>({
    queryKey: ['admin-students', search, branch],
    queryFn: () =>
      apiClient.get(API.admin.students.list, { params: { search: search || undefined, branch: branch || undefined } }).then((r) => r.data),
  });

  const students = data?.data ?? [];

  const handleExport = () => {
    window.open(`${import.meta.env.VITE_API_BASE_URL || '/api'}${API.admin.students.export}`, '_blank');
  };

  const handleViewResume = async (studentId: number) => {
    try {
      const response = await apiClient.get(API.admin.students.resume(studentId), { responseType: 'blob' });
      const blob = new Blob([response.data], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob);
      window.open(url, '_blank');
    } catch {
      toast.error('Failed to open resume');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Student Directory</h1>
          <p className="text-sm text-slate-500">{students.length} student(s) enrolled</p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" size="sm" onClick={() => setIsImportModalOpen(true)} leftIcon={<Upload className="w-4 h-4 text-indigo-600" />}>
            Import CSV
          </Button>
          <Button variant="outline" size="sm" onClick={handleExport} leftIcon={<Download className="w-4 h-4 text-slate-600" />}>
            Export CSV
          </Button>
        </div>
      </div>

      <div className="flex gap-3">
        <div className="flex-1">
          <Input placeholder="Search name or email..." value={search} onChange={(e) => setSearch(e.target.value)} leftIcon={<Search className="w-4 h-4" />} />
        </div>
        <input
          placeholder="Filter branch..."
          value={branch}
          onChange={(e) => setBranch(e.target.value)}
          className="h-10 rounded-lg border border-slate-300 px-3 text-sm focus:border-indigo-500 focus:outline-none"
        />
      </div>

      {isLoading ? (
        <Skeleton className="h-64 rounded-xl" />
      ) : (
        <div className="rounded-xl border border-slate-200 bg-white overflow-hidden shadow-sm">
          <table className="w-full text-sm">
            <thead className="bg-slate-50 border-b border-slate-200">
              <tr>
                {['Name', 'Branch', 'Year', 'CGPA', 'Skills', 'Profile', 'Resume'].map((h) => (
                  <th key={h} className="p-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {students.map((s) => (
                <tr key={s.id} className="hover:bg-slate-50/80">
                  <td className="p-3">
                    <p className="font-medium text-slate-900">{s.fullName}</p>
                    <p className="text-xs text-slate-500">{s.email}</p>
                  </td>
                  <td className="p-3 text-slate-600">{s.branch}</td>
                  <td className="p-3 text-slate-600">{s.graduationYear}</td>
                  <td className="p-3 font-medium text-slate-900">{s.cgpa}</td>
                  <td className="p-3">
                    <div className="flex flex-wrap gap-1">
                      {s.skills.slice(0, 3).map((sk) => (
                        <Badge key={sk} variant="secondary" size="sm">
                          {sk}
                        </Badge>
                      ))}
                      {s.skills.length > 3 && (
                        <Badge variant="secondary" size="sm">
                          +{s.skills.length - 3}
                        </Badge>
                      )}
                    </div>
                  </td>
                  <td className="p-3">
                    <span className={`text-xs font-semibold ${s.profileComplete ? 'text-emerald-600' : 'text-amber-600'}`}>
                      {s.profileComplete ? '✓ Complete' : '⚠ Incomplete'}
                    </span>
                  </td>
                  <td className="p-3">
                    {s.hasResume ? (
                      <button
                        onClick={() => handleViewResume(s.id)}
                        className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 hover:underline flex items-center gap-1"
                      >
                        <FileText className="w-3.5 h-3.5" />
                        View PDF
                      </button>
                    ) : (
                      <span className="text-xs text-slate-400">—</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {students.length === 0 && <p className="text-center py-8 text-slate-400 text-sm">No students found.</p>}
        </div>
      )}

      {/* Bulk Import Modal */}
      <BulkImportModal
        isOpen={isImportModalOpen}
        onClose={() => setIsImportModalOpen(false)}
        onSuccess={() => queryClient.invalidateQueries({ queryKey: ['admin-students'] })}
      />
    </div>
  );
}
