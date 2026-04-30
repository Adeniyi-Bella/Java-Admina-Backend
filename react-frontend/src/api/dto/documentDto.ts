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
