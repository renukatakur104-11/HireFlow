import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { Send } from 'lucide-react';
import apiClient from '../../../lib/api/client';
import { API } from '../../../lib/api/endpoints';
import { Button } from '../../../components/ui/Button';
import { Input } from '../../../components/ui/Input';
import { Card, CardContent, CardHeader, CardTitle } from '../../../components/ui/Card';

export function AdminNotificationsPage() {
  const [title, setTitle] = useState('');
  const [message, setMessage] = useState('');
  const [priority, setPriority] = useState<'NORMAL'|'URGENT'>('NORMAL');
  const [audience, setAudience] = useState('ALL');
  const [previewCount, setPreviewCount] = useState<number|null>(null);
  const previewMutation = useMutation({
    mutationFn: () => apiClient.post(API.admin.notifications.previewCount, { title, message, priority, audience }).then(r=>r.data),
    onSuccess: (d) => setPreviewCount(d.data),
    onError: () => toast.error('Preview failed'),
  });
  const broadcastMutation = useMutation({
    mutationFn: () => apiClient.post(API.admin.notifications.broadcast, { title, message, priority, audience }),
    onSuccess: () => { toast.success('Notification sent!'); setTitle(''); setMessage(''); setPreviewCount(null); },
    onError: () => toast.error('Send failed'),
  });
  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div><h1 className="text-2xl font-bold text-slate-900">Send Notification</h1><p className="text-sm text-slate-500">Target students with placement updates</p></div>
      <Card>
        <CardHeader><CardTitle>Compose Message</CardTitle></CardHeader>
        <CardContent>
          <div className="space-y-4">
            <Input label="Title" value={title} onChange={e=>setTitle(e.target.value)} placeholder="e.g. New Drive Published" />
            <div className="space-y-1.5">
              <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">Message</label>
              <textarea className="flex min-h-[120px] w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm" value={message} onChange={e=>setMessage(e.target.value)} placeholder="Write your message..." />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">Priority</label>
                <select className="flex h-10 w-full rounded-lg border border-slate-300 bg-white px-3 text-sm" value={priority} onChange={e=>setPriority(e.target.value as 'NORMAL'|'URGENT')}>
                  <option value="NORMAL">Normal</option><option value="URGENT">Urgent</option>
                </select>
              </div>
              <div className="space-y-1.5">
                <label className="text-xs font-semibold uppercase tracking-wider text-slate-700">Audience</label>
                <select className="flex h-10 w-full rounded-lg border border-slate-300 bg-white px-3 text-sm" value={audience} onChange={e=>setAudience(e.target.value)}>
                  <option value="ALL">All Students</option>
                  <option value="DRIVE_ELIGIBLE">Drive Eligible</option>
                  <option value="DRIVE_APPLICANTS">Drive Applicants</option>
                </select>
              </div>
            </div>
            {previewCount!==null&&<div className="rounded-lg border border-indigo-200 bg-indigo-50 p-3 text-sm text-indigo-700">Will be sent to <strong>{previewCount}</strong> recipients.</div>}
            <div className="flex gap-3">
              <Button variant="outline" onClick={()=>previewMutation.mutate()} isLoading={previewMutation.isPending} disabled={!title||!message}>Preview Audience</Button>
              <Button onClick={()=>broadcastMutation.mutate()} isLoading={broadcastMutation.isPending} disabled={!title||!message} leftIcon={<Send className="w-4 h-4" />}>Send Now</Button>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
