export type ApplicationStatus =
  | 'REGISTERED'
  | 'ROUND_1'
  | 'ROUND_2'
  | 'TECHNICAL_INTERVIEW'
  | 'HR_INTERVIEW'
  | 'SELECTED'
  | 'REJECTED'
  | 'WITHDRAWN';

export interface Application {
  id: number;
  driveId: number;
  companyName: string;
  roleTitle: string;
  location: string;
  packageLpa: number;
  studentProfileId: number;
  studentName: string;
  studentEmail: string;
  branch: string;
  cgpa: number;
  currentStatus: ApplicationStatus;
  appliedAt: string;
  updatedAt: string;
  driveDate: string;
  applicationDeadline: string;
}

export interface ApplicationStatusHistory {
  id: number;
  fromStatus: ApplicationStatus | null;
  toStatus: ApplicationStatus;
  changedByEmail: string;
  remarks: string | null;
  override: boolean;
  createdAt: string;
}
