"use client";

import Link from "next/link";
import { useAuth } from "@/lib/auth";

export function NavBar() {
  const { user, logout } = useAuth();

  return (
    <header className="border-b border-white/10">
      <nav className="w-full max-w-5xl mx-auto px-4 h-14 flex items-center justify-between gap-4">
        <Link href="/" className="font-semibold tracking-tight whitespace-nowrap">
          🎬 <span className="hidden sm:inline">Movie Watchlist</span>
        </Link>

        {user && (
          <div className="flex items-center gap-3 sm:gap-4 text-sm whitespace-nowrap">
            <Link href="/watchlist" className="text-white/70 hover:text-white transition">
              My list
            </Link>
            <Link href="/add" className="text-white/70 hover:text-white transition">
              Add movies
            </Link>
            <span className="text-white/40 hidden sm:inline">{user.email}</span>
            <button
              onClick={logout}
              className="rounded-md border border-white/15 px-3 py-1.5 text-white/80 hover:bg-white/5 transition"
            >
              Log out
            </button>
          </div>
        )}
      </nav>
    </header>
  );
}
