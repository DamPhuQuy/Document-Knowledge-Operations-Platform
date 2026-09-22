import { apiClient } from './client';
import type { Page, AuditLogResponseDto, AuditLogQueryFilter } from '@/types';

export const auditApi = {
  getAuditLogs: async (filter: AuditLogQueryFilter = {}): Promise<Page<AuditLogResponseDto>> => {
    const params = new URLSearchParams();

    if (filter.userId) params.append('userId', filter.userId);
    if (filter.action) params.append('action', filter.action);
    if (filter.resourceType) params.append('resourceType', filter.resourceType);
    if (filter.resourceId) params.append('resourceId', filter.resourceId);
    if (filter.status) params.append('status', filter.status);
    if (filter.startDate) params.append('startDate', filter.startDate);
    if (filter.endDate) params.append('endDate', filter.endDate);
    if (filter.page !== undefined) params.append('page', String(filter.page));
    if (filter.size !== undefined) params.append('size', String(filter.size));

    const queryString = params.toString();
    const endpoint = queryString ? `/audit-logs?${queryString}` : '/audit-logs';

    return apiClient<Page<AuditLogResponseDto>>(endpoint, {
      method: 'GET',
    });
  },
};
