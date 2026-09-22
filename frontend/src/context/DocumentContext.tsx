import React, { createContext, useContext, useState, useEffect, useCallback, useMemo } from 'react';
import { documentsApi } from '@/api/documents';
import type {
  DocumentResponseDto,
  DocumentVersionResponseDto,
  DocumentPermissionsResponseDto,
  UpdateDocumentPermissionsRequest,
  UploadDocumentParams,
  UploadVersionParams,
} from '@/types';
import { ApiError } from '@/api/client';

const STORAGE_KEY = 'docops_session_documents';

export interface DocumentContextValue {
  documents: DocumentResponseDto[];
  isLoading: boolean;
  error: string | null;
  documentCount: number;
  totalStorageBytes: number;
  highSecurityCount: number;
  uniqueDepartmentsCount: number;
  addDocument: (doc: DocumentResponseDto) => void;
  removeDocument: (id: string) => void;
  updateDocument: (id: string, partial: Partial<DocumentResponseDto>) => void;
  uploadDocument: (params: UploadDocumentParams) => Promise<DocumentResponseDto>;
  uploadVersion: (documentId: string, params: UploadVersionParams) => Promise<DocumentVersionResponseDto>;
  getPermissions: (documentId: string) => Promise<DocumentPermissionsResponseDto>;
  updatePermissions: (documentId: string, payload: UpdateDocumentPermissionsRequest) => Promise<DocumentPermissionsResponseDto>;
  deleteDocument: (documentId: string) => Promise<void>;
  clearError: () => void;
}

const DocumentContext = createContext<DocumentContextValue | null>(null);

export const DocumentProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [documents, setDocuments] = useState<DocumentResponseDto[]>(() => {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      return saved ? (JSON.parse(saved) as DocumentResponseDto[]) : [];
    } catch {
      return [];
    }
  });

  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(documents));
    } catch {
      // Quota exceeded
    }
  }, [documents]);

  const documentCount = useMemo(() => documents.length, [documents]);

  const totalStorageBytes = useMemo(() => {
    return documents.reduce((acc, doc) => acc + (doc.fileSizeBytes || 0), 0);
  }, [documents]);

  const highSecurityCount = useMemo(() => {
    return documents.filter((d) => d.accessLevel === 'RESTRICTED' || d.accessLevel === 'CONFIDENTIAL').length;
  }, [documents]);

  const uniqueDepartmentsCount = useMemo(() => {
    const deptIds = new Set(documents.map((d) => d.departmentId).filter(Boolean));
    return deptIds.size;
  }, [documents]);

  const addDocument = useCallback((doc: DocumentResponseDto) => {
    setDocuments((prev) => {
      const filtered = prev.filter((d) => d.id !== doc.id);
      return [doc, ...filtered];
    });
  }, []);

  const removeDocument = useCallback((id: string) => {
    setDocuments((prev) => prev.filter((d) => d.id !== id));
  }, []);

  const updateDocument = useCallback((id: string, partial: Partial<DocumentResponseDto>) => {
    setDocuments((prev) =>
      prev.map((d) => (d.id === id ? { ...d, ...partial, updatedAt: new Date().toISOString() } : d))
    );
  }, []);

  const uploadDocument = useCallback(async (params: UploadDocumentParams): Promise<DocumentResponseDto> => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await documentsApi.uploadDocument(params);
      addDocument(res);
      return res;
    } catch (err: unknown) {
      const msg = err instanceof ApiError ? err.message : 'Failed to upload document.';
      setError(msg);
      throw err;
    } finally {
      setIsLoading(false);
    }
  }, [addDocument]);

  const uploadVersion = useCallback(
    async (documentId: string, params: UploadVersionParams): Promise<DocumentVersionResponseDto> => {
      setIsLoading(true);
      setError(null);
      try {
        const res = await documentsApi.uploadVersion(documentId, params);
        updateDocument(documentId, {
          fileSizeBytes: res.fileSizeBytes,
          checksumSha256: res.checksumSha256,
          storageKey: res.storageKey,
          updatedAt: res.createdAt,
        });
        return res;
      } catch (err: unknown) {
        const msg = err instanceof ApiError ? err.message : 'Failed to upload document version.';
        setError(msg);
        throw err;
      } finally {
        setIsLoading(false);
      }
    },
    [updateDocument]
  );

  const getPermissions = useCallback(async (documentId: string): Promise<DocumentPermissionsResponseDto> => {
    return documentsApi.getPermissions(documentId);
  }, []);

  const updatePermissions = useCallback(
    async (documentId: string, payload: UpdateDocumentPermissionsRequest): Promise<DocumentPermissionsResponseDto> => {
      setIsLoading(true);
      setError(null);
      try {
        const res = await documentsApi.updatePermissions(documentId, payload);
        updateDocument(documentId, {
          accessLevel: res.accessLevel,
        });
        return res;
      } catch (err: unknown) {
        const msg = err instanceof ApiError ? err.message : 'Failed to update document permissions.';
        setError(msg);
        throw err;
      } finally {
        setIsLoading(false);
      }
    },
    [updateDocument]
  );

  const deleteDocument = useCallback(
    async (documentId: string): Promise<void> => {
      setIsLoading(true);
      setError(null);
      try {
        await documentsApi.deleteDocument(documentId);
        removeDocument(documentId);
      } catch (err: unknown) {
        const msg = err instanceof ApiError ? err.message : 'Failed to delete document.';
        setError(msg);
        throw err;
      } finally {
        setIsLoading(false);
      }
    },
    [removeDocument]
  );

  const clearError = useCallback(() => {
    setError(null);
  }, []);

  const value = useMemo<DocumentContextValue>(
    () => ({
      documents,
      isLoading,
      error,
      documentCount,
      totalStorageBytes,
      highSecurityCount,
      uniqueDepartmentsCount,
      addDocument,
      removeDocument,
      updateDocument,
      uploadDocument,
      uploadVersion,
      getPermissions,
      updatePermissions,
      deleteDocument,
      clearError,
    }),
    [
      documents,
      isLoading,
      error,
      documentCount,
      totalStorageBytes,
      highSecurityCount,
      uniqueDepartmentsCount,
      addDocument,
      removeDocument,
      updateDocument,
      uploadDocument,
      uploadVersion,
      getPermissions,
      updatePermissions,
      deleteDocument,
      clearError,
    ]
  );

  return <DocumentContext.Provider value={value}>{children}</DocumentContext.Provider>;
};

export function useDocuments(): DocumentContextValue {
  const ctx = useContext(DocumentContext);
  if (!ctx) {
    throw new Error('useDocuments must be used within a DocumentProvider');
  }
  return ctx;
}

export const useDocument = useDocuments;
