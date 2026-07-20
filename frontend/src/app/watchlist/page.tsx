"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";

import { api, ApiError, UnauthenticatedError } from "@/lib/api";
import { useRequireAuth } from "@/lib/auth";
import { Poster } from "@/components/Poster";
import type { Page, WatchlistItem, WatchlistStatus } from "@/lib/types";

const FILTERS: { label: string; value: WatchlistStatus | null }[] = [
  { label: "All", value: null },
  { label: "To watch", value: "TO_WATCH" },
  { label: "Watched", value: "WATCHED" },
];

export default function WatchlistPage() {
  const { user, loading: authLoading } = useRequireAuth();

  const [page, setPage] = useState<Page<WatchlistItem> | null>(null);
  const [pageNumber, setPageNumber] = useState(0);
  const [filter, setFilter] = useState<WatchlistStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);

  // Bumping this re-runs the fetch effect after a mutation, which keeps all
  // the state updates inside the effect instead of in the event handlers.
  const [refreshKey, setRefreshKey] = useState(0);
  const reload = useCallback(() => setRefreshKey((key) => key + 1), []);

  useEffect(() => {
    if (!user) return;
    let cancelled = false;

    async function loadWatchlist() {
      try {
        const result = await api.getWatchlist(pageNumber, filter);
        if (cancelled) return;
        setPage(result);
        setError(null);
      } catch (err) {
        if (cancelled || err instanceof UnauthenticatedError) return; // useRequireAuth redirects
        setError(err instanceof ApiError ? err.message : "Could not load your watchlist");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    void loadWatchlist();
    return () => {
      cancelled = true;
    };
  }, [user, pageNumber, filter, refreshKey]);

  async function toggleStatus(item: WatchlistItem) {
    setBusyId(item.id);
    setError(null);
    const nextStatus: WatchlistStatus =
      item.status === "TO_WATCH" ? "WATCHED" : "TO_WATCH";
    try {
      // PUT replaces the whole resource on this API, so resend every field.
      await api.updateWatchlistItem(item.id, {
        imdbId: item.imdbId,
        title: item.title,
        year: item.year,
        poster: item.poster,
        status: nextStatus,
      });
      reload();
    } catch (err) {
      if (err instanceof UnauthenticatedError) return;
      setError(err instanceof ApiError ? err.message : "Could not update that item");
    } finally {
      setBusyId(null);
    }
  }

  async function remove(item: WatchlistItem) {
    setBusyId(item.id);
    setError(null);
    try {
      await api.deleteWatchlistItem(item.id);
      // Stepping back a page avoids landing on an empty last page.
      if (page && page.content.length === 1 && pageNumber > 0) {
        setPageNumber(pageNumber - 1);
      } else {
        reload();
      }
    } catch (err) {
      if (err instanceof UnauthenticatedError) return;
      setError(err instanceof ApiError ? err.message : "Could not delete that item");
    } finally {
      setBusyId(null);
    }
  }

  function changeFilter(value: WatchlistStatus | null) {
    setFilter(value);
    setPageNumber(0);
  }

  if (authLoading || !user) {
    return <p className="text-white/40">Loading…</p>;
  }

  return (
    <div>
      <div className="flex items-center justify-between gap-4 mb-6">
        <h1 className="text-2xl font-semibold">My Watchlist</h1>
        <Link
          href="/add"
          className="rounded-md bg-white text-black text-sm font-medium px-4 py-2 hover:bg-white/90 transition"
        >
          Add movies
        </Link>
      </div>

      <div className="flex gap-1 mb-6">
        {FILTERS.map((option) => (
          <button
            key={option.label}
            onClick={() => changeFilter(option.value)}
            className={`text-sm rounded-md px-3 py-1.5 transition ${
              filter === option.value
                ? "bg-white/15 text-white"
                : "text-white/50 hover:text-white hover:bg-white/5"
            }`}
          >
            {option.label}
          </button>
        ))}
      </div>

      {error && (
        <p role="alert" className="text-sm text-red-400 bg-red-400/10 rounded-md px-3 py-2 mb-4">
          {error}
        </p>
      )}

      {loading && <p className="text-white/40">Loading…</p>}

      {!loading && page && page.content.length === 0 && (
        <div className="border border-dashed border-white/15 rounded-xl py-16 text-center">
          <p className="text-white/60 mb-4">
            {filter ? "Nothing here yet." : "Your watchlist is empty."}
          </p>
          <Link href="/add" className="text-white underline underline-offset-4 text-sm">
            Find something to watch
          </Link>
        </div>
      )}

      {!loading && page && page.content.length > 0 && (
        <>
          <ul className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-5">
            {page.content.map((item) => (
              <li key={item.id} className="flex flex-col gap-2">
                <Poster src={item.poster} alt={item.title} />

                <div className="min-w-0">
                  <p className="text-sm font-medium truncate" title={item.title}>
                    {item.title}
                  </p>
                  <p className="text-xs text-white/40">{item.year}</p>
                </div>

                <span
                  className={`self-start text-[11px] rounded-full px-2 py-0.5 ${
                    item.status === "WATCHED"
                      ? "bg-emerald-400/15 text-emerald-300"
                      : "bg-amber-400/15 text-amber-300"
                  }`}
                >
                  {item.status === "WATCHED" ? "Watched" : "To watch"}
                </span>

                <div className="flex gap-2 mt-1">
                  <button
                    onClick={() => toggleStatus(item)}
                    disabled={busyId === item.id}
                    className="flex-1 text-xs rounded-md border border-white/15 px-2 py-1.5 hover:bg-white/5 disabled:opacity-40 transition"
                  >
                    {item.status === "WATCHED" ? "Mark unwatched" : "Mark watched"}
                  </button>
                  <button
                    onClick={() => remove(item)}
                    disabled={busyId === item.id}
                    aria-label={`Remove ${item.title}`}
                    className="text-xs rounded-md border border-white/15 px-2 py-1.5 text-red-300 hover:bg-red-400/10 disabled:opacity-40 transition"
                  >
                    Delete
                  </button>
                </div>
              </li>
            ))}
          </ul>

          {page.totalPages > 1 && (
            <div className="flex items-center justify-center gap-4 mt-10 text-sm">
              <button
                onClick={() => setPageNumber((n) => n - 1)}
                disabled={page.first}
                className="rounded-md border border-white/15 px-3 py-1.5 disabled:opacity-30 hover:bg-white/5 transition"
              >
                Previous
              </button>
              <span className="text-white/50">
                Page {page.number + 1} of {page.totalPages}
              </span>
              <button
                onClick={() => setPageNumber((n) => n + 1)}
                disabled={page.last}
                className="rounded-md border border-white/15 px-3 py-1.5 disabled:opacity-30 hover:bg-white/5 transition"
              >
                Next
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
