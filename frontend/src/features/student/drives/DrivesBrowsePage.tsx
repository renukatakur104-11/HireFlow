import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Search, Filter, Briefcase, MapPin, Clock, ChevronRight, CheckCircle, XCircle } from 'lucide-react';
import apiClient from '../../../lib/api/client';
import { API } from '../../../lib/api/endpoints';
import { Drive } from '../../../types/drive';
import { ApiResponse, PagedResponse } from '../../../types/api';
import { Input } from '../../../components/ui/Input';
import { Badge } from '../../../components/ui/Badge';
import { Skeleton } from '../../../components/ui/Skeleton';
import { EmptyState } from '../../../components/ui/EmptyState';
import { formatDate, formatCurrencyLPA, timeUntil } from '../../../lib/utils';


export function DrivesBrowsePage() {
  const [search, setSearch] = useState('');
  const [eligibleOnly, setEligibleOnly] = useState(false);
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery<ApiResponse<PagedResponse<Drive>>>({
    queryKey: ['drives', search, eligibleOnly, page],
    queryFn: () => apiClient.get(API.drives.list, { params: { search: search || undefined, status: 'OPEN', page, size: 12 } }).then(r => r.data),
    keepPreviousData: true,
  } as any);

  const drives = data?.data?.content ?? [];
  const totalPages = data?.data?.totalPages ?? 0;

  const filtered = eligibleOnly ? drives.filter(d => d.isEligible) : drives;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Placement Drives</h1>
          <p className="text-sm text-slate-500">Discover open placement opportunities</p>
        </div>
      </div>

      <div className="flex flex-col sm:flex-row gap-3">
        <div className="flex-1">
          <Input placeholder="Search company or role..." value={search} onChange={e => { setSearch(e.target.value); setPage(0); }}
            leftIcon={<Search className="w-4 h-4" />} />
        </div>
        <button
          onClick={() => setEligibleOnly(p => !p)}
          className={`flex items-center gap-2 px-4 py-2 rounded-lg border text-sm font-medium transition-colors ${
            eligibleOnly ? 'bg-indigo-600 text-white border-indigo-600' : 'bg-white text-slate-600 border-slate-300 hover:border-indigo-400'
          }`}
        >
          <Filter className="w-4 h-4" />
          Eligible for me
        </button>
      </div>

      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {Array.from({ length: 6 }).map((_, i) => <Skeleton key={i} className="h-52 rounded-xl" />)}
        </div>
      ) : filtered.length === 0 ? (
        <EmptyState title="No drives found" description="Try adjusting your search or filters." />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtered.map(drive => <DriveCard key={drive.id} drive={drive} />)}
        </div>
      )}

      {totalPages > 1 && (
        <div className="flex justify-center gap-2">
          <button disabled={page === 0} onClick={() => setPage(p => p - 1)} className="px-3 py-1.5 text-sm rounded-lg border border-slate-300 disabled:opacity-40 hover:bg-slate-50">Previous</button>
          <span className="px-3 py-1.5 text-sm text-slate-600">Page {page + 1} of {totalPages}</span>
          <button disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)} className="px-3 py-1.5 text-sm rounded-lg border border-slate-300 disabled:opacity-40 hover:bg-slate-50">Next</button>
        </div>
      )}
    </div>
  );
}

function DriveCard({ drive }: { drive: Drive }) {
  const deadlineStr = timeUntil(drive.applicationDeadline);
  const urgent = new Date(drive.applicationDeadline).getTime() - Date.now() < 86400000;

  return (
    <Link to={`/drives/${drive.id}`} className="block rounded-xl border border-slate-200 bg-white p-5 shadow-sm hover:shadow-md hover:border-indigo-300 transition-all group">
      <div className="flex items-start justify-between mb-3">
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600 font-bold text-lg">
          {drive.companyName.charAt(0)}
        </div>
        <div className="flex items-center gap-1.5">
          {drive.isEligible !== undefined && (
            drive.isEligible
              ? <span className="flex items-center gap-1 text-xs text-emerald-600 font-medium"><CheckCircle className="w-3.5 h-3.5" />Eligible</span>
              : <span className="flex items-center gap-1 text-xs text-slate-400"><XCircle className="w-3.5 h-3.5" />Ineligible</span>
          )}
          <span className={`text-xs font-medium px-2 py-0.5 rounded-full border ${ urgent ? 'bg-rose-50 text-rose-700 border-rose-200' : 'bg-slate-100 text-slate-600 border-slate-200'}`}>
            {deadlineStr}
          </span>
        </div>
      </div>

      <h3 className="font-semibold text-slate-900 group-hover:text-indigo-600 transition-colors">{drive.companyName}</h3>
      <p className="text-sm text-slate-500 mb-3">{drive.roleTitle}</p>

      <div className="flex flex-wrap gap-x-3 gap-y-1 text-xs text-slate-500 mb-3">
        <span className="flex items-center gap-1"><MapPin className="w-3.5 h-3.5" />{drive.location}</span>
        <span className="flex items-center gap-1"><Briefcase className="w-3.5 h-3.5" />{formatCurrencyLPA(drive.packageLpa)}</span>
        <span className="flex items-center gap-1"><Clock className="w-3.5 h-3.5" />{formatDate(drive.driveDate)}</span>
      </div>

      <div className="flex flex-wrap gap-1">
        {drive.allowedBranches.slice(0, 3).map(b => <Badge key={b} variant="secondary" size="sm">{b}</Badge>)}
        {drive.allowedBranches.length > 3 && <Badge variant="secondary" size="sm">+{drive.allowedBranches.length - 3}</Badge>}
      </div>

      <div className="mt-3 flex items-center text-xs font-medium text-indigo-600 group-hover:underline">
        View details <ChevronRight className="w-3.5 h-3.5 ml-1" />
      </div>
    </Link>
  );
}
