import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { AlertCircle, CheckCircle } from 'lucide-react';
import apiClient from '../../../lib/api/client';
import { API } from '../../../lib/api/endpoints';
import { Application } from '../../../types/application';
import { Drive } from '../../../types/drive';
import { ApiResponse, PagedResponse } from '../../../types/api';
import { useAuth } from '../../../contexts/AuthContext';
import { Button } from '../../../components/ui/Button';
import { Card, CardContent, CardHeader, CardTitle } from '../../../components/ui/Card';
import { StatusBadge } from '../../../components/ui/StatusBadge';
import { Skeleton } from '../../../components/ui/Skeleton';
import { formatCurrencyLPA, timeUntil, formatDate } from '../../../lib/utils';

export function StudentDashboardPage() {
  const { user } = useAuth();
  const { data: appsData, isLoading: appsLoading } = useQuery<ApiResponse<Application[]>>({
    queryKey: ['my-applications'],
    queryFn: () => apiClient.get(API.applications.my).then(r => r.data),
  });
  const { data: drivesData, isLoading: drivesLoading } = useQuery<ApiResponse<PagedResponse<Drive>>>({
    queryKey: ['open-drives'],
    queryFn: () => apiClient.get(API.drives.list, { params: { status: 'OPEN', size: 6 } }).then(r => r.data),
  });
  const apps = appsData?.data ?? [];
  const drives = drivesData?.data?.content ?? [];
  const eligibleDrives = drives.filter(d => d.isEligible);
  const urgentDrives = eligibleDrives.filter(d => new Date(d.applicationDeadline).getTime() - Date.now() < 86400000 * 2);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Welcome back, {user?.fullName?.split(' ')[0] || 'Student'}!</h1>
        <p className="text-sm text-slate-500">Track your placement journey from here.</p>
      </div>
      {!user?.profileComplete && (
        <div className="flex items-center gap-3 rounded-xl border border-amber-200 bg-amber-50 p-4">
          <AlertCircle className="w-5 h-5 text-amber-500 shrink-0" />
          <div className="flex-1">
            <p className="text-sm font-semibold text-amber-800">Complete your profile to apply</p>
            <p className="text-xs text-amber-600">Upload resume and fill in all details.</p>
          </div>
          <Link to="/profile"><Button size="sm" variant="outline">Complete Profile</Button></Link>
        </div>
      )}
      {urgentDrives.length > 0 && (
        <div className="rounded-xl border border-rose-200 bg-rose-50 p-4">
          <p className="text-sm font-semibold text-rose-800 mb-2">⏰ Deadlines closing soon</p>
          <div className="space-y-1">
            {urgentDrives.map(d => (
              <Link key={d.id} to={`/drives/${d.id}`} className="flex items-center justify-between text-sm text-rose-700 hover:underline">
                <span>{d.companyName} – {d.roleTitle}</span>
                <span className="font-medium">{timeUntil(d.applicationDeadline)}</span>
              </Link>
            ))}
          </div>
        </div>
      )}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {[{label:'Applied',v:apps.length,c:'indigo'},{label:'In Progress',v:apps.filter(a=>!['SELECTED','REJECTED','WITHDRAWN'].includes(a.currentStatus)).length,c:'blue'},{label:'Selected',v:apps.filter(a=>a.currentStatus==='SELECTED').length,c:'emerald'},{label:'Eligible Drives',v:eligibleDrives.length,c:'violet'}].map(({label,v,c}) => (
          <div key={label} className={`rounded-xl p-4 bg-${c}-50 text-${c}-700`}>
            <p className="text-2xl font-bold">{v}</p>
            <p className="text-xs font-medium mt-1">{label}</p>
          </div>
        ))}
      </div>
      <Card>
        <CardHeader><CardTitle>Active Applications</CardTitle><Link to="/applications" className="text-sm text-indigo-600 hover:underline">View all</Link></CardHeader>
        <CardContent>
          {appsLoading ? <Skeleton className="h-32" /> : apps.length === 0 ? (
            <p className="text-sm text-slate-400 text-center py-4">No applications yet. <Link to="/drives" className="text-indigo-600 hover:underline">Browse drives</Link></p>
          ) : (
            <div className="space-y-3">
              {apps.slice(0,4).map(app => (
                <div key={app.id} className="flex items-center justify-between py-2 border-b border-slate-100 last:border-0">
                  <div><p className="text-sm font-medium text-slate-900">{app.companyName}</p><p className="text-xs text-slate-500">{app.roleTitle} · {formatDate(app.driveDate)}</p></div>
                  <StatusBadge status={app.currentStatus} />
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
      <Card>
        <CardHeader><CardTitle>Eligible Drives for You</CardTitle><Link to="/drives" className="text-sm text-indigo-600 hover:underline">Browse all</Link></CardHeader>
        <CardContent>
          {drivesLoading ? <Skeleton className="h-32" /> : eligibleDrives.length === 0 ? (
            <p className="text-sm text-slate-400 text-center py-4">No eligible drives right now.</p>
          ) : (
            <div className="space-y-3">
              {eligibleDrives.slice(0,4).map(d => (
                <Link key={d.id} to={`/drives/${d.id}`} className="flex items-center justify-between py-2 border-b border-slate-100 last:border-0 hover:bg-slate-50 rounded px-1">
                  <div><p className="text-sm font-medium text-slate-900">{d.companyName}</p><p className="text-xs text-slate-500">{d.roleTitle} · {formatCurrencyLPA(d.packageLpa)}</p></div>
                  <div className="flex items-center gap-2"><span className="text-xs text-slate-400">{timeUntil(d.applicationDeadline)}</span><CheckCircle className="w-4 h-4 text-emerald-500" /></div>
                </Link>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
