"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";

import { ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth";

export default function SignupPage() {
  const { user, loading, signup } = useAuth();
  const router = useRouter();

  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!loading && user) router.replace("/watchlist");
  }, [loading, user, router]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setFieldErrors({});
    setSubmitting(true);
    try {
      await signup(fullName, email, password);
      router.replace("/watchlist");
    } catch (err) {
      if (err instanceof ApiError) {
        // A 400 carries per-field messages; anything else (e.g. 409 for a
        // taken email) only has the summary detail.
        setFieldErrors(err.fieldErrors);
        if (Object.keys(err.fieldErrors).length === 0) setError(err.message);
      } else {
        setError("Something went wrong");
      }
    } finally {
      setSubmitting(false);
    }
  }

  const inputClass = (field: string) =>
    `w-full rounded-md bg-white/5 border px-3 py-2 outline-none transition ${
      fieldErrors[field] ? "border-red-400/60" : "border-white/10 focus:border-white/30"
    }`;

  return (
    <div className="max-w-sm mx-auto mt-12">
      <h1 className="text-2xl font-semibold mb-1">Create an account</h1>
      <p className="text-white/50 text-sm mb-6">Start tracking films to watch.</p>

      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        <div>
          <label htmlFor="fullName" className="block text-sm text-white/70 mb-1.5">
            Full name
          </label>
          <input
            id="fullName"
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            className={inputClass("fullName")}
          />
          {fieldErrors.fullName && (
            <p className="text-xs text-red-400 mt-1.5">{fieldErrors.fullName}</p>
          )}
        </div>

        <div>
          <label htmlFor="email" className="block text-sm text-white/70 mb-1.5">
            Email
          </label>
          <input
            id="email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className={inputClass("email")}
          />
          {fieldErrors.email && (
            <p className="text-xs text-red-400 mt-1.5">{fieldErrors.email}</p>
          )}
        </div>

        <div>
          <label htmlFor="password" className="block text-sm text-white/70 mb-1.5">
            Password
          </label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className={inputClass("password")}
          />
          {fieldErrors.password ? (
            <p className="text-xs text-red-400 mt-1.5">{fieldErrors.password}</p>
          ) : (
            <p className="text-xs text-white/35 mt-1.5">At least 8 characters.</p>
          )}
        </div>

        {error && (
          <p role="alert" className="text-sm text-red-400 bg-red-400/10 rounded-md px-3 py-2">
            {error}
          </p>
        )}

        <button
          type="submit"
          disabled={submitting}
          className="w-full rounded-md bg-white text-black font-medium py-2 hover:bg-white/90 disabled:opacity-50 transition"
        >
          {submitting ? "Creating account…" : "Sign up"}
        </button>
      </form>

      <p className="text-sm text-white/50 mt-6">
        Already have an account?{" "}
        <Link href="/login" className="text-white underline underline-offset-4">
          Log in
        </Link>
      </p>
    </div>
  );
}
