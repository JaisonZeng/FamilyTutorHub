import { request } from '@umijs/max';

export interface LoginParams {
  username: string;
  password: string;
}

export interface LoginResult {
  token: string;
  currentUser: API.CurrentUser;
}

export async function login(params: LoginParams) {
  return request<LoginResult>('/api/login', {
    method: 'POST',
    data: params,
  });
}

export async function logout() {
  return request('/api/logout', {
    method: 'POST',
  });
}

export async function getCurrentUser() {
  return request<{ data: API.CurrentUser }>('/api/currentUser');
}

export interface ChangePasswordParams {
  old_password: string;
  new_password: string;
}

export async function changePassword(params: ChangePasswordParams) {
  return request('/api/password', {
    method: 'PUT',
    data: params,
  });
}
