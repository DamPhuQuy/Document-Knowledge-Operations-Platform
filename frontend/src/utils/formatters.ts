import type { AccessLevel } from '@/types';

export function formatBytes(bytes: number, decimals = 2): string {
  if (!bytes || bytes === 0) return '0 Bytes';

  const k = 1024;
  const dm = decimals < 0 ? 0 : decimals;
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB'];

  const i = Math.floor(Math.log(bytes) / Math.log(k));
  const idx = Math.min(i, sizes.length - 1);

  return `${parseFloat((bytes / Math.pow(k, idx)).toFixed(dm))} ${sizes[idx]}`;
}

export function formatDate(dateString: string): string {
  if (!dateString) return '—';
  try {
    const d = new Date(dateString);
    if (isNaN(d.getTime())) return dateString;
    return d.toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return dateString;
  }
}

export function getAccessBadgeConfig(level: AccessLevel): {
  variant: 'danger' | 'warning' | 'primary' | 'success';
  label: string;
  color: string;
} {
  switch (level) {
    case 'RESTRICTED':
      return { variant: 'danger', label: 'Restricted', color: 'red' };
    case 'CONFIDENTIAL':
      return { variant: 'warning', label: 'Confidential', color: 'orange' };
    case 'INTERNAL':
      return { variant: 'primary', label: 'Internal', color: 'blue' };
    case 'PUBLIC':
      return { variant: 'success', label: 'Public', color: 'green' };
    default:
      return { variant: 'primary', label: level, color: 'default' };
  }
}
