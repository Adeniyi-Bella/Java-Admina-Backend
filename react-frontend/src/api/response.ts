import { ApiError } from "./error/customeError";
import type { CustomApiResponse } from "./interfaces/customApiResponse.interface";

type RequireApiDataOptions = {
  message: string;
  code: string;
  statusCode?: number;
};

export function requireApiData<T>(
  response: CustomApiResponse<T>,
  options: RequireApiDataOptions,
): NonNullable<T> {
  if (response.data === undefined || response.data === null) {
    throw new ApiError(
      options.message,
      options.code,
      options.statusCode ?? 502,
    );
  }

  return response.data as NonNullable<T>;
}

export function buildAuthenticateUserMessage(
  username: string | undefined,
  created: boolean,
): string {
  const safeUsername = username?.trim();

  if (created) {
    return safeUsername
      ? `Welcome, ${safeUsername}. Your account has been created successfully.`
      : "Your account has been created successfully.";
  }

  return safeUsername
    ? `Welcome back, ${safeUsername}. You have successfully authenticated.`
    : "You have successfully authenticated.";
}
