export type DriveStatus = 'DRAFT' | 'OPEN' | 'CLOSED' | 'COMPLETED';
export type RoundStatusCode = 'ROUND_1' | 'ROUND_2' | 'TECHNICAL_INTERVIEW' | 'HR_INTERVIEW';

export interface DriveRound {
  id: number;
  roundOrder: number;
  roundName: string;
  statusCode: RoundStatusCode;
  description: string | null;
}

export interface Drive {
  id: number;
  companyName: string;
  roleTitle: string;
  description: string;
  packageLpa: number;
  packageDetails: string | null;
  location: string;
  minCgpa: number;
  maxBacklogs: number;
  driveDate: string;
  applicationDeadline: string;
  status: DriveStatus;
  allowedBranches: string[];
  requiredSkills: string[];
  rounds: DriveRound[];
  totalApplications: number;
  createdAt: string;
  updatedAt: string;
  // Student-specific
  isEligible?: boolean;
  hasApplied?: boolean;
  eligibilityReasons?: string[];
}
