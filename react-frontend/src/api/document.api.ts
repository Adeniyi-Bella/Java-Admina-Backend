import { type AccountInfo, type IPublicClientApplication } from "@azure/msal-browser";
import { buildBearerHeaders } from "./auth/authHeaders";
import { apiClient } from "./clients/axiosClient";
import { requireApiData } from "./response";
import type {
  AddTaskToDocumentRequestDto,
  CreateDocumentRequestDto,
  UpdateExistingTaskRequestDto,
} from "./dto/requestDto";
import type {
  ActionPlanTaskDto,
  ChatJobResponseDto,
  ChatJobStatusResponseDto,
  CustomApiResponse,
  DocumentJobResponseDto,
  DocumentStatusResponseDto,
  GetDocumentResponseDto,
} from "./dto/responseDto";
import type { ChatbotPromptRequestDto } from "./dto/requestDto";

export class DocumentApi {
  static async createDocument(
    instance: IPublicClientApplication,
    account: AccountInfo,
    file: File,
    request: CreateDocumentRequestDto,
  ): Promise<DocumentJobResponseDto> {
    const headers = await buildBearerHeaders(instance, account);
    const formData = new FormData();

    formData.append("file", file);
    formData.append("docLanguage", request.docLanguage);
    formData.append("targetLanguage", request.targetLanguage);

    const response = await apiClient.post<CustomApiResponse<DocumentJobResponseDto>>(
      "/documents",
      formData,
      {
        headers,
      },
    );

    return requireApiData(response.data, {
      message: "Create document response missing data",
      code: "EMPTY_CREATE_DOCUMENT_RESPONSE",
      statusCode: response.status,
    });
  }

  static async getDocumentStatus(
    instance: IPublicClientApplication,
    account: AccountInfo,
    docId: string,
  ): Promise<DocumentStatusResponseDto> {
    const headers = await buildBearerHeaders(instance, account);

    const response = await apiClient.get<
      CustomApiResponse<DocumentStatusResponseDto>
    >(`/documents/status/${docId}`, {
      headers,
    });

    return requireApiData(response.data, {
      message: "Document status response missing data",
      code: "EMPTY_DOCUMENT_STATUS_RESPONSE",
      statusCode: response.status,
    });
  }

  static async getDocument(
    instance: IPublicClientApplication,
    account: AccountInfo,
    docId: string,
  ): Promise<GetDocumentResponseDto> {
    const headers = await buildBearerHeaders(instance, account);

    const response = await apiClient.get<
      CustomApiResponse<GetDocumentResponseDto>
    >(`/documents/${docId}`, {
      headers,
    });

    return requireApiData(response.data, {
      message: "Get document response missing data",
      code: "EMPTY_GET_DOCUMENT_RESPONSE",
      statusCode: response.status,
    });
  }

  static async sendChatMessage(
    instance: IPublicClientApplication,
    account: AccountInfo,
    docId: string,
    request: ChatbotPromptRequestDto,
  ): Promise<ChatJobResponseDto> {
    const headers = await buildBearerHeaders(instance, account);

    const response = await apiClient.post<CustomApiResponse<ChatJobResponseDto>>(
      `/documents/${docId}/chat`,
      request,
      {
        headers,
      },
    );

    return requireApiData(response.data, {
      message: "Chat response missing data",
      code: "EMPTY_CHAT_RESPONSE",
      statusCode: response.status,
    });
  }

  static async getChatJobStatus(
    instance: IPublicClientApplication,
    account: AccountInfo,
    docId: string,
    chatbotPollingId: string,
  ): Promise<ChatJobStatusResponseDto> {
    const headers = await buildBearerHeaders(instance, account);

    const response = await apiClient.get<
      CustomApiResponse<ChatJobStatusResponseDto>
    >(`/documents/${docId}/chat/status/${chatbotPollingId}`, {
      headers,
    });

    return requireApiData(response.data, {
      message: "Chat job status response missing data",
      code: "EMPTY_CHAT_STATUS_RESPONSE",
      statusCode: response.status,
    });
  }

  static async addTask(
    instance: IPublicClientApplication,
    account: AccountInfo,
    docId: string,
    request: AddTaskToDocumentRequestDto,
  ): Promise<ActionPlanTaskDto> {
    const headers = await buildBearerHeaders(instance, account);

    const response = await apiClient.post<
      CustomApiResponse<ActionPlanTaskDto>
    >(`/documents/${docId}/tasks`, request, {
      headers,
    });

    return requireApiData(response.data, {
      message: "Add task response missing data",
      code: "EMPTY_ADD_TASK_RESPONSE",
      statusCode: response.status,
    });
  }

  static async updateTask(
    instance: IPublicClientApplication,
    account: AccountInfo,
    docId: string,
    taskId: string,
    request: UpdateExistingTaskRequestDto,
  ): Promise<ActionPlanTaskDto> {
    const headers = await buildBearerHeaders(instance, account);

    const response = await apiClient.patch<
      CustomApiResponse<ActionPlanTaskDto>
    >(`/documents/${docId}/tasks/${taskId}`, request, {
      headers,
    });

    return requireApiData(response.data, {
      message: "Update task response missing data",
      code: "EMPTY_UPDATE_TASK_RESPONSE",
      statusCode: response.status,
    });
  }

  static async deleteTask(
    instance: IPublicClientApplication,
    account: AccountInfo,
    docId: string,
    taskId: string,
  ): Promise<void> {
    const headers = await buildBearerHeaders(instance, account);

    await apiClient.delete(`/documents/${docId}/tasks/${taskId}`, {
      headers,
    });
  }
}
