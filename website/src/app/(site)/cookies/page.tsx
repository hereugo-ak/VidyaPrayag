import type { Metadata } from "next";
import { LegalLayout, LegalSection } from "@/components/legal/LegalLayout";

export const metadata: Metadata = {
  title: "Cookie Policy — Enroll+",
  description:
    "How Enroll+ uses cookies and local storage on the website. We keep them to a minimum — essentials for signing in and preferences — and run no third-party advertising trackers.",
  alternates: { canonical: "/cookies" },
};

const UPDATED = "16 June 2026";

export default function CookiesPage() {
  return (
    <LegalLayout title="Cookie Policy" updated={UPDATED}>
      <p>
        This policy explains how the Enroll+ website uses cookies and similar
        browser storage. We keep it deliberately small: we use only what is
        needed to run the service and remember your choices. We do{" "}
        <strong>not</strong> run third-party advertising or cross-site tracking
        cookies.
      </p>

      <LegalSection title="1. What cookies are">
        <p>
          Cookies are small text files a website stores in your browser. Related
          technologies — <code>localStorage</code> and <code>sessionStorage</code> —
          let a site keep small amounts of data on your device between page loads.
          Together they let the site remember things like whether you are signed in.
        </p>
      </LegalSection>

      <LegalSection title="2. Essential cookies & storage">
        <p>
          These are required for the platform to function — you cannot turn them
          off without breaking sign-in:
        </p>
        <ul>
          <li><strong>Session token</strong> — keeps you signed in after you log in, so you don&apos;t re-enter credentials on every page.</li>
          <li><strong>Security tokens</strong> — protect forms and requests against cross-site forgery.</li>
          <li><strong>Load balancing</strong> — short-lived cookies set by our infrastructure to route requests reliably.</li>
        </ul>
      </LegalSection>

      <LegalSection title="3. Preference storage">
        <p>
          We use local storage to remember low-stakes choices so the product feels
          consistent between visits:
        </p>
        <ul>
          <li>Your language preference.</li>
          <li>Interface state such as a dismissed onboarding nudge.</li>
        </ul>
        <p>
          These never contain student data and are stored only on your own device.
        </p>
      </LegalSection>

      <LegalSection title="4. What we do not use">
        <ul>
          <li>No third-party advertising or retargeting cookies.</li>
          <li>No cross-site tracking of your browsing elsewhere.</li>
          <li>No sale of any cookie-derived data — ever.</li>
        </ul>
      </LegalSection>

      <LegalSection title="5. Managing cookies">
        <p>
          You can clear or block cookies in your browser settings at any time.
          Blocking essential cookies will sign you out and prevent you from logging
          back in until you allow them again. Clearing preference storage simply
          resets the relevant choices to their defaults.
        </p>
      </LegalSection>

      <LegalSection title="6. Relationship to our Privacy Policy">
        <p>
          Cookies are one small part of how we handle data. For the full picture of
          what the platform stores and why — accounts, school data, attendance,
          marks, fees, messaging and notifications — see our{" "}
          <a href="/privacy">Privacy Policy</a>.
        </p>
      </LegalSection>

      <LegalSection title="7. Changes to this policy">
        <p>
          We may update this policy as the website evolves. The &ldquo;last
          updated&rdquo; date above always reflects the current version.
        </p>
      </LegalSection>
    </LegalLayout>
  );
}
