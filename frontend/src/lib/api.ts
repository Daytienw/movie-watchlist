import type {
  LoginResponse,
  MovieSearchResult,
  Page,
  ProblemDetail,
  User,
  WatchlistItem,
  WatchlistStatus,
} from "./types";

const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

const TOKEN_KEY = "movie-watchlist-token";

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string) {
  window.localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken() {
  window.localStorage.removeItem(TOKEN_KEY);
}

/**
 * An error carrying the API's ProblemDetail. `fieldErrors` is populated from
 * the `errors` map the backend returns on a 400 so forms can highlight the
 * offending inputs rather than showing one generic message.
 */
export class ApiError extends Error {
  status: number;
  fieldErrors: Record<string, string>;

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

/** Thrown when the token is missing, expired or rejected. */
export class UnauthenticatedError extends ApiError {
  constructor() {
    super(403, "Your session has expired. Please log in again.");
    this.name = "UnauthenticatedError";
  }
}

interface RequestOptions {
  method?: string;
  body?: unknown;
  auth?: boolean;
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = "GET", body, auth = true } = options;

  const headers: Record<string, string> = {};
  if (body !== undefined) headers["Content-Type"] = "application/json";

  if (auth) {
    const token = getToken();
    if (!token) throw new UnauthenticatedError();
    headers["Authorization"] = `Bearer ${token}`;
  }

  let response: Response;
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(0, "Could not reach the API. Is the backend running on port 8080?");
  }

  // The API returns 403 for a missing, expired or tampered token. Treat it as
  // a session problem so callers can bounce the user back to the login screen.
  if (response.status === 403 && auth) {
    clearToken();
    throw new UnauthenticatedError();
  }

  if (response.status === 204) return undefined as T;

  const text = await response.text();
  const payload = text ? (JSON.parse(text) as unknown) : null;

  if (!response.ok) {
    const problem = (payload ?? {}) as ProblemDetail;
    throw new ApiError(
      response.status,
      problem.detail ?? problem.title ?? `Request failed (${response.status})`,
      problem.errors ?? {},
    );
  }

  return payload as T;
}

export const api = {
  signup: (fullName: string, email: string, password: string) =>
    request<User>("/api/auth/signup", {
      method: "POST",
      body: { fullName, email, password },
      auth: false,
    }),

  login: (email: string, password: string) =>
    request<LoginResponse>("/api/auth/login", {
      method: "POST",
      body: { email, password },
      auth: false,
    }),

  me: () => request<User>("/api/users/me"),

  searchMovies: (query: string) =>
    request<MovieSearchResult[]>(`/api/movies/search?q=${encodeURIComponent(query)}`),

  suggestedMovies: () => request<MovieSearchResult[]>("/api/movies/suggestions"),

  getWatchlist: (page: number, status: WatchlistStatus | null, size = 12) => {
    const params = new URLSearchParams({
      page: String(page),
      size: String(size),
      sort: "addedDate,desc",
    });
    if (status) params.set("status", status);
    return request<Page<WatchlistItem>>(`/api/watchlist/?${params}`);
  },

  addToWatchlist: (item: {
    imdbId: string;
    title: string;
    year: string;
    poster: string;
    status: WatchlistStatus;
  }) => request<WatchlistItem>("/api/watchlist/", { method: "POST", body: item }),

  // PUT is full-replacement on this API, so every field has to be sent.
  updateWatchlistItem: (id: number, item: Omit<WatchlistItem, "id" | "addedDate">) =>
    request<WatchlistItem>(`/api/watchlist/${id}`, { method: "PUT", body: item }),

  deleteWatchlistItem: (id: number) =>
    request<void>(`/api/watchlist/${id}`, { method: "DELETE" }),
};
