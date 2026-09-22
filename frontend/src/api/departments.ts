import { apiClient } from './client';
import type {
  DepartmentResponseDto,
  CreateDepartmentRequest,
  UpdateDepartmentRequest,
} from '@/types';

export const departmentsApi = {
  listDepartments: async (): Promise<DepartmentResponseDto[]> => {
    return apiClient<DepartmentResponseDto[]>('/departments', {
      method: 'GET',
    });
  },

  getDepartmentById: async (id: string): Promise<DepartmentResponseDto> => {
    return apiClient<DepartmentResponseDto>(`/departments/${id}`, {
      method: 'GET',
    });
  },

  createDepartment: async (payload: CreateDepartmentRequest): Promise<DepartmentResponseDto> => {
    return apiClient<DepartmentResponseDto>('/departments', {
      method: 'POST',
      body: payload,
    });
  },

  updateDepartment: async (
    id: string,
    payload: UpdateDepartmentRequest
  ): Promise<DepartmentResponseDto> => {
    return apiClient<DepartmentResponseDto>(`/departments/${id}`, {
      method: 'PUT',
      body: payload,
    });
  },
};
