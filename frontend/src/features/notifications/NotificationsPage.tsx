import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { Bell, CheckCheck } from 'lucide-react';
import apiClient from '../../lib/api/client';
import { API } from '../../lib/api/endpoints';
import { Notification } from '../../types/notification';
import { ApiResponse, PagedResponse } from '../../types/api';
import { Button } from '../../components/ui/Button';
import { Skeleton } from '../../components/ui/Skeleton';
import { EmptyState } from '../../components/ui/EmptyState';
import { formatDateTime, cn } from '../../lib/utils';

export function NotificationsPage() {
  const qc = useQueryClient();
  const { data, isLoading } = useQuery<ApiResponse<PagedResponse<Notification>>>({
    queryKey: ['notifications'],
    queryFn: () => apiClient.get(API.notifications.list).then(r => r.data),
  });
  const markReadMutation = useMutation({
    mutationFn: (id: number) => apiClient.patch(API.notifications.markRead(id)),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['notifications'] }); qc.invalidateQueries({ queryKey: ['notif-count'] }); },
  });
  const markAllMutation = useMutation({
    mutationFn: () => apiClient.patch(API.notifications.markAllRead),
    onSuccess: () => { toast.success('All marked as read'); qc.invalidateQueries({ queryKey: ['notifications'] }); qc.invalidateQueries({ queryKey: ['notif-count'] }); },
  });
  const notifs = data?.data?.content ?? [];

  return (
    <div className="max-w-2xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">Notifications</h1>
        {notifs.some(n => !n.read) && (
          <Button size="sm" variant="outline" onClick={() => markAllMutation.mutate()} isLoading={markAllMutation.isPending}>
            <CheckCheck className="w-4 h-4 mr-1.5" />Mark all read
          </Button>
        )}
      </div>
      {isLoading ? <Skeleton className="h-64 rounded-xl" /> : notifs.length === 0 ? (
        <EmptyState icon={<Bell className="w-8 h-8" />} title="No notifications" description="You're all caught up!" />
      ) : (
        <div className="space-y-2">
          {notifs.map(n => (
            <div key={n.id} className={cn('rounded-xl border p-4 transition-colors cursor-pointer', n.read ? 'border-slate-200 bg-white' : 'border-indigo-200 bg-indigo-50')}
              onClick={() => !n.read && markReadMutation.mutate(n.notificationId)}>
              <div className="flex items-start justify-between gap-3">
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    {!n.read && <div className="w-2 h-2 rounded-full bg-indigo-600 shrink-0" />}
                    <p className={cn('text-sm font-semibold', n.read ? 'text-slate-700' : 'text-indigo-900')}>{n.title}</p>
                    {n.priority === 'URGENT' && <span className="text-xs text-rose-600 font-medium bg-rose-50 px-1.5 py-0.5 rounded">URGENT</span>}
                  </div>
                  <p className="text-sm text-slate-500 mt-1">{n.message}</p>
                  <p className="text-xs text-slate-400 mt-2">{formatDateTime(n.createdAt)}</p>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
