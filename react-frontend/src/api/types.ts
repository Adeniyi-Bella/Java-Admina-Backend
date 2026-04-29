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

export interface IValues {
  max: number;
  min: number;
  current: number;
}

export interface ILimits {
  products?: IValues;
  orders?: IValues;
  apiCalls?: IValues;
}

export interface UpdateUserPlanRequest {
  plan: SubscriptionPlan;
}

export interface UserDTO {
  userId: string;
  email: string;
  username: string;
  plan: SubscriptionPlan;
  limits: ILimits;
  createdAt?: string;
  updatedAt?: string;
}
export interface UserPlanResponse {
  planName: "free" | "standard" | "premium" | string;
  email: string;
  documentLimits: IValues;
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
  documentLimits: IValues
}

export interface UseDeleteUserOptions {
  onError: (error: AppError) => void;
  onSuccess?: () => void;
}
