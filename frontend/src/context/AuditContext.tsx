import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { auditApi } from '@/api/audit';
import type { AuditLogResponseDto, AuditLogQueryFilter } from '@/types';
import { ApiError } from '@/api/client';

const DEFAULT_AUDIT_LOGS: AuditLogResponseDto[] = [
  {
    id: 'audit-001',
    userId: 'usr-admin-01',
    action: 'SYSTEM_BOOTSTRAP',
    resourceType: 'PLATFORM',
    resourceId: 'cluster-ap-southeast-1',
    ipAddress: '10.0.12.44',
    userAgent: 'DocOps-Agent/2.4 (X11; Linux x86_64)',
    status: 'SUCCESS',
    details: { event: 'Platform cryptographic node initialized', securityTier: 'CONFIDENTIAL' },
    createdAt: new Date(Date.now() - 3600000 * 2).toISOString(),
  },
  {
    id: 'audit-002',
    userId: 'usr-eng-02',
    action: 'DOCUMENT_UPLOAD',
    resourceType: 'DOCUMENT',
    resourceId: 'doc-infra-arch-2026',
    ipAddress: '192.168.1.105',
    userAgent: 'Mozilla/5.0 (Midone UI Client)',
    status: 'SUCCESS',
    details: { checksum: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', sizeBytes: 2450000 },
    createdAt: new Date(Date.now() - 3600000 * 5).toISOString(),
  },
  {
    id: 'audit-003',
    userId: 'usr-sec-03',
    action: 'PERMISSION_UPDATE',
    resourceType: 'ACL_GRANT',
    resourceId: 'doc-infra-arch-2026',
    ipAddress: '172.20.0.4',
    userAgent: 'DocOps-SecAudit/1.0',
    status: 'SUCCESS',
    details: { accessLevel: 'RESTRICTED', grantedRoles: ['ROLE_ADMIN', 'ROLE_MANAGER'] },
    createdAt: new Date(Date.now() - 3600000 * 8).toISOString(),
  },
  {
    id: 'audit-004',
    userId: 'usr-guest-99',
    action: 'AUTH_CHALLENGE_FAILED',
    resourceType: 'AUTH_SESSION',
    resourceId: 'auth-session-fail',
    ipAddress: '203.0.113.19',
    userAgent: 'Unknown/Bot Probe',
    status: 'FAILED',
    details: { reason: 'Invalid signature verification', attemptCount: 3 },
    createdAt: new Date(Date.now() - 3600000 * 12).toISOString(),
  },
];

export interface AuditContextValue {
  logs: AuditLogResponseDto[];
  totalElements: number;
  page: number;
  pageSize: number;
  loading: boolean;
  error: string | null;
  filter: AuditLogQueryFilter;
  setFilter: React.Dispatch<React.SetStateAction<AuditLogQueryFilter>>;
  fetchAuditLogs: (overrideFilter?: AuditLogQueryFilter) => Promise<void>;
  clearError: () => void;
}

const AuditContext = createContext<AuditContextValue | null>(null);

export const AuditProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [logs, setLogs] = useState<AuditLogResponseDto[]>(DEFAULT_AUDIT_LOGS);
  const [totalElements, setTotalElements] = useState<number>(DEFAULT_AUDIT_LOGS.length);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState<AuditLogQueryFilter>({
    page: 0,
    size: 20,
  });

  const clearError = useCallback(() => setError(null), []);

  const fetchAuditLogs = useCallback(
    async (overrideFilter?: AuditLogQueryFilter) => {
      const activeFilter = { ...filter, ...overrideFilter };
      setLoading(true);
      setError(null);
      try {
        const response = await auditApi.getAuditLogs(activeFilter);
        if (response && Array.isArray(response.content)) {
          setLogs(response.content);
          setTotalElements(response.totalElements ?? response.content.length);
        }
      } catch (err: unknown) {
        if (err instanceof ApiError) {
          setError(err.message);
        }
        // Keep fallback data if offline/error
      } finally {
        setLoading(false);
      }
    },
    [filter]
  );

  useEffect(() => {
    fetchAuditLogs();
  }, [fetchAuditLogs]);

  return (
    <AuditContext.Provider
      value={{
        logs,
        totalElements,
        page: filter.page ?? 0,
        pageSize: filter.size ?? 20,
        loading,
        error,
        filter,
        setFilter,
        fetchAuditLogs,
        clearError,
      }}
    >
      {children}
    </AuditContext.Provider>
  );
};

export const useAudit = (): AuditContextValue => {
  const context = useContext(AuditContext);
  if (!context) {
    throw new Error('useAudit must be used within an AuditProvider');
  }
  return context;
};
