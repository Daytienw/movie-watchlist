"use client";

import Link from "next/link";
import { useEffect, useState, type FormEvent } from "react";

import { api, ApiError, UnauthenticatedError } from "@/lib/api";
import { useRequireAuth } from "@/lib/auth";
import { Poster } from "@/components/Poster";
import type { MovieSearchResult } from "@/lib/types";

type AddState = "idle" | "adding" | "added" | "duplicate";

export default function AddPage() {
  const { user, loading: authLoading } = useRequireAuth();

  const [query, setQuery] = useState("");
  const [activeQuery, setActiveQuery] = useState<string | null>(null);
  // null means no search has run, so the curated suggestions are on screen.
  const [results, setResults] = useState<MovieSearchResult[] | null>(null);
  const [suggestions, setSuggestions] = useState<MovieSearchResult[]>([]);
  const [loadingSuggestions, setLoadingSuggestions] = useState(true);
  const [searching, setSearching] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [addState, setAddState] = useState<Record<string, AddState>>({});
  // Bumping this re-runs the effect for a fresh draw. The backend caches each
  // seed word, so re-rolling costs no OMDb quota.
  const [shuffleKey, setShuffleKey] = useState(0);

  useEffect(() => {
    if (!user) return;
    let cancelled = false;

    async function loadSuggestions() {
      try {
        const suggested = await api.suggestedMovies();
        if (!cancelled) setSuggestions(suggested);
      } catch (err) {
        // A failed suggestion list shouldn't block searching, so stay quiet
        // and let the empty state stand in.
        if (!cancelled && !(err instanceof UnauthenticatedError)) setSuggestions([]);
      } finally {
        if (!cancelled) setLoadingSuggestions(false);
      }
    }

    void loadSuggestions();
    return () => {
      cancelled = true;
    };
  }, [user, shuffleKey]);

  async function handleSearch(event: FormEvent) {
    event.preventDefault();
    const trimmed = query.trim();
    if (!trimmed) return;

    setSearching(true);
    setError(null);
    try {
      const found = await api.searchMovies(trimmed);
      setResults(found);
      setActiveQuery(trimmed);
    } catch (err) {
      if (err instanceof UnauthenticatedError) return;
      setError(err instanceof ApiError ? err.message : "Search failed");
      setResults(null);
    } finally {
      setSearching(false);
    }
  }

  function clearSearch() {
    setQuery("");
    setResults(null);
    setActiveQuery(null);
    setError(null);
  }

  async function add(movie: MovieSearchResult) {
    setAddState((state) => ({ ...state, [movie.imdbID]: "adding" }));
    setError(null);
    try {
      await api.addToWatchlist({
        imdbId: movie.imdbID,
        title: movie.Title,
        year: movie.Year,
        poster: movie.Poster,
        status: "TO_WATCH",
      });
      setAddState((state) => ({ ...state, [movie.imdbID]: "added" }));
    } catch (err) {
      if (err instanceof UnauthenticatedError) return;
      // 409 means it's already on this user's list, which isn't really a failure.
      if (err instanceof ApiError && err.status === 409) {
        setAddState((state) => ({ ...state, [movie.imdbID]: "duplicate" }));
        return;
      }
      setAddState((state) => ({ ...state, [movie.imdbID]: "idle" }));
      setError(err instanceof ApiError ? err.message : "Could not add that film");
    }
  }

  if (authLoading || !user) {
    return <p className="text-white/40">Loading…</p>;
  }

  const showingSearch = results !== null;
  const visible = showingSearch ? results : suggestions;

  return (
    <div>
      <div className="flex items-center justify-between gap-4 mb-6">
        <h1 className="text-2xl font-semibold">Add movies</h1>
        <Link href="/watchlist" className="text-sm text-white/60 hover:text-white transition">
          Back to my list
        </Link>
      </div>

      <form onSubmit={handleSearch} className="flex gap-2 mb-8">
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search by title, e.g. inception"
          aria-label="Search movies"
          className="flex-1 rounded-md bg-white/5 border border-white/10 px-3 py-2 outline-none focus:border-white/30 transition"
        />
        <button
          type="submit"
          disabled={searching || !query.trim()}
          className="rounded-md bg-white text-black text-sm font-medium px-5 hover:bg-white/90 disabled:opacity-40 transition"
        >
          {searching ? "Searching…" : "Search"}
        </button>
      </form>

      {error && (
        <p role="alert" className="text-sm text-red-400 bg-red-400/10 rounded-md px-3 py-2 mb-4">
          {error}
        </p>
      )}

      <div className="flex items-baseline justify-between gap-4 mb-4">
        <h2 className="text-sm font-medium text-white/70">
          {showingSearch ? `Results for “${activeQuery}”` : "Discover"}
        </h2>
        {showingSearch ? (
          <button
            onClick={clearSearch}
            className="text-sm text-white/50 hover:text-white transition"
          >
            Clear search
          </button>
        ) : (
          <button
            onClick={() => setShuffleKey((key) => key + 1)}
            disabled={loadingSuggestions}
            className="text-sm text-white/50 hover:text-white disabled:opacity-40 transition"
          >
            ↻ Shuffle
          </button>
        )}
      </div>

      {!showingSearch && loadingSuggestions && <p className="text-white/40">Loading…</p>}

      {showingSearch && visible.length === 0 && (
        <p className="text-white/50">No films matched “{activeQuery}”.</p>
      )}

      {!showingSearch && !loadingSuggestions && visible.length === 0 && (
        <p className="text-white/50">
          Suggestions are unavailable right now — search for a title instead.
        </p>
      )}

      {visible.length > 0 && (
        <ul className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-5">
          {visible.map((movie) => {
            const state = addState[movie.imdbID] ?? "idle";
            return (
              <li key={movie.imdbID} className="flex flex-col gap-2">
                <Poster src={movie.Poster} alt={movie.Title} />

                <div className="min-w-0">
                  <p className="text-sm font-medium truncate" title={movie.Title}>
                    {movie.Title}
                  </p>
                  <p className="text-xs text-white/40">{movie.Year}</p>
                </div>

                <button
                  onClick={() => add(movie)}
                  disabled={state !== "idle"}
                  className={`mt-1 text-xs rounded-md px-2 py-1.5 border transition ${
                    state === "added"
                      ? "border-emerald-400/30 text-emerald-300 bg-emerald-400/10"
                      : state === "duplicate"
                        ? "border-white/10 text-white/40"
                        : "border-white/15 hover:bg-white/5 disabled:opacity-40"
                  }`}
                >
                  {state === "adding" && "Adding…"}
                  {state === "added" && "✓ Added"}
                  {state === "duplicate" && "Already on your list"}
                  {state === "idle" && "Add to watchlist"}
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
