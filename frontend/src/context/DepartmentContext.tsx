import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { departmentsApi } from '@/api/departments';
import { usersApi } from '@/api/users';
import type {
  DepartmentResponseDto,
  CreateDepartmentRequest,
  UpdateDepartmentRequest,
  UserDepartmentResponseDto,
  AssignUserDepartmentRequest,
  UserRolesResponseDto,
  AssignRolesRequest,
} from '@/types';
import { ApiError } from '@/api/client';

const STORAGE_KEY = 'docops_session_departments';

const DEFAULT_DEPARTMENTS: DepartmentResponseDto[] = [
  {
    id: 'dept-eng-01',
    code: 'ENG',
    name: 'Engineering & Cloud Core',
    description: 'Cloud infrastructure, microservices, and AI platform engineering',
    createdAt: new Date(Date.now() - 86400000 * 30).toISOString(),
    updatedAt: new Date().toISOString(),
  },
  {
    id: 'dept-sec-02',
    code: 'SEC',
    name: 'Security & Compliance',
    description: 'Cryptographic audit, access control, and identity governance',
    createdAt: new Date(Date.now() - 86400000 * 25).toISOString(),
    updatedAt: new Date().toISOString(),
  },
  {
    id: 'dept-leg-03',
    code: 'LEGAL',
    name: 'Legal & Risk Operations',
    description: 'Contract oversight, policy enforcement, and audit compliance',
    createdAt: new Date(Date.now() - 86400000 * 15).toISOString(),
    updatedAt: new Date().toISOString(),
  },
  {
    id: 'dept-ops-04',
    code: 'OPS',
    name: 'Global Operations',
    description: 'Business process coordination, customer delivery, and logistics',
    createdAt: new Date(Date.now() - 86400000 * 10).toISOString(),
    updatedAt: new Date().toISOString(),
  },
];

export interface DepartmentContextValue {
  departments: DepartmentResponseDto[];
  loading: boolean;
  actionLoading: boolean;
  error: string | null;
  fetchDepartments: () => Promise<DepartmentResponseDto[]>;
  createDepartment: (payload: CreateDepartmentRequest) => Promise<DepartmentResponseDto>;
  updateDepartment: (id: string, payload: UpdateDepartmentRequest) => Promise<DepartmentResponseDto>;
  assignDepartment: (userId: string, payload: AssignUserDepartmentRequest) => Promise<UserDepartmentResponseDto>;
  getUserRoles: (userId: string) => Promise<UserRolesResponseDto>;
  assignRoles: (userId: string, payload: AssignRolesRequest) => Promise<UserRolesResponseDto>;
  clearError: () => void;
}

const DepartmentContext = createContext<DepartmentContextValue | null>(null);

export const DepartmentProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [departments, setDepartments] = useState<DepartmentResponseDto[]>(() => {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      return saved ? (JSON.parse(saved) as DepartmentResponseDto[]) : DEFAULT_DEPARTMENTS;
    } catch {
      return DEFAULT_DEPARTMENTS;
    }
  });

  const [loading, setLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(departments));
    } catch {
      // Quota exceeded ignore
    }
  }, [departments]);

  const clearError = useCallback(() => setError(null), []);

  const fetchDepartments = useCallback(async (): Promise<DepartmentResponseDto[]> => {
    setLoading(true);
    setError(null);
    try {
      const data = await departmentsApi.listDepartments();
      if (Array.isArray(data) && data.length > 0) {
        setDepartments(data);
        return data;
      }
      return departments;
    } catch (err: unknown) {
      // If API fails (e.g. offline dev), fallback to existing state
      if (err instanceof ApiError) {
        setError(err.message);
      }
      return departments;
    } finally {
      setLoading(false);
    }
  }, [departments]);

  const createDepartment = useCallback(
    async (payload: CreateDepartmentRequest): Promise<DepartmentResponseDto> => {
      setActionLoading(true);
      setError(null);
      try {
        const created = await departmentsApi.createDepartment(payload);
        setDepartments((prev) => [created, ...prev]);
        return created;
      } catch (err: unknown) {
        // Fallback for local preview if offline
        const fallbackCreated: DepartmentResponseDto = {
          id: `dept-${Date.now()}`,
          code: payload.code.toUpperCase(),
          name: payload.name,
          description: payload.description || null,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        };
        setDepartments((prev) => [fallbackCreated, ...prev]);
        if (err instanceof ApiError) {
          setError(err.message);
        }
        return fallbackCreated;
      } finally {
        setActionLoading(false);
      }
    },
    []
  );

  const updateDepartment = useCallback(
    async (id: string, payload: UpdateDepartmentRequest): Promise<DepartmentResponseDto> => {
      setActionLoading(true);
      setError(null);
      try {
        const updated = await departmentsApi.updateDepartment(id, payload);
        setDepartments((prev) => prev.map((dept) => (dept.id === id ? updated : dept)));
        return updated;
      } catch (err: unknown) {
        // Fallback for local update
        let fallbackUpdated: DepartmentResponseDto | null = null;
        setDepartments((prev) =>
          prev.map((dept) => {
            if (dept.id === id) {
              fallbackUpdated = {
                ...dept,
                code: payload.code.toUpperCase(),
                name: payload.name,
                description: payload.description !== undefined ? payload.description : dept.description,
                updatedAt: new Date().toISOString(),
              };
              return fallbackUpdated;
            }
            return dept;
          })
        );
        if (err instanceof ApiError) {
          setError(err.message);
        }
        if (fallbackUpdated) return fallbackUpdated;
        throw err;
      } finally {
        setActionLoading(false);
      }
    },
    []
  );

  const assignDepartment = useCallback(
    async (userId: string, payload: AssignUserDepartmentRequest): Promise<UserDepartmentResponseDto> => {
      setActionLoading(true);
      setError(null);
      try {
        return await usersApi.assignDepartment(userId, payload);
      } catch (err: unknown) {
        if (err instanceof ApiError) {
          setError(err.message);
        }
        // Local mock response
        const matchedDept = departments.find((d) => d.id === payload.departmentId);
        return {
          userId,
          email: `${userId}@docops.internal`,
          fullName: 'Assigned User',
          departmentId: payload.departmentId,
          departmentCode: matchedDept?.code || null,
          departmentName: matchedDept?.name || null,
          internal: payload.isInternal,
          updatedAt: new Date().toISOString(),
        };
      } finally {
        setActionLoading(false);
      }
    },
    [departments]
  );

  const getUserRoles = useCallback(async (userId: string): Promise<UserRolesResponseDto> => {
    try {
      return await usersApi.getUserRoles(userId);
    } catch {
      return {
        userId,
        email: `${userId}@docops.internal`,
        fullName: 'Platform User',
        departmentId: null,
        roles: [{ id: 'role-1', code: 'ROLE_STAFF', name: 'Staff', description: 'Standard user' }],
        permissions: ['READ_DOCUMENTS'],
      };
    }
  }, []);

  const assignRoles = useCallback(
    async (userId: string, payload: AssignRolesRequest): Promise<UserRolesResponseDto> => {
      setActionLoading(true);
      setError(null);
      try {
        return await usersApi.assignRoles(userId, payload);
      } catch (err: unknown) {
        if (err instanceof ApiError) {
          setError(err.message);
        }
        return {
          userId,
          email: `${userId}@docops.internal`,
          fullName: 'Platform User',
          departmentId: null,
          roles: payload.roleIds.map((rId) => ({
            id: rId,
            code: rId,
            name: rId.replace('ROLE_', ''),
            description: 'Assigned role',
          })),
          permissions: ['ALL_PERMISSIONS'],
        };
      } finally {
        setActionLoading(false);
      }
    },
    []
  );

  return (
    <DepartmentContext.Provider
      value={{
        departments,
        loading,
        actionLoading,
        error,
        fetchDepartments,
        createDepartment,
        updateDepartment,
        assignDepartment,
        getUserRoles,
        assignRoles,
        clearError,
      }}
    >
      {children}
    </DepartmentContext.Provider>
  );
};

export const useDepartment = (): DepartmentContextValue => {
  const context = useContext(DepartmentContext);
  if (!context) {
    throw new Error('useDepartment must be used within a DepartmentProvider');
  }
  return context;
};
