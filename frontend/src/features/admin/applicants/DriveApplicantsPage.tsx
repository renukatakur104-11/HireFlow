import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { CheckSquare, Square, Sparkles, ShieldAlert, Award } from 'lucide-react';
import apiClient from '../../../lib/api/client';
import { API } from '../../../lib/api/endpoints';
import { Application, ApplicationStatus } from '../../../types/application';
import { ApiResponse, PagedResponse } from '../../../types/api';
import { Button } from '../../../components/ui/Button';
import { Skeleton } from '../../../components/ui/Skeleton';
import { StatusBadge } from '../../../components/ui/StatusBadge';
import { Modal } from '../../../components/ui/Modal';
import { formatDate } from '../../../lib/utils';
import { Badge } from '../../../components/ui/Badge';

const STATUSES: ApplicationStatus[] = ['REGISTERED', 'ROUND_1', 'ROUND_2', 'TECHNICAL_INTERVIEW', 'HR_INTERVIEW', 'SELECTED', 'REJECTED'];

interface CandidateMatchResult {
  applicationId: number;
  studentProfileId: number;
  studentName: string;
  studentEmail: string;
  branch: string;
  cgpa: number;
  skills: string[];
  currentStatus: string;
  matchScore: number;
  matchExplanation: string;
  aiEnriched: boolean;
}

interface CandidateMatchResponse {
  driveId: number;
  companyName: string;
  roleTitle: string;
  totalCandidates: number;
  aiAssisted: boolean;
  disclaimer: string;
  candidates: CandidateMatchResult[];
}

export function DriveApplicantsPage() {
  const { driveId } = useParams<{ driveId: string }>();
  const qc = useQueryClient();
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const [updateModal, setUpdateModal] = useState<{ appId: number | null; bulk: boolean }>({ appId: null, bulk: false });
  const [newStatus, setNewStatus] = useState<ApplicationStatus>('ROUND_1');
  const [remarks, setRemarks] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [showAiMatchModal, setShowAiMatchModal] = useState(false);

  const { data, isLoading } = useQuery<ApiResponse<PagedResponse<Application>>>({
    queryKey: ['drive-applicants', driveId, statusFilter],
    queryFn: () =>
      apiClient.get(API.admin.drives.applicants(Number(driveId)), { params: { status: statusFilter || undefined } }).then((r) => r.data),
  });

  const { data: aiMatchData, isLoading: aiMatchLoading, refetch: refetchAiMatch } = useQuery<ApiResponse<CandidateMatchResponse>>({
    queryKey: ['ai-match-candidates', driveId],
    queryFn: () => apiClient.post(API.admin.ai.matchCandidates, { driveId: Number(driveId) }).then((r) => r.data),
    enabled: showAiMatchModal,
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, status, r }: { id: number; status: ApplicationStatus; r: string }) =>
      apiClient.patch(API.admin.applications.status(id), { newStatus: status, remarks: r }),
    onSuccess: () => {
      toast.success('Status updated');
      qc.invalidateQueries({ queryKey: ['drive-applicants'] });
      setUpdateModal({ appId: null, bulk: false });
    },
    onError: (err: unknown) => {
      const e = err as { response?: { data?: { message?: string } } };
      toast.error(e.response?.data?.message || 'Failed');
    },
  });

  const bulkMutation = useMutation({
    mutationFn: ({ ids, status, r }: { ids: number[]; status: ApplicationStatus; r: string }) =>
      apiClient.post(API.admin.applications.bulkStatus, { applicationIds: ids, newStatus: status, remarks: r }),
    onSuccess: () => {
      toast.success('Bulk update done');
      qc.invalidateQueries({ queryKey: ['drive-applicants'] });
      setSelected(new Set());
      setUpdateModal({ appId: null, bulk: false });
    },
    onError: (err: unknown) => {
      const e = err as { response?: { data?: { message?: string } } };
      toast.error(e.response?.data?.message || 'Failed');
    },
  });

  const apps = data?.data?.content ?? [];
  const toggleSelect = (id: number) =>
    setSelected((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  const toggleAll = () => setSelected((prev) => (prev.size === apps.length ? new Set() : new Set(apps.map((a) => a.id))));

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Drive Applicants</h1>
          <p className="text-sm text-slate-500">{apps.length} candidate(s) currently registered</p>
        </div>
        <div className="flex items-center gap-2">
          <Button
            size="sm"
            variant="outline"
            onClick={() => {
              setShowAiMatchModal(true);
              refetchAiMatch();
            }}
            leftIcon={<Sparkles className="w-3.5 h-3.5 text-indigo-600" />}
          >
            AI Candidate Match Advisor
          </Button>
          {selected.size > 0 && (
            <Button size="sm" onClick={() => setUpdateModal({ appId: null, bulk: true })}>
              Update {selected.size} selected
            </Button>
          )}
        </div>
      </div>

      <div className="flex gap-2 flex-wrap">
        <button
          onClick={() => setStatusFilter('')}
          className={`px-3 py-1.5 text-xs rounded-full border ${!statusFilter ? 'bg-indigo-600 text-white border-indigo-600' : 'border-slate-300 text-slate-600'}`}
        >
          All
        </button>
        {STATUSES.map((s) => (
          <button
            key={s}
            onClick={() => setStatusFilter(s)}
            className={`px-3 py-1.5 text-xs rounded-full border ${statusFilter === s ? 'bg-indigo-600 text-white border-indigo-600' : 'border-slate-300 text-slate-600'}`}
          >
            {s.replace(/_/g, ' ')}
          </button>
        ))}
      </div>

      {isLoading ? (
        <Skeleton className="h-64 rounded-xl" />
      ) : (
        <div className="rounded-xl border border-slate-200 bg-white overflow-hidden shadow-sm">
          <table className="w-full text-sm">
            <thead className="bg-slate-50 border-b border-slate-200">
              <tr>
                <th className="p-3 w-10">
                  <button onClick={toggleAll} aria-label="Select all applicants">
                    {selected.size === apps.length && apps.length > 0 ? (
                      <CheckSquare className="w-4 h-4 text-indigo-600" />
                    ) : (
                      <Square className="w-4 h-4 text-slate-400" />
                    )}
                  </button>
                </th>
                {['Student', 'Branch / CGPA', 'Status', 'Applied', 'Action'].map((h) => (
                  <th key={h} className="p-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {apps.map((app) => (
                <tr key={app.id} className={selected.has(app.id) ? 'bg-indigo-50/60' : 'hover:bg-slate-50/80'}>
                  <td className="p-3">
                    <button onClick={() => toggleSelect(app.id)} aria-label={`Select ${app.studentName}`}>
                      {selected.has(app.id) ? <CheckSquare className="w-4 h-4 text-indigo-600" /> : <Square className="w-4 h-4 text-slate-400" />}
                    </button>
                  </td>
                  <td className="p-3">
                    <p className="font-medium text-slate-900">{app.studentName}</p>
                    <p className="text-xs text-slate-500">{app.studentEmail}</p>
                  </td>
                  <td className="p-3 text-slate-600">
                    {app.branch} / <span className="font-medium text-slate-900">{app.cgpa}</span>
                  </td>
                  <td className="p-3">
                    <StatusBadge status={app.currentStatus} />
                  </td>
                  <td className="p-3 text-slate-500">{formatDate(app.appliedAt)}</td>
                  <td className="p-3">
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => {
                        setUpdateModal({ appId: app.id, bulk: false });
                        setNewStatus('ROUND_1');
                        setRemarks('');
                      }}
                    >
                      Update
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {apps.length === 0 && <p className="text-center py-8 text-slate-400 text-sm">No applicants found.</p>}
        </div>
      )}

      {/* Status Update Modal */}
      {(updateModal.appId !== null || updateModal.bulk) && (
        <Modal
          isOpen
          title={updateModal.bulk ? `Update ${selected.size} applications` : 'Update Status'}
          onClose={() => setUpdateModal({ appId: null, bulk: false })}
          maxWidth="sm"
          footer={
            <>
              <Button variant="secondary" size="sm" onClick={() => setUpdateModal({ appId: null, bulk: false })}>
                Cancel
              </Button>
              <Button
                size="sm"
                isLoading={updateMutation.isPending || bulkMutation.isPending}
                onClick={() => {
                  if (updateModal.bulk) bulkMutation.mutate({ ids: Array.from(selected), status: newStatus, r: remarks });
                  else updateMutation.mutate({ id: updateModal.appId!, status: newStatus, r: remarks });
                }}
              >
                Apply
              </Button>
            </>
          }
        >
          <div className="space-y-4">
            <div className="space-y-1.5">
              <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">New Status</label>
              <select
                className="flex h-10 w-full rounded-lg border border-slate-300 bg-white px-3 text-sm focus:outline-none focus:border-indigo-500"
                value={newStatus}
                onChange={(e) => setNewStatus(e.target.value as ApplicationStatus)}
              >
                {STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {s.replace(/_/g, ' ')}
                  </option>
                ))}
              </select>
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">Remarks</label>
              <textarea
                className="flex min-h-[80px] w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm focus:outline-none focus:border-indigo-500"
                value={remarks}
                onChange={(e) => setRemarks(e.target.value)}
                placeholder="Optional remarks..."
              />
            </div>
          </div>
        </Modal>
      )}

      {/* AI Candidate Matching Advisor Modal */}
      {showAiMatchModal && (
        <Modal
          isOpen
          title="AI Candidate Match Advisor"
          onClose={() => setShowAiMatchModal(false)}
          maxWidth="2xl"
          footer={
            <Button variant="secondary" size="sm" onClick={() => setShowAiMatchModal(false)}>
              Close
            </Button>
          }
        >
          <div className="space-y-4">
            <div className="rounded-lg border border-amber-200 bg-amber-50/80 p-3 text-xs text-amber-800 flex items-start gap-2">
              <ShieldAlert className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
              <div>
                <strong>Advisory Only:</strong> All students below have already satisfied the strict deterministic eligibility rules (CGPA, Branch, Backlogs). AI match scores and explanations are advisory recommendations to assist coordinator evaluation. Final selection decisions remain 100% with the Placement Coordinator.
              </div>
            </div>

            {aiMatchLoading ? (
              <div className="space-y-3 py-4">
                <Skeleton className="h-16 rounded-xl" />
                <Skeleton className="h-16 rounded-xl" />
                <Skeleton className="h-16 rounded-xl" />
              </div>
            ) : aiMatchData?.data?.candidates?.length ? (
              <div className="space-y-3 max-h-[450px] overflow-y-auto pr-1">
                {aiMatchData.data.candidates.map((cand, idx) => {
                  const scorePct = Math.round(cand.matchScore * 100);
                  const scoreBadgeClass =
                    scorePct >= 80
                      ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                      : scorePct >= 60
                      ? 'bg-indigo-50 text-indigo-700 border-indigo-200'
                      : 'bg-slate-100 text-slate-700 border-slate-200';

                  return (
                    <div key={cand.applicationId} className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm space-y-2 hover:border-indigo-300 transition-colors">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <span className="flex h-6 w-6 items-center justify-center rounded-full bg-slate-100 text-xs font-bold text-slate-600">
                            #{idx + 1}
                          </span>
                          <div>
                            <h4 className="font-semibold text-slate-900 text-sm">{cand.studentName}</h4>
                            <p className="text-xs text-slate-500">{cand.branch} • CGPA: {cand.cgpa}</p>
                          </div>
                        </div>
                        <div className="flex items-center gap-2">
                          <span className={`px-2.5 py-1 text-xs font-bold rounded-full border flex items-center gap-1 ${scoreBadgeClass}`}>
                            <Award className="w-3 h-3" />
                            {scorePct}% Match
                          </span>
                          {cand.aiEnriched && (
                            <span className="px-1.5 py-0.5 text-[10px] font-semibold bg-violet-100 text-violet-700 rounded">
                              AI Enriched
                            </span>
                          )}
                        </div>
                      </div>

                      {cand.skills && cand.skills.length > 0 && (
                        <div className="flex flex-wrap gap-1">
                          {cand.skills.map((s) => (
                            <Badge key={s} variant="secondary" size="sm">
                              {s}
                            </Badge>
                          ))}
                        </div>
                      )}

                      <p className="text-xs text-slate-600 bg-slate-50 p-2 rounded border border-slate-100 italic">
                        "{cand.matchExplanation}"
                      </p>
                    </div>
                  );
                })}
              </div>
            ) : (
              <p className="text-center py-8 text-slate-400 text-sm">No eligible candidate applications found to evaluate.</p>
            )}
          </div>
        </Modal>
      )}
    </div>
  );
}
