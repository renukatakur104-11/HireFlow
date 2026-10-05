import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { Sparkles, Loader2 } from 'lucide-react';
import apiClient from '../../../lib/api/client';
import { API } from '../../../lib/api/endpoints';
import { Drive } from '../../../types/drive';
import { Modal } from '../../../components/ui/Modal';
import { Button } from '../../../components/ui/Button';
import { Input } from '../../../components/ui/Input';

const driveSchema = z.object({
  companyName: z.string().min(1, 'Company name is required'),
  roleTitle: z.string().min(1, 'Role title is required'),
  description: z.string().min(10, 'Description must be at least 10 characters'),
  packageLpa: z.coerce.number().min(0, 'Package must be positive'),
  location: z.string().min(1, 'Location is required'),
  minCgpa: z.coerce.number().min(0).max(10).default(0),
  maxBacklogs: z.coerce.number().min(0).default(0),
  driveDate: z.string().min(1, 'Drive date is required'),
  applicationDeadline: z.string().min(1, 'Deadline is required'),
  status: z.string().default('DRAFT'),
});
type DriveFormValues = z.infer<typeof driveSchema>;

interface DriveFormModalProps {
  drive: Drive | null;
  onClose: () => void;
  onSuccess: () => void;
}

export function DriveFormModal({ drive, onClose, onSuccess }: DriveFormModalProps) {
  const isEdit = !!drive;
  const [showAiParser, setShowAiParser] = useState(false);
  const [rawJd, setRawJd] = useState('');

  const { register, handleSubmit, reset, setValue, formState: { errors } } = useForm<DriveFormValues>({
    resolver: zodResolver(driveSchema),
    defaultValues: { minCgpa: 0, maxBacklogs: 0, status: 'DRAFT' },
  });

  useEffect(() => {
    if (drive) {
      reset({
        companyName: drive.companyName,
        roleTitle: drive.roleTitle,
        description: drive.description,
        packageLpa: drive.packageLpa,
        location: drive.location,
        minCgpa: drive.minCgpa,
        maxBacklogs: drive.maxBacklogs,
        driveDate: drive.driveDate.slice(0, 16),
        applicationDeadline: drive.applicationDeadline.slice(0, 16),
        status: drive.status,
      });
    }
  }, [drive, reset]);

  const mutation = useMutation({
    mutationFn: (data: DriveFormValues) => {
      const payload = {
        ...data,
        driveDate: new Date(data.driveDate).toISOString(),
        applicationDeadline: new Date(data.applicationDeadline).toISOString(),
      };
      return isEdit ? apiClient.put(API.admin.drives.detail(drive!.id), payload) : apiClient.post(API.admin.drives.create, payload);
    },
    onSuccess: () => {
      toast.success(isEdit ? 'Drive updated!' : 'Drive created!');
      onSuccess();
    },
    onError: (err: unknown) => {
      const e = err as { response?: { data?: { message?: string } } };
      toast.error(e.response?.data?.message || 'Failed');
    },
  });

  const aiAnalyzeMutation = useMutation({
    mutationFn: (jobDescription: string) =>
      apiClient.post(API.admin.ai.analyzeJd, { jobDescription }).then((r) => r.data.data),
    onSuccess: (data) => {
      if (data.extractedRole) setValue('roleTitle', data.extractedRole, { shouldValidate: true });
      if (data.summary || data.keyRequirements?.length) {
        const fullDesc = [data.summary, data.keyRequirements ? '\nKey Requirements:\n- ' + data.keyRequirements.join('\n- ') : '']
          .filter(Boolean)
          .join('\n');
        setValue('description', fullDesc || rawJd, { shouldValidate: true });
      } else if (rawJd) {
        setValue('description', rawJd, { shouldValidate: true });
      }
      if (data.suggestedMinCgpa) setValue('minCgpa', data.suggestedMinCgpa, { shouldValidate: true });
      toast.success(data.aiGenerated ? 'AI extracted criteria from JD!' : 'Extracted criteria using rule-based parser');
      setShowAiParser(false);
    },
    onError: () => toast.error('Failed to analyze JD with AI'),
  });

  return (
    <Modal
      isOpen
      title={isEdit ? 'Edit Drive' : 'Create Drive'}
      onClose={onClose}
      maxWidth="lg"
      footer={
        <>
          <Button variant="secondary" size="sm" onClick={onClose}>Cancel</Button>
          <Button size="sm" isLoading={mutation.isPending} onClick={handleSubmit((d) => mutation.mutate(d))}>
            {isEdit ? 'Save Changes' : 'Create Drive'}
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        {!isEdit && (
          <div className="rounded-lg border border-indigo-100 bg-indigo-50/70 p-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Sparkles className="w-4 h-4 text-indigo-600" />
                <span className="text-xs font-semibold text-indigo-900">AI Job Description Assistant</span>
              </div>
              <button
                type="button"
                onClick={() => setShowAiParser(!showAiParser)}
                className="text-xs text-indigo-600 hover:text-indigo-800 font-medium underline"
              >
                {showAiParser ? 'Hide' : 'Auto-fill from JD text'}
              </button>
            </div>
            {showAiParser && (
              <div className="mt-3 space-y-2">
                <textarea
                  className="w-full text-xs rounded border border-indigo-200 bg-white p-2 min-h-[90px] focus:outline-none focus:ring-1 focus:ring-indigo-500"
                  placeholder="Paste the company's full job description or email here..."
                  value={rawJd}
                  onChange={(e) => setRawJd(e.target.value)}
                />
                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  disabled={!rawJd.trim()}
                  isLoading={aiAnalyzeMutation.isPending}
                  onClick={() => aiAnalyzeMutation.mutate(rawJd)}
                  leftIcon={aiAnalyzeMutation.isPending ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Sparkles className="w-3.5 h-3.5" />}
                >
                  Analyze & Auto-fill Form
                </Button>
              </div>
            )}
          </div>
        )}

        <form className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input label="Company Name" error={errors.companyName?.message} {...register('companyName')} />
            <Input label="Role Title" error={errors.roleTitle?.message} {...register('roleTitle')} />
            <Input label="Location" error={errors.location?.message} {...register('location')} />
            <Input label="Package (LPA)" type="number" step="0.1" error={errors.packageLpa?.message} {...register('packageLpa')} />
            <Input label="Min CGPA" type="number" step="0.1" error={errors.minCgpa?.message} {...register('minCgpa')} />
            <Input label="Max Backlogs" type="number" error={errors.maxBacklogs?.message} {...register('maxBacklogs')} />
            <Input label="Drive Date" type="datetime-local" error={errors.driveDate?.message} {...register('driveDate')} />
            <Input label="Application Deadline" type="datetime-local" error={errors.applicationDeadline?.message} {...register('applicationDeadline')} />
          </div>
          <div className="space-y-1.5">
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-700">Description</label>
            <textarea className="flex min-h-[100px] w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm focus:outline-none focus:border-indigo-500" {...register('description')} />
            {errors.description && <p className="text-xs text-rose-600">{errors.description.message}</p>}
          </div>
          <div className="space-y-1.5">
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-700">Status</label>
            <select className="flex h-10 w-full rounded-lg border border-slate-300 bg-white px-3 text-sm focus:outline-none focus:border-indigo-500" {...register('status')}>
              <option value="DRAFT">Draft</option>
              <option value="OPEN">Open</option>
              <option value="CLOSED">Closed</option>
              <option value="COMPLETED">Completed</option>
            </select>
          </div>
        </form>
      </div>
    </Modal>
  );
}
