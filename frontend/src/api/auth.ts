import { apiClient } from './client';
import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
  VerifyOtpRequest,
  VerifyOtpResponse,
} from '@/types';

export const authApi = {
  login: async (payload: LoginRequest): Promise<LoginResponse> => {
    return apiClient<LoginResponse>('/auth/login', {
      method: 'POST',
      body: payload,
    });
  },

  register: async (payload: RegisterRequest): Promise<RegisterResponse> => {
    return apiClient<RegisterResponse>('/auth/register', {
      method: 'POST',
      body: payload,
    });
  },

  verifyOtp: async (payload: VerifyOtpRequest): Promise<VerifyOtpResponse> => {
    return apiClient<VerifyOtpResponse>('/auth/verify-otp', {
      method: 'POST',
      body: payload,
    });
  },
};
