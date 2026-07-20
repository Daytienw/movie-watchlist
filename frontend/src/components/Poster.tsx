/* eslint-disable @next/next/no-img-element */
"use client";

import { useState } from "react";

/**
 * OMDb returns the literal string "N/A" when it has no artwork, and some of
 * the URLs it does return are dead, so fall back on both.
 */
export function Poster({ src, alt }: { src: string; alt: string }) {
  const [failed, setFailed] = useState(false);
  const missing = !src || src === "N/A" || failed;

  if (missing) {
    return (
      <div className="aspect-[2/3] w-full rounded-lg bg-white/5 grid place-items-center text-white/25 text-xs px-2 text-center">
        No poster
      </div>
    );
  }

  return (
    <img
      src={src}
      alt={alt}
      onError={() => setFailed(true)}
      className="aspect-[2/3] w-full rounded-lg object-cover bg-white/5"
    />
  );
}
