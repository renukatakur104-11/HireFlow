export type Role = 'STUDENT' | 'ADMIN';

export interface UserSummary {
  id: number;
  email: string;
  role: Role;
  fullName: string;
  branch: string | null;
  cgpa: number | null;
  profileComplete: boolean;
  hasResume: boolean;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInMs: number;
  user: UserSummary;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
  phone: string;
  branch: string;
  graduationYear: number;
  cgpa: number;
  skills: string[];
}
