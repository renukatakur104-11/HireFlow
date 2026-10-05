import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { Plus, Edit, Trash2, Users } from 'lucide-react';
import apiClient from '../../../lib/api/client';
import { API } from '../../../lib/api/endpoints';
import { Drive } from '../../../types/drive';
import { ApiResponse, PagedResponse } from '../../../types/api';
import { Button } from '../../../components/ui/Button';
import { Input } from '../../../components/ui/Input';
import { Skeleton } from '../../../components/ui/Skeleton';
import { StatusBadge } from '../../../components/ui/StatusBadge';
import { formatDate, formatCurrencyLPA } from '../../../lib/utils';
import { DriveFormModal } from './DriveFormModal';

export function AdminDrivesPage() {
  const qc = useQueryClient();
  const [search, setSearch] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [editDrive, setEditDrive] = useState<Drive | null>(null);
  const { data, isLoading } = useQuery<ApiResponse<PagedResponse<Drive>>>({
    queryKey: ['admin-drives', search],
    queryFn: () => apiClient.get(API.admin.drives.list, { params: { search: search || undefined } }).then(r => r.data),
  });
  const deleteMutation = useMutation({
    mutationFn: (id: number) => apiClient.delete(API.admin.drives.detail(id)),
    onSuccess: () => { toast.success('Drive deleted'); qc.invalidateQueries({ queryKey: ['admin-drives'] }); },
    onError: () => toast.error('Failed to delete drive'),
  });
  const drives = data?.data?.content ?? [];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div><h1 className="text-2xl font-bold text-slate-900">Manage Drives</h1><p className="text-sm text-slate-500">{drives.length} drives</p></div>
        <Button onClick={() => { setEditDrive(null); setShowForm(true); }} leftIcon={<Plus className="w-4 h-4" />}>Create Drive</Button>
      </div>
      <Input placeholder="Search company or role..." value={search} onChange={e => setSearch(e.target.value)} />
      {isLoading ? (
        <div className="space-y-3">{[1,2,3,4].map(i => <Skeleton key={i} className="h-20 rounded-xl" />)}</div>
      ) : (
        <div className="space-y-3">
          {drives.map(drive => (
            <div key={drive.id} className="flex items-center justify-between rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-1">
                  <h3 className="font-semibold text-slate-900 truncate">{drive.companyName}</h3>
                  <StatusBadge status={drive.status} />
                </div>
                <p className="text-sm text-slate-500 truncate">{drive.roleTitle} · {formatCurrencyLPA(drive.packageLpa)} · Deadline: {formatDate(drive.applicationDeadline)}</p>
                <p className="text-xs text-slate-400">{drive.totalApplications} applicants</p>
              </div>
              <div className="flex items-center gap-2 ml-4 shrink-0">
                <Link to={`/admin/drives/${drive.id}/applicants`}>
                  <Button size="sm" variant="outline" leftIcon={<Users className="w-3.5 h-3.5" />}>Applicants</Button>
                </Link>
                <Button size="sm" variant="outline" onClick={() => { setEditDrive(drive); setShowForm(true); }}><Edit className="w-3.5 h-3.5" /></Button>
                <Button size="sm" variant="danger" onClick={() => { if (confirm('Delete this drive?')) deleteMutation.mutate(drive.id); }}><Trash2 className="w-3.5 h-3.5" /></Button>
              </div>
            </div>
          ))}
          {drives.length === 0 && <p className="text-center py-8 text-slate-400 text-sm">No drives found.</p>}
        </div>
      )}
      {showForm && <DriveFormModal drive={editDrive} onClose={() => { setShowForm(false); setEditDrive(null); }} onSuccess={() => { setShowForm(false); setEditDrive(null); qc.invalidateQueries({ queryKey: ['admin-drives'] }); }} />}
    </div>
  );
}
