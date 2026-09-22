import { apiClient } from './client';
import type {
  UserDepartmentResponseDto,
  AssignUserDepartmentRequest,
  UserRolesResponseDto,
  AssignRolesRequest,
} from '@/types';

export const usersApi = {
  assignDepartment: async (
    userId: string,
    payload: AssignUserDepartmentRequest
  ): Promise<UserDepartmentResponseDto> => {
    return apiClient<UserDepartmentResponseDto>(`/users/${userId}/department`, {
      method: 'PUT',
      body: payload,
    });
  },

  getUserRoles: async (userId: string): Promise<UserRolesResponseDto> => {
    return apiClient<UserRolesResponseDto>(`/users/${userId}/roles`, {
      method: 'GET',
    });
  },

  assignRoles: async (
    userId: string,
    payload: AssignRolesRequest
  ): Promise<UserRolesResponseDto> => {
    return apiClient<UserRolesResponseDto>(`/users/${userId}/roles`, {
      method: 'PUT',
      body: payload,
    });
  },
};
