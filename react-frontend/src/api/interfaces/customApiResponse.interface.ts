export interface CustomApiErrorResponse {
  status: number;
  message: string;
}

export interface CustomApiResponse<T> {
  success: boolean;
  data: T | null;
  error: CustomApiErrorResponse | null;
  timestamp: string;
}
