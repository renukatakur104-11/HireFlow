import React from 'react';
import { cn } from '@/lib/utils';

export type RoundStatusType =
  | 'REGISTERED'
  | 'ROUND_1'
  | 'ROUND_2'
  | 'TECHNICAL_INTERVIEW'
  | 'HR_INTERVIEW'
  | 'SELECTED'
  | 'REJECTED'
  | 'WITHDRAWN';

export type DriveStatusType = 'DRAFT' | 'OPEN' | 'CLOSED' | 'COMPLETED';

interface StatusBadgeProps {
  status: RoundStatusType | DriveStatusType | string;
  className?: string;
  pulse?: boolean;
}

const statusConfig: Record<
  string,
  { label: string; bg: string; text: string; border: string; dot: string }
> = {
  // Application Statuses
  REGISTERED: {
    label: 'Registered',
    bg: 'bg-slate-50',
    text: 'text-slate-700',
    border: 'border-slate-300',
    dot: 'bg-slate-400',
  },
  ROUND_1: {
    label: 'Round 1 (OA)',
    bg: 'bg-sky-50',
    text: 'text-sky-700',
    border: 'border-sky-300',
    dot: 'bg-sky-500',
  },
  ROUND_2: {
    label: 'Round 2',
    bg: 'bg-cyan-50',
    text: 'text-cyan-700',
    border: 'border-cyan-300',
    dot: 'bg-cyan-500',
  },
  TECHNICAL_INTERVIEW: {
    label: 'Technical Interview',
    bg: 'bg-purple-50',
    text: 'text-purple-700',
    border: 'border-purple-300',
    dot: 'bg-purple-500',
  },
  HR_INTERVIEW: {
    label: 'HR Interview',
    bg: 'bg-indigo-50',
    text: 'text-indigo-700',
    border: 'border-indigo-300',
    dot: 'bg-indigo-500',
  },
  SELECTED: {
    label: 'Selected / Placed',
    bg: 'bg-emerald-50',
    text: 'text-emerald-700',
    border: 'border-emerald-300',
    dot: 'bg-emerald-500',
  },
  REJECTED: {
    label: 'Rejected',
    bg: 'bg-rose-50',
    text: 'text-rose-700',
    border: 'border-rose-300',
    dot: 'bg-rose-500',
  },
  WITHDRAWN: {
    label: 'Withdrawn',
    bg: 'bg-amber-50',
    text: 'text-amber-700',
    border: 'border-amber-300',
    dot: 'bg-amber-500',
  },

  // Drive Statuses
  DRAFT: {
    label: 'Draft',
    bg: 'bg-slate-100',
    text: 'text-slate-600',
    border: 'border-slate-200',
    dot: 'bg-slate-400',
  },
  OPEN: {
    label: 'Open for Applications',
    bg: 'bg-emerald-50',
    text: 'text-emerald-700',
    border: 'border-emerald-300',
    dot: 'bg-emerald-500',
  },
  CLOSED: {
    label: 'Applications Closed',
    bg: 'bg-rose-50',
    text: 'text-rose-700',
    border: 'border-rose-300',
    dot: 'bg-rose-500',
  },
  COMPLETED: {
    label: 'Drive Completed',
    bg: 'bg-blue-50',
    text: 'text-blue-700',
    border: 'border-blue-300',
    dot: 'bg-blue-500',
  },
};

export const StatusBadge: React.FC<StatusBadgeProps> = ({
  status,
  className,
  pulse = false,
}) => {
  const config = statusConfig[status] || {
    label: status.replace(/_/g, ' '),
    bg: 'bg-slate-50',
    text: 'text-slate-700',
    border: 'border-slate-200',
    dot: 'bg-slate-400',
  };

  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold border select-none',
        config.bg,
        config.text,
        config.border,
        className
      )}
    >
      <span
        className={cn(
          'w-1.5 h-1.5 rounded-full',
          config.dot,
          pulse && 'animate-ping'
        )}
      />
      {config.label}
    </span>
  );
};
