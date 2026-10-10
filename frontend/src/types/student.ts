export interface StudentProfile {
  id: number;
  userId: number;
  email: string;
  fullName: string;
  phone: string;
  branch: string;
  graduationYear: number;
  cgpa: number;
  backlogs: number;
  skills: string[];
  profileComplete: boolean;
  completenessPercent: number;
  hasResume: boolean;
  resumeOriginalName: string | null;
  resumeUploadedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface RowError {
  rowNumber: number;
  email: string;
  message: string;
}

export interface BulkImportResponse {
  totalRows: number;
  successfulCount: number;
  failedCount: number;
  errors: RowError[];
}
