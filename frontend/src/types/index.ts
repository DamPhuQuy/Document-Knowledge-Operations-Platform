/**
 * Central Domain Types & API Contracts for Document Knowledge Operations Platform
 */

// Common Pagination & Error
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface ErrorResponse {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  path?: string;
  errors?: Record<string, string>;
}

// Authentication & Identity
export interface UserProfileDto {
  id: string;
  email: string;
  fullName: string;
  departmentId: string | null;
  isInternal: boolean;
  roles: string[];
  permissions: string[];
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserProfileDto;
}

export interface RegisterRequest {
  email: string;
  password: string;
  confirmPassword: string;
  firstName: string;
  lastName?: string;
}

export interface RegisterResponse {
  id: string;
  email: string;
  fullName: string;
  roles: string[];
}

export interface VerifyOtpRequest {
  email: string;
  otp: string;
}

export interface VerifyOtpResponse {
  message: string;
  activated: boolean;
}

// Document Management
export type AccessLevel = "CONFIDENTIAL" | "INTERNAL" | "PUBLIC" | "RESTRICTED";
export type PermissionLevel = "VIEW" | "EDIT" | "ADMIN";
export type DocumentStatus = "ACTIVE" | "ARCHIVED" | "DELETED";

export interface UserGrantDto {
  userId: string;
  permissionLevel: PermissionLevel;
}

export interface DepartmentGrantDto {
  departmentId: string;
  permissionLevel: PermissionLevel;
}

export interface RoleGrantDto {
  roleId: string;
  permissionLevel: PermissionLevel;
}

export interface DocumentResponseDto {
  id: string;
  title: string | null;
  originalFileName: string;
  contentType: string;
  fileSizeBytes: number;
  checksumSha256: string;
  storageKey: string;
  status: DocumentStatus;
  accessLevel: AccessLevel;
  departmentId: string | null;
  uploadedByUserId: string;
  createdAt: string;
  updatedAt: string;
}

export interface DocumentVersionResponseDto {
  id: string;
  documentId: string;
  versionNumber: number;
  storageKey: string;
  fileSizeBytes: number;
  checksumSha256: string;
  changeSummary: string | null;
  uploadedByUserId: string;
  createdAt: string;
}

export interface DocumentPermissionsResponseDto {
  documentId: string;
  accessLevel: AccessLevel;
  userGrants: UserGrantDto[];
  departmentGrants: DepartmentGrantDto[];
  roleGrants: RoleGrantDto[];
  updatedAt: string;
}

export interface UpdateDocumentPermissionsRequest {
  accessLevel: AccessLevel;
  userGrants?: UserGrantDto[];
  departmentGrants?: DepartmentGrantDto[];
  roleGrants?: RoleGrantDto[];
}

export interface UploadDocumentParams {
  file: File;
  title?: string;
  description?: string;
  accessLevel?: AccessLevel;
  departmentId?: string;
}

export interface UploadVersionParams {
  file: File;
  changeSummary?: string;
}

// Department & Organization
export interface DepartmentResponseDto {
  id: string;
  code: string;
  name: string;
  description: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateDepartmentRequest {
  code: string;
  name: string;
  description?: string;
}

export interface UpdateDepartmentRequest {
  code: string;
  name: string;
  description?: string;
}

export interface RoleDto {
  id: string;
  code: string;
  name: string;
  description: string | null;
}

export interface UserDepartmentResponseDto {
  userId: string;
  email: string;
  fullName: string;
  departmentId: string | null;
  departmentCode: string | null;
  departmentName: string | null;
  internal: boolean;
  updatedAt: string;
}

export interface AssignUserDepartmentRequest {
  departmentId: string | null;
  isInternal: boolean;
}

export interface UserRolesResponseDto {
  userId: string;
  email: string;
  fullName: string;
  departmentId: string | null;
  roles: RoleDto[];
  permissions: string[];
}

export interface AssignRolesRequest {
  roleIds: string[];
}

// Audit Logs
export type AuditStatus = "SUCCESS" | "FAILED";

export interface AuditLogResponseDto {
  id: string;
  userId: string | null;
  action: string;
  resourceType: string;
  resourceId: string | null;
  ipAddress: string | null;
  userAgent: string | null;
  status: AuditStatus;
  details: Record<string, unknown> | null;
  createdAt: string;
}

export interface AuditLogQueryFilter {
  userId?: string;
  action?: string;
  resourceType?: string;
  resourceId?: string;
  status?: AuditStatus;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
}
