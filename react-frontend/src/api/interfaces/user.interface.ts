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
