import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { Link } from 'react-router-dom';
import { Briefcase, MapPin, Calendar, CheckCircle, XCircle, Clock } from 'lucide-react';
import apiClient from '../../../lib/api/client';
import { API } from '../../../lib/api/endpoints';
import { Application, ApplicationStatus } from '../../../types/application';
import { ApiResponse } from '../../../types/api';
import { Button } from '../../../components/ui/Button';
import { Skeleton } from '../../../components/ui/Skeleton';
import { EmptyState } from '../../../components/ui/EmptyState';
import { StatusBadge } from '../../../components/ui/StatusBadge';
import { formatDate, formatCurrencyLPA } from '../../../lib/utils';

const STATUS_ORDER: ApplicationStatus[] = ['REGISTERED', 'ROUND_1', 'ROUND_2', 'TECHNICAL_INTERVIEW', 'HR_INTERVIEW', 'SELECTED', 'REJECTED', 'WITHDRAWN'];

export function MyApplicationsPage() {
  const qc = useQueryClient();
  const { data, isLoading } = useQuery<ApiResponse<Application[]>>({
    queryKey: ['my-applications'],
    queryFn: () => apiClient.get(API.applications.my).then(r => r.data),
  });
  const withdrawMutation = useMutation({
    mutationFn: (id: number) => apiClient.post(API.applications.withdraw(id)).then(r => r.data),
    onSuccess: () => { toast.success('Application withdrawn'); qc.invalidateQueries({ queryKey: ['my-applications'] }); },
    onError: (err: unknown) => { const e = err as { response?: { data?: { message?: string } } }; toast.error(e.response?.data?.message || 'Withdrawal failed'); },
  });
  const apps = data?.data ?? [];

  if (isLoading) return <div className="space-y-4">{[1,2,3].map(i => <Skeleton key={i} className="h-40 rounded-xl" />)}</div>;
  if (apps.length === 0) return <EmptyState title="No applications yet" description="Browse drives and apply." action={<Link to="/drives"><Button variant="outline">Browse Drives</Button></Link>} />;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">My Applications</h1>
        <p className="text-sm text-slate-500">{apps.length} application{apps.length !== 1 ? 's' : ''}</p>
      </div>
      <div className="space-y-4">
        {apps.map(app => (
          <div key={app.id} className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="flex items-start justify-between mb-4">
              <div>
                <h3 className="font-semibold text-slate-900">{app.companyName}</h3>
                <p className="text-sm text-slate-500">{app.roleTitle}</p>
                <div className="flex flex-wrap gap-3 mt-1 text-xs text-slate-400">
                  <span className="flex items-center gap-1"><MapPin className="w-3.5 h-3.5" />{app.location}</span>
                  <span className="flex items-center gap-1"><Briefcase className="w-3.5 h-3.5" />{formatCurrencyLPA(app.packageLpa)}</span>
                  <span className="flex items-center gap-1"><Calendar className="w-3.5 h-3.5" />{formatDate(app.driveDate)}</span>
                </div>
              </div>
              <StatusBadge status={app.currentStatus} />
            </div>
            <div className="flex items-center gap-1 overflow-x-auto pb-1">
              {STATUS_ORDER.filter(s => !['WITHDRAWN','REJECTED'].includes(s) || s === app.currentStatus).map((status, i, arr) => {
                const idx = STATUS_ORDER.indexOf(app.currentStatus);
                const sIdx = STATUS_ORDER.indexOf(status);
                const reached = idx >= sIdx;
                const isRej = app.currentStatus === 'REJECTED' && status === 'REJECTED';
                const isWit = app.currentStatus === 'WITHDRAWN' && status === 'WITHDRAWN';
                return (
                  <div key={status} className="flex items-center">
                    <div className={`flex h-6 w-6 items-center justify-center rounded-full text-xs shrink-0 ${ isRej ? 'bg-rose-100 text-rose-600' : isWit ? 'bg-slate-100 text-slate-400' : reached ? 'bg-indigo-600 text-white' : 'bg-slate-100 text-slate-400' }`}>
                      {reached && !isRej && !isWit ? <CheckCircle className="w-3.5 h-3.5" /> : isRej ? <XCircle className="w-3.5 h-3.5" /> : <Clock className="w-3.5 h-3.5" />}
                    </div>
                    <p className="hidden sm:block ml-1 text-xs text-slate-500 whitespace-nowrap">{status.replace(/_/g,' ')}</p>
                    {i < arr.length - 1 && <div className={`w-6 h-0.5 mx-1 ${ reached ? 'bg-indigo-300' : 'bg-slate-200'}`} />}
                  </div>
                );
              })}
            </div>
            {app.currentStatus === 'REGISTERED' && (
              <div className="mt-3">
                <Button size="sm" variant="danger" isLoading={withdrawMutation.isPending} onClick={() => withdrawMutation.mutate(app.id)}>Withdraw Application</Button>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
