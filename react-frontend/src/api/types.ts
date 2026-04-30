import type { AppError } from "./error/customeError";
import type {
  CustomApiErrorResponse,
  CustomApiResponse,
} from "./interfaces/customApiResponse.interface";

export type ApiSuccessResponse<T> = CustomApiResponse<T>;
export type ApiErrorResponse = CustomApiErrorResponse;

export interface PaginationParams {
  limit?: number;
  offset?: number;
}

export interface PaginatedResponse<T> {
  data: T[];
  pagination: {
    total: number;
    limit: number;
    offset: number;
    hasMore: boolean;
  };
}

export enum SubscriptionPlan {
  FREE = "free",
  STANDARD = "standard",
  PREMIUM = "premium",
}

export interface UpdateUserPlanRequest {
  plan: SubscriptionPlan;
}

export interface UserDTO {
  userId: string;
  email: string;
  username: string;
  plan: SubscriptionPlan;
  createdAt?: string;
  updatedAt?: string;
}

export interface IActionPlan {
  id: string;
  title?: string;
  dueDate?: Date;
  completed: boolean;
  location?: string;
}

export interface IDocumentPreview {
  docId: string;
  title?: string;
  sender?: string;
  receivedDate?: Date;
  actionPlans?: IActionPlan[];
}

export interface IGetAllDocumentsResponse {
  limit?: number;
  offset?: number;
  total?: number;
  documents: IDocumentPreview[];
  userPlan: string;
  email: string;
}

export interface UseDeleteUserOptions {
  onError: (error: AppError) => void;
  onSuccess?: () => void;
}
