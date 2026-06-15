"use client";

import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import { loadAuth, type StoredAuth } from "@/lib/auth";
import { Button } from "@/components/ui/Button";

/**
 * Onboarding success — confirmation after the REVIEW step's final submission.
 * The wizard clears the draft and routes here; the auth token remains so we can
 * greet the admin by name. No fake metrics — just a clear next-step path.
 */
export default function OnboardingSuccessPage() {
  const [auth, setAuth] = useState<StoredAuth | null>(null);

  useEffect(() => {
    setAuth(loadAuth());
  }, []);

  const firstName = auth?.name?.trim().split(/\s+/)[0];

  return (
    <div className="shell flex min-h-[70vh] items-center justify-center pb-24 pt-28">
      <motion.div
        className="max-w-xl text-center"
        initial={{ opacity: 0, y: 22 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, ease: [0.16, 1, 0.3, 1] }}
      >
        <motion.span
          className="mx-auto mb-8 flex h-16 w-16 items-center justify-center rounded-full bg-teal/10 text-teal-deep"
          initial={{ scale: 0.6, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          transition={{ delay: 0.15, duration: 0.4, ease: [0.16, 1, 0.3, 1] }}
        >
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <path d="M5 13l4 4L19 7" />
          </svg>
        </motion.span>

        <p className="eyebrow mb-3 text-center">You&apos;re live</p>
        <h1 className="display text-3xl leading-[1.1] sm:text-4xl">
          {firstName ? `Welcome aboard, ${firstName}.` : "Your school is set up."}
        </h1>
        <p className="mx-auto mt-5 max-w-md text-lg leading-relaxed text-ink-2">
          Your admin account and school are created. Next, add your teachers and invite
          parents to connect by roll number — everything flows from there.
        </p>

        <div className="mt-10 flex flex-col items-center justify-center gap-3 sm:flex-row">
          <Button href="/login" size="lg">
            Go to sign in
          </Button>
          <Button href="/features" size="lg" variant="secondary">
            See what&apos;s next
          </Button>
        </div>

        <p className="mt-8 text-sm text-ink-3">
          Tip: download the Enroll+ mobile app to manage on the go — same login.
        </p>
      </motion.div>
    </div>
  );
}
