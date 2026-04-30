export interface CreateDocumentRequestDto {
  docLanguage: string;
  targetLanguage: string;
}

export interface AddTaskToDocumentRequestDto {
  title: string;
  dueDate?: string | null;
  location?: string | null;
}

export interface UpdateExistingTaskRequestDto {
  title?: string | null;
  dueDate?: string | null;
  completed?: boolean | null;
  location?: string | null;
}

export interface ChatbotPromptRequestDto {
  prompt: string;
}
