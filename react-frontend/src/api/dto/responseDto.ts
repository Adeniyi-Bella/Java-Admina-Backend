export interface GetOrCreateUserResponseDto {
  email: string;
  username: string;
  role: string;
  plan: string;
  documentsUsed: number;
}

export interface ActionPlanTaskDto {
  id: string;
  title: string;
  dueDate: string | null;
  completed: boolean;
  location: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface UserDocumentDto {
  id: string;
  title: string;
  sender: string;
  receivedDate: string | null;
  actionPlanTasks: ActionPlanTaskDto[];
  createdAt: string;
  updatedAt: string;
}

export interface UserWithDocumentsResponseDto {
  user: GetOrCreateUserResponseDto;
  documents: UserDocumentDto[];
}

export type DocumentProcessStatus =
  | "PENDING"
  | "QUEUE"
  | "TRANSLATE"
  | "SUMMARIZE"
  | "SAVING"
  | "COMPLETED"
  | "ERROR"
  | "CANCELLED";

export interface DocumentJobResponseDto {
  docId: string;
  status: DocumentProcessStatus;
}

export interface DocumentStatusResponseDto {
  status: DocumentProcessStatus;
  errorMessage: string | null;
  updatedAt: string;
}

export interface ActionPlanItemDto {
  title: string;
  reason: string;
}

export interface ActionPlanTaskDto {
  id: string;
  title: string;
  dueDate: string | null;
  completed: boolean;
  location: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface GetDocumentResponseDto {
  id: string;
  targetLanguage: string;
  title: string | null;
  sender: string | null;
  receivedDate: string | null;
  summary: string | null;
  structuredTranslatedText: Record<string, string> | null;
  actionPlan: ActionPlanItemDto[] | null;
  actionPlanTasks: ActionPlanTaskDto[] | null;
  chatMessagesHistory: Array<Record<string, unknown>> | null;
}

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

