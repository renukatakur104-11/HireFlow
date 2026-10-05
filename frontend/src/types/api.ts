export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface PagedResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface ErrorResponse {
  success: false;
  status: number;
  error: string;
  message: string;
  path: string;
  validationErrors?: Record<string, string>;
  timestamp: string;
}
