"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { motion } from "framer-motion";
import { loginAdmin, ApiError } from "@/lib/api";
import { saveAuth } from "@/lib/auth";
import { TextField } from "@/components/ui/Field";
import { Button } from "@/components/ui/Button";
import { Photo } from "@/components/ui/Photo";
import { PHOTOS } from "@/lib/images";

/**
 * Admin sign-in. Calls /api/v1/auth/login with role: "school_admin".
 * Onboarding (account creation) lives at /onboarding — this is for returning admins.
 */
export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    if (!email.trim() || !password) {
      setError("Enter your email and password.");
      return;
    }
    setSubmitting(true);
    try {
      const res = await loginAdmin(email.trim().toLowerCase(), password);
      saveAuth(res);
      // If onboarding isn't finished, route them back into the wizard.
      router.push(res.profile_completed ? "/onboarding/success" : "/onboarding");
    } catch (err) {
      setError(
        err instanceof ApiError
          ? err.message
          : "Couldn't sign you in. Please try again."
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="shell grid min-h-[80vh] items-center gap-12 pb-24 pt-28 lg:grid-cols-2 md:pt-32">
      {/* Form */}
      <motion.div
        className="mx-auto w-full max-w-md"
        initial={{ opacity: 0, y: 22 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, ease: [0.16, 1, 0.3, 1] }}
      >
        <p className="eyebrow mb-3">Welcome back</p>
        <h1 className="display text-3xl leading-[1.1] sm:text-4xl">Sign in to Enroll+</h1>
        <p className="mt-3 text-ink-2">
          For school administrators. Teachers and parents sign in from the mobile app.
        </p>

        <form onSubmit={onSubmit} className="mt-9 grid gap-4">
          <TextField
            label="Email"
            type="email"
            inputMode="email"
            value={email}
            onChange={setEmail}
            autoComplete="email"
          />
          <TextField
            label="Password"
            type="password"
            value={password}
            onChange={setPassword}
            autoComplete="current-password"
          />

          {error && (
            <p className="rounded-xl border border-danger/30 bg-danger/5 px-4 py-3 text-sm font-medium text-danger">
              {error}
            </p>
          )}

          <Button type="submit" size="lg" disabled={submitting} className="mt-1 w-full">
            {submitting ? "Signing in…" : "Sign in"}
          </Button>
        </form>

        <p className="mt-6 text-sm text-ink-3">
          Don&apos;t have an account yet?{" "}
          <a href="/onboarding" className="font-semibold text-accent hover:underline">
            Onboard your school
          </a>
        </p>
      </motion.div>

      {/* Photo */}
      <motion.div
        className="hidden lg:block"
        initial={{ opacity: 0, y: 22 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, delay: 0.1, ease: [0.16, 1, 0.3, 1] }}
      >
        <Photo photo={PHOTOS.collaboration} ratio="4/5" sizes="50vw" />
      </motion.div>
    </div>
  );
}
