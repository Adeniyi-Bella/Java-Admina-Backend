export interface GetOrCreateUserResponseDto {
  email: string;
  username: string;
  role: string;
  plan: string;
  documentsUsed: number;
  totalDocuments: number;
  pendingActions: number;
  upcomingDeadlines: number;
  completedTasks: number;
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
  upcomingTasks: ActionPlanTaskDto[];
}

export interface DocumentSummaryDto {
  id: string;
  title: string | null;
  sender: string | null;
  receivedDate: string | null;
  completedTasksCount: number;
  uncompletedTasksCount: number;
}

export interface GetDocumentsPageDto {
  items: DocumentSummaryDto[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
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
  chatMessagesHistory: ChatMessageResponseDto[] | null;
  chatbotCreditRemaining: number;
}

export interface ChatMessageResponseDto {
  id: string;
  role: string;
  content: string;
  createdAt: string;
}

export interface ChatJobResponseDto {
  chatbotPollingId: string;
  docId: string;
  status: string;
}

export interface ChatJobStatusResponseDto {
  chatbotPollingId: string;
  docId: string;
  status: string;
  errorMessage: string | null;
  response: string | null;
  updatedAt: string;
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
