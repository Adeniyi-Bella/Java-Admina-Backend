// Shape of a document as displayed in the history list
export type HistoryDocument = {
  docId: string;
  title?: string;
  sender?: string;
  receivedDate?: Date;
  status: "active" | "completed";
  completedTasks: number;
  totalTasks: number;
  progress: number;
};