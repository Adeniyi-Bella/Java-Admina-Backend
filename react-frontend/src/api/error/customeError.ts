/**
 * @copyright 2025 Adeniyi Bella
 * @license Apache-2.0
 */

export abstract class AppError extends Error {
  public readonly statusCode?: number;
  public readonly code: string;
  public readonly userMessage: string;
  public readonly retryable: boolean;

  constructor(message: string, userMessage: string, code: string, statusCode?: number, retryable = false) {
    super(message);
    this.name = this.constructor.name;
    this.code = code;
    this.userMessage = userMessage;
    this.statusCode = statusCode;
    this.retryable = retryable;
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

// 1. Pre-Server Errors (Axios level)
export class NetworkError extends AppError {
  constructor() {
    super("Network connection failed", "Please check your internet connection or try again later as our server may be temporarily down.", "NETWORK_ERROR", undefined, true);
  }
}

export class TimeoutError extends AppError {
  constructor() {
    super("Request timed out", "The server is taking too long to respond.", "TIMEOUT_ERROR", 408, true);
  }
}

export class ServerDownError extends AppError {
  constructor() {
    super("Server unreachable", "The service is temporarily down.", "SERVER_DOWN", 503, true);
  }
}

// 2. Auth Errors (MSAL level)
export class MsalNoAccountError extends AppError {
  constructor() {
    super("No active account", "Please log in again.", "AUTH_NO_ACCOUNT", 401, false);
  }
}

// 3. The "Generic" Server Error (Used for ALL status codes returned by your API)
export class ApiError extends AppError {
  constructor(message: string, code: string, statusCode: number) {
    // Trust the server's message and status code completely
    super(message, message, code, statusCode, statusCode >= 500);
  }
}

/**
 * 4. Empty classes for your instanceof checks
 * We only keep these so your handleQueryError logic (instanceof UnauthorizedError) 
 * still works without a massive switch case.
 */
export class UnauthorizedError extends ApiError {}