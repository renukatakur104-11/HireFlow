import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { Briefcase, MapPin, Clock, Calendar, CheckCircle, XCircle } from 'lucide-react';
import apiClient from '../../../lib/api/client';
import { API } from '../../../lib/api/endpoints';
import { Drive } from '../../../types/drive';
import { ApiResponse } from '../../../types/api';
import { Button } from '../../../components/ui/Button';
import { Badge } from '../../../components/ui/Badge';
import { Card, CardContent } from '../../../components/ui/Card';
import { Skeleton } from '../../../components/ui/Skeleton';
import { formatDate, formatCurrencyLPA } from '../../../lib/utils';

export function DriveDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const qc = useQueryClient();

  const { data, isLoading } = useQuery<ApiResponse<Drive>>({
    queryKey: ['drive', id],
    queryFn: () => apiClient.get(API.drives.detail(Number(id))).then(r => r.data),
  });
  const drive = data?.data;

  const applyMutation = useMutation({
    mutationFn: () => apiClient.post(API.drives.apply(Number(id))).then(r => r.data),
    onSuccess: () => {
      toast.success('Application submitted successfully!');
      qc.invalidateQueries({ queryKey: ['drive', id] });
      qc.invalidateQueries({ queryKey: ['my-applications'] });
      navigate('/applications');
    },
    onError: (err: unknown) => {
      const e = err as { response?: { data?: { message?: string } } };
      toast.error(e.response?.data?.message || 'Failed to apply');
    },
  });

  if (isLoading) return <div className="space-y-4"><Skeleton className="h-64 rounded-xl" /><Skeleton className="h-48 rounded-xl" /></div>;
  if (!drive) return <div className="text-center py-12 text-slate-500">Drive not found.</div>;

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <div className="flex items-start justify-between mb-4">
          <div className="flex h-14 w-14 items-center justify-center rounded-xl bg-indigo-50 text-indigo-700 font-bold text-2xl">
            {drive.companyName.charAt(0)}
          </div>
          <Badge variant={drive.status === 'OPEN' ? 'success' : 'secondary'}>{drive.status}</Badge>
        </div>
        <h1 className="text-2xl font-bold text-slate-900">{drive.companyName}</h1>
        <p className="text-lg text-slate-600 mt-1">{drive.roleTitle}</p>
        <div className="flex flex-wrap gap-4 mt-4 text-sm text-slate-500">
          <span className="flex items-center gap-1.5"><MapPin className="w-4 h-4" />{drive.location}</span>
          <span className="flex items-center gap-1.5"><Briefcase className="w-4 h-4" />{formatCurrencyLPA(drive.packageLpa)}</span>
          <span className="flex items-center gap-1.5"><Clock className="w-4 h-4" />{formatDate(drive.driveDate)}</span>
          <span className="flex items-center gap-1.5"><Calendar className="w-4 h-4" />Deadline: {formatDate(drive.applicationDeadline)}</span>
        </div>
      </div>
      
      <Card>
        <CardContent className="pt-6">
          <h2 className="text-xl font-bold mb-4">About this role</h2>
          <p className="whitespace-pre-line text-slate-700">{drive.description}</p>
        </CardContent>
      </Card>
      
      <div className="flex justify-end pt-4">
        <Button 
          onClick={() => applyMutation.mutate()} 
          isLoading={applyMutation.isPending}
          disabled={!drive.isEligible || drive.hasApplied}
          className="w-full sm:w-auto"
        >
          {drive.hasApplied ? (
            <><CheckCircle className="w-4 h-4 mr-2" />Applied</>
          ) : !drive.isEligible ? (
            <><XCircle className="w-4 h-4 mr-2" />Ineligible</>
          ) : (
            'Apply Now'
          )}
        </Button>
      </div>
    </div>
  );
}
