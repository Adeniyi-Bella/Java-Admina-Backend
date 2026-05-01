import type { DocumentProcessStatus } from "@/api/dto/responseDto";
import { config } from "@/lib/config/config";

export const API_BASE_URL = config.VITE_API_URL;
export const API_TIMEOUT_MS = 15000;
export const DOCUMENT_POLL_INTERVAL_MS = 5000;
export const DOCUMENT_POLL_TIMEOUT_MS = 2 * 60 * 1000;
export const AUTH_ERROR_EVENT = "admina:auth-error";

export const queryKey = {
  authenticateUser: (accountId?: string) =>
    ["authenticateUser", accountId] as const,
  getDocuments: (accountId?: string) => ["getDocuments", accountId] as const,
  getDocument: (docId: string) => ["getDocument", docId] as const,
  getDocumentChatbotCredit: (docId: string) =>
    ["getDocumentChatbotCredit", docId] as const,
  getChatbotStatus: (docId: string, chatbotPollingId?: string) =>
    ["getChatbotStatus", docId, chatbotPollingId] as const,
  getDocumentStatus: (docId: string) => ["getDocumentStatus", docId] as const,
};

export const languages = [
  { code: "de", name: "German" },
  { code: "en", name: "English" },
  { code: "es", name: "Spanish" },
  { code: "fr", name: "French" },
  { code: "it", name: "Italian" },
  { code: "pt", name: "Portuguese" },
  { code: "ru", name: "Russian" },
  { code: "ar", name: "Arabic" },
  { code: "tr", name: "Turkish" },
  { code: "pl", name: "Polish" },
  { code: "zh", name: "Chinese" },
  { code: "nl", name: "Dutch" },
  { code: "sv", name: "Swedish" },
  { code: "da", name: "Danish" },
  { code: "no", name: "Norwegian" },
];

export const documentStatusMessages: Record<DocumentProcessStatus, string> = {
  PENDING: "Uploading document...",
  QUEUE: "Queued for processing...",
  TRANSLATE: "Translating document...",
  SUMMARIZE: "Summarizing document...",
  SAVING: "Saving document...",
  COMPLETED: "Document processed successfully",
  ERROR: "Document processing failed",
  CANCELLED: "Document processing cancelled",
};
