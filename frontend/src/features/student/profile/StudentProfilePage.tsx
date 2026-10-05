import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { Upload, User, BookOpen, CheckCircle, AlertCircle, X } from 'lucide-react';
import apiClient from '../../../lib/api/client';
import { API } from '../../../lib/api/endpoints';
import { profileUpdateSchema, ProfileUpdateFormValues } from '../../../lib/validation/profileSchemas';
import { StudentProfile } from '../../../types/student';
import { ApiResponse } from '../../../types/api';
import { Button } from '../../../components/ui/Button';
import { Input } from '../../../components/ui/Input';
import { Card, CardHeader, CardTitle, CardContent } from '../../../components/ui/Card';
import { Skeleton } from '../../../components/ui/Skeleton';

const BRANCHES = ['Computer Science', 'Information Technology', 'Electronics & Communication', 'Mechanical Engineering', 'Civil Engineering', 'Chemical Engineering', 'Electrical Engineering', 'Other'];

export function StudentProfilePage() {
  const queryClient = useQueryClient();
  const [skillInput, setSkillInput] = useState('');
  const [localSkills, setLocalSkills] = useState<string[]>([]);
  const [uploadingResume, setUploadingResume] = useState(false);

  const { data, isLoading } = useQuery<ApiResponse<StudentProfile>>({
    queryKey: ['student-profile'],
    queryFn: () => apiClient.get(API.students.me).then(r => r.data),
  });
  const profile = data?.data;

  const { register, handleSubmit, reset, formState: { errors, isDirty } } = useForm<ProfileUpdateFormValues>({
    resolver: zodResolver(profileUpdateSchema),
  });

  useEffect(() => {
    if (profile) {
      reset({ fullName: profile.fullName, phone: profile.phone, branch: profile.branch, graduationYear: profile.graduationYear, cgpa: profile.cgpa, backlogs: profile.backlogs });
      setLocalSkills(profile.skills || []);
    }
  }, [profile, reset]);

  const updateMutation = useMutation({
    mutationFn: (formData: ProfileUpdateFormValues) =>
      apiClient.put(API.students.me, { ...formData, skills: localSkills }).then(r => r.data),
    onSuccess: () => {
      toast.success('Profile updated!');
      queryClient.invalidateQueries({ queryKey: ['student-profile'] });
    },
    onError: (err: unknown) => {
      const e = err as { response?: { data?: { message?: string } } };
      toast.error(e.response?.data?.message || 'Update failed');
    },
  });

  const handleResumeUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (!file.name.toLowerCase().endsWith('.pdf')) { toast.error('Only PDF files allowed'); return; }
    if (file.size > 5 * 1024 * 1024) { toast.error('Max 5MB allowed'); return; }
    setUploadingResume(true);
    const fd = new FormData();
    fd.append('resume', file);
    try {
      await apiClient.post(API.students.resume, fd, { headers: { 'Content-Type': 'multipart/form-data' } });
      toast.success('Resume uploaded!');
      queryClient.invalidateQueries({ queryKey: ['student-profile'] });
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } };
      toast.error(e.response?.data?.message || 'Upload failed');
    } finally { setUploadingResume(false); e.target.value = ''; }
  };

  const addSkill = () => {
    const s = skillInput.trim();
    if (s && !localSkills.includes(s)) setLocalSkills(p => [...p, s]);
    setSkillInput('');
  };

  const pct = profile?.completenessPercent ?? 0;

  if (isLoading) return (
    <div className="max-w-3xl mx-auto space-y-6">
      <Skeleton className="h-8 w-48" />
      <Skeleton className="h-64 w-full rounded-xl" />
    </div>
  );

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">My Profile</h1>
          <p className="text-sm text-slate-500">Keep your profile complete to apply to placement drives.</p>
        </div>
        <div className="text-right">
          <div className="text-2xl font-bold text-indigo-600">{pct}%</div>
          <div className="text-xs text-slate-500">Complete</div>
        </div>
      </div>

      <div className="h-2 w-full rounded-full bg-slate-100 overflow-hidden">
        <div
          className={`h-2 rounded-full transition-all duration-500 ${pct >= 100 ? 'bg-emerald-500' : pct >= 60 ? 'bg-indigo-500' : 'bg-amber-400'}`}
          style={{ width: `${pct}%` }}
        />
      </div>

      {!profile?.profileComplete && (
        <div className="flex gap-3 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
          <AlertCircle className="h-5 w-5 text-amber-500 shrink-0 mt-0.5" />
          <span>Complete your profile and upload your resume before applying to placement drives.</span>
        </div>
      )}

      <form onSubmit={handleSubmit(d => updateMutation.mutate(d))} className="space-y-6">
        <Card>
          <CardHeader><CardTitle><User className="inline w-4 h-4 mr-2 text-indigo-500" />Personal Information</CardTitle></CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Input label="Full Name" error={errors.fullName?.message} {...register('fullName')} />
              <Input label="Phone" error={errors.phone?.message} {...register('phone')} />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader><CardTitle><BookOpen className="inline w-4 h-4 mr-2 text-indigo-500" />Academic Details</CardTitle></CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-700">Branch</label>
                <select className="flex h-10 w-full rounded-lg border border-slate-300 bg-white px-3 text-sm focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/20" {...register('branch')}>
                  <option value="">Select branch</option>
                  {BRANCHES.map(b => <option key={b} value={b}>{b}</option>)}
                </select>
              </div>
              <Input label="Graduation Year" type="number" error={errors.graduationYear?.message} {...register('graduationYear')} />
              <Input label="CGPA" type="number" step="0.01" error={errors.cgpa?.message} {...register('cgpa')} />
              <Input label="Active Backlogs" type="number" error={errors.backlogs?.message} {...register('backlogs')} />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader><CardTitle>Skills</CardTitle></CardHeader>
          <CardContent>
            <div className="flex gap-2 mb-3">
              <input value={skillInput} onChange={e => setSkillInput(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && (e.preventDefault(), addSkill())}
                placeholder="e.g. Java, React, MySQL"
                className="flex-1 h-9 rounded-lg border border-slate-300 bg-white px-3 text-sm focus:border-indigo-500 focus:outline-none"
              />
              <Button type="button" variant="outline" size="sm" onClick={addSkill}>Add</Button>
            </div>
            <div className="flex flex-wrap gap-2">
              {localSkills.map(s => (
                <span key={s} className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-indigo-50 text-indigo-700 border border-indigo-200 text-xs font-medium">
                  {s}<button type="button" onClick={() => setLocalSkills(p => p.filter(x => x !== s))}><X className="w-3 h-3" /></button>
                </span>
              ))}
              {localSkills.length === 0 && <p className="text-sm text-slate-400">No skills added yet.</p>}
            </div>
          </CardContent>
        </Card>

        <Button type="submit" isLoading={updateMutation.isPending} disabled={!isDirty && localSkills.join() === (profile?.skills || []).join()}>Save Changes</Button>
      </form>

      <Card>
        <CardHeader>
          <CardTitle>
            <Upload className="inline w-4 h-4 mr-2 text-indigo-500" />Resume
            {profile?.hasResume && <span className="ml-2 inline-flex items-center gap-1 text-xs text-emerald-600 font-normal"><CheckCircle className="w-3.5 h-3.5" />Uploaded</span>}
          </CardTitle>
        </CardHeader>
        <CardContent>
          {profile?.hasResume && (
            <div className="mb-3 rounded-lg border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-700">
              Current: <strong>{profile.resumeOriginalName}</strong>
            </div>
          )}
          <label className="flex cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed border-slate-300 bg-slate-50 p-8 text-center hover:border-indigo-400 hover:bg-indigo-50 transition-colors">
            <Upload className="h-8 w-8 text-slate-400 mb-2" />
            <p className="text-sm font-medium text-slate-700">{uploadingResume ? 'Uploading...' : profile?.hasResume ? 'Replace resume' : 'Upload resume'}</p>
            <p className="text-xs text-slate-500 mt-1">PDF only • Max 5MB</p>
            <input type="file" accept=".pdf" className="sr-only" onChange={handleResumeUpload} disabled={uploadingResume} />
          </label>
        </CardContent>
      </Card>
    </div>
  );
}
