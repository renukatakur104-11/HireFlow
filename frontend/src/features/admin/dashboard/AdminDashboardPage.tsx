import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Users, Briefcase, TrendingUp, CheckCircle } from 'lucide-react';
import apiClient from '../../../lib/api/client';
import { API } from '../../../lib/api/endpoints';
import { ApiResponse } from '../../../types/api';
import { Card, CardContent, CardHeader, CardTitle } from '../../../components/ui/Card';
import { Skeleton } from '../../../components/ui/Skeleton';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';

interface KpiStats { totalStudents: number; activeDrives: number; totalApplications: number; selectedStudents: number; selectionRate: number; }
interface FunnelData { status: string; count: number; }

export function AdminDashboardPage() {
  const { data: kpiData, isLoading: kpiLoading } = useQuery<ApiResponse<KpiStats>>({
    queryKey: ['admin-kpi'],
    queryFn: () => apiClient.get(API.admin.stats.kpi).then(r => r.data),
  });
  const { data: funnelData, isLoading: funnelLoading } = useQuery<ApiResponse<FunnelData[]>>({
    queryKey: ['admin-funnel'],
    queryFn: () => apiClient.get(API.admin.stats.funnel).then(r => r.data),
  });
  const kpi = kpiData?.data;
  const funnel = funnelData?.data ?? [];

  return (
    <div className="space-y-6">
      <div><h1 className="text-2xl font-bold text-slate-900">Admin Dashboard</h1><p className="text-sm text-slate-500">Overview of all placement activity</p></div>
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {kpiLoading ? [1,2,3,4].map(i => <Skeleton key={i} className="h-24 rounded-xl" />) :
          [{ label:'Total Students',value:kpi?.totalStudents??0,Icon:Users,color:'bg-sky-50 text-sky-600' },
           { label:'Active Drives',value:kpi?.activeDrives??0,Icon:Briefcase,color:'bg-indigo-50 text-indigo-600' },
           { label:'Total Applications',value:kpi?.totalApplications??0,Icon:TrendingUp,color:'bg-violet-50 text-violet-600' },
           { label:'Selected',value:kpi?.selectedStudents??0,Icon:CheckCircle,color:'bg-emerald-50 text-emerald-600' }]
          .map(({ label, value, Icon, color }) => (
            <div key={label} className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
              <div className={`inline-flex h-10 w-10 items-center justify-center rounded-xl mb-3 ${color}`}><Icon className="w-5 h-5" /></div>
              <p className="text-2xl font-bold text-slate-900">{value}</p>
              <p className="text-xs text-slate-500 mt-1">{label}</p>
            </div>
          ))
        }
      </div>
      <Card>
        <CardHeader><CardTitle>Application Funnel</CardTitle></CardHeader>
        <CardContent>
          {funnelLoading ? <Skeleton className="h-64" /> : (
            <ResponsiveContainer width="100%" height={240}>
              <BarChart data={funnel.filter(f => f.count > 0)} margin={{ top:5,right:20,left:0,bottom:5 }}>
                <XAxis dataKey="status" tick={{ fontSize: 11 }} />
                <YAxis tick={{ fontSize: 11 }} />
                <Tooltip />
                <Bar dataKey="count" fill="#4f46e5" radius={[4,4,0,0]} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </CardContent>
      </Card>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <Card>
          <CardHeader><CardTitle>Quick Actions</CardTitle></CardHeader>
          <CardContent>
            <div className="space-y-2">
              <Link to="/admin/drives" className="flex items-center gap-2 p-3 rounded-lg hover:bg-slate-50 text-sm font-medium text-slate-700"><Briefcase className="w-4 h-4 text-indigo-500" />Manage Drives</Link>
              <Link to="/admin/students" className="flex items-center gap-2 p-3 rounded-lg hover:bg-slate-50 text-sm font-medium text-slate-700"><Users className="w-4 h-4 text-indigo-500" />Student Directory</Link>
              <Link to="/admin/notifications" className="flex items-center gap-2 p-3 rounded-lg hover:bg-slate-50 text-sm font-medium text-slate-700"><TrendingUp className="w-4 h-4 text-indigo-500" />Send Notifications</Link>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader><CardTitle>Placement Rate</CardTitle></CardHeader>
          <CardContent>
            <div className="flex items-center justify-center h-32">
              <div className="text-center">
                <p className="text-5xl font-bold text-indigo-600">{kpi?.selectionRate ?? 0}%</p>
                <p className="text-sm text-slate-500 mt-2">Overall selection rate</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
