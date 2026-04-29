import axios, { type AxiosInstance, AxiosError } from "axios";
import { API_BASE_URL, API_TIMEOUT_MS } from "@/types/constants";
import {
  ApiError,
  NetworkError,
  ServerDownError,
  TimeoutError,
  UnauthorizedError,
} from "../error/customeError";
import type { CustomApiResponse } from "../interfaces/customApiResponse.interface";

export class AxiosApiClient {
  private static instance: AxiosInstance;

  static getInstance(): AxiosInstance {
    if (!this.instance) {
      this.instance = this.createInstance();
      this.setupInterceptors();
    }
    return this.instance;
  }

  private static createInstance(): AxiosInstance {
    return axios.create({
      baseURL: API_BASE_URL,
      timeout: API_TIMEOUT_MS,
      headers: {
        // "Content-Type": "application/json",
        "ngrok-skip-browser-warning": "true",
      },
    });
  }

  private static setupInterceptors(): void {
    this.instance.interceptors.response.use(
      (response) => response,
      (error: AxiosError<CustomApiResponse<unknown>>) => {
        if (error.response) {
          const { status, data } = error.response;
          const message =
            data?.error?.message ||
            "An unexpected error occurred";
          const code = `HTTP_${data?.error?.status ?? status}`;

          if (status === 401)
            return Promise.reject(new UnauthorizedError(message, code, status));

          return Promise.reject(new ApiError(message, code, status));
        }

        // 2. No Response (Internet is out or Server is totally crashed/timed out)
        if (error.code === "ECONNABORTED")
          return Promise.reject(new TimeoutError());
        if (error.code === "ERR_NETWORK")
          return Promise.reject(new NetworkError());

        return Promise.reject(new ServerDownError());
      },
    );
  }
}

export const apiClient = AxiosApiClient.getInstance();
