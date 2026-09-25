import { apiClient } from './client';
import type {
  DocumentResponseDto,
  DocumentVersionResponseDto,
  DocumentPermissionsResponseDto,
  UpdateDocumentPermissionsRequest,
  UploadDocumentParams,
  UploadVersionParams,
} from '@/types';

export const documentsApi = {
  uploadDocument: async (params: UploadDocumentParams): Promise<DocumentResponseDto> => {
    const formData = new FormData();
    formData.append('file', params.file);
    if (params.title) {
      formData.append('title', params.title);
    }
    if (params.description) {
      formData.append('description', params.description);
    }
    if (params.accessLevel) {
      formData.append('accessLevel', params.accessLevel);
    }
    const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
    if (params.departmentId && UUID_REGEX.test(params.departmentId.trim())) {
      formData.append('departmentId', params.departmentId.trim());
    }

    return apiClient<DocumentResponseDto>('/documents', {
      method: 'POST',
      body: formData,
    });
  },

  uploadVersion: async (
    documentId: string,
    params: UploadVersionParams
  ): Promise<DocumentVersionResponseDto> => {
    const formData = new FormData();
    formData.append('file', params.file);
    if (params.changeSummary) {
      formData.append('changeSummary', params.changeSummary);
    }

    return apiClient<DocumentVersionResponseDto>(`/documents/${documentId}/versions`, {
      method: 'POST',
      body: formData,
    });
  },

  getPermissions: async (documentId: string): Promise<DocumentPermissionsResponseDto> => {
    return apiClient<DocumentPermissionsResponseDto>(`/documents/${documentId}/permissions`, {
      method: 'GET',
    });
  },

  updatePermissions: async (
    documentId: string,
    payload: UpdateDocumentPermissionsRequest
  ): Promise<DocumentPermissionsResponseDto> => {
    return apiClient<DocumentPermissionsResponseDto>(`/documents/${documentId}/permissions`, {
      method: 'PUT',
      body: payload,
    });
  },

  deleteDocument: async (documentId: string): Promise<void> => {
    return apiClient<void>(`/documents/${documentId}`, {
      method: 'DELETE',
    });
  },
};
