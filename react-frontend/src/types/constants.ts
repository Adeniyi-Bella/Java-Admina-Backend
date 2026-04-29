import { config } from "@/lib/config/config";

export const API_BASE_URL = config.VITE_API_URL;
export const API_TIMEOUT_MS = 15000;
export const DOCUMENT_POLL_INTERVAL_MS = 5000;
export const DOCUMENT_POLL_TIMEOUT_MS = 2 * 60 * 1000;

export const queryKey = {
  authenticateUser: (accountId?: string) =>
    ['authenticateUser', accountId] as const,
  getAllDocuments: ['getAllDocuments'] as const,
  getDocument: (docId: string) => ['getDocument', docId] as const,
  getChatbotLimit: (docId: string) => ['getChatbotLimit', docId] as const,
};
