import apiClient from './client';
import { AuthResponse, ForgotPasswordResponse, ResetPasswordRequest, ChangePasswordRequest } from '@/types';

export const authApi = {
  async register(username: string, email: string, password: string): Promise<AuthResponse> {
    const response = await apiClient.post<AuthResponse>('/auth/register', {
      username,
      email,
      password,
    });
    return response.data;
  },

  async login(username: string, password: string): Promise<AuthResponse> {
    const response = await apiClient.post<AuthResponse>('/auth/login', {
      username,
      password,
    });
    return response.data;
  },

  async forgotPassword(identifier: string): Promise<ForgotPasswordResponse> {
    const response = await apiClient.post<ForgotPasswordResponse>('/auth/forgot-password', {
      identifier,
    });
    return response.data;
  },

  async resetPassword(data: ResetPasswordRequest): Promise<{ message: string }> {
    const response = await apiClient.post<{ message: string }>('/auth/reset-password', data);
    return response.data;
  },

  async changePassword(data: ChangePasswordRequest): Promise<{ message: string }> {
    const response = await apiClient.put<{ message: string }>('/auth/change-password', data);
    return response.data;
  },
};
