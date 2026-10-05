export type NotificationPriority = 'NORMAL' | 'URGENT';
export type NotificationType =
  | 'DRIVE_PUBLISHED'
  | 'DRIVE_UPDATED'
  | 'DEADLINE_APPROACHING'
  | 'STATUS_CHANGED'
  | 'APPLICATION_CONFIRMED'
  | 'DRIVE_REMINDER'
  | 'CUSTOM_BROADCAST';

export interface Notification {
  id: number;
  notificationId: number;
  title: string;
  message: string;
  priority: NotificationPriority;
  type: NotificationType;
  targetDriveId: number | null;
  read: boolean;
  readAt: string | null;
  createdAt: string;
}
