import {
  type AccountInfo,
  type IPublicClientApplication,
} from "@azure/msal-browser";
import { acquireTokenSilently } from "./acquireTokenSilently";

type HeaderMap = Record<string, string>;

function normalizeHeaders(headers?: HeadersInit): HeaderMap {
  if (!headers) {
    return {};
  }

  if (headers instanceof Headers) {
    return Object.fromEntries(headers.entries());
  }

  if (Array.isArray(headers)) {
    return Object.fromEntries(headers);
  }

  return { ...headers };
}

export async function buildBearerAuthHeader(
  instance: IPublicClientApplication,
  account: AccountInfo,
): Promise<string> {
  const { idToken, accessToken } = await acquireTokenSilently(
    instance,
    account,
  );
  return `Bearer ${idToken}auth${accessToken}`;
}

export async function buildBearerHeaders(
  instance: IPublicClientApplication,
  account: AccountInfo,
  headers?: HeadersInit,
): Promise<HeaderMap> {
  const authHeader = await buildBearerAuthHeader(instance, account);

  return {
    ...normalizeHeaders(headers),
    Authorization: authHeader,
  };
}
