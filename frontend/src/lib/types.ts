export type WatchlistStatus = "TO_WATCH" | "WATCHED";

export interface User {
  id: number;
  fullName: string;
  email: string;
  createdAt: string;
  updatedAt: string;
}

export interface LoginResponse {
  token: string;
  expiresIn: number;
}

export interface WatchlistItem {
  id: number;
  imdbId: string;
  title: string;
  year: string;
  poster: string;
  status: WatchlistStatus;
  addedDate: string;
}

/** OMDb keeps its original capitalisation through the search endpoint. */
export interface MovieSearchResult {
  Title: string;
  Year: string;
  imdbID: string;
  Poster: string;
  Type: string;
}

/** Spring Data's Page envelope, as returned by GET /api/watchlist/. */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

/** RFC 7807 ProblemDetail, plus the `errors` map the API adds on validation failures. */
export interface ProblemDetail {
  status: number;
  title?: string;
  detail?: string;
  description?: string;
  errors?: Record<string, string>;
}
