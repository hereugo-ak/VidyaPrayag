import type { Metadata } from "next";
import Link from "next/link";
import { PageHeader } from "@/components/ui/PageHeader";
import { Reveal, RevealGroup, RevealItem } from "@/components/ui/Reveal";
import { Button } from "@/components/ui/Button";
import { FOOTER_CONTACT } from "@/lib/content";

export const metadata: Metadata = {
  title: "Help & Support | Enroll+",
  description:
    "Get help with Enroll+ onboarding, teacher provisioning, parent linking, attendance, marks, fees and messaging. Real humans, fast answers, for admins, teachers and parents alike.",
  alternates: { canonical: "/support" },
};

/* Support channels, honest: the inbox is real, the response window is a
   commitment, not a vanity SLA. No invented phone line or chat widget. */
const CHANNELS = [
  {
    title: "Email support",
    body: "The fastest way to reach a real person. Include your school name and the role you're signing in as and we'll route it straight to the right place.",
    action: { label: FOOTER_CONTACT.support, href: `mailto:${FOOTER_CONTACT.support}` },
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="2" y="4" width="20" height="16" rx="2" /><path d="m22 7-10 6L2 7" /></svg>
    ),
  },
  {
    title: "Onboarding help",
    body: "Setting up your school for the first time? The four-step web wizard takes minutes, and if you get stuck on basics, branding or your academic structure, we'll walk you through it.",
    action: { label: "Onboard your school", href: "/onboarding" },
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 2 2 7l10 5 10-5-10-5Z" /><path d="m2 17 10 5 10-5M2 12l10 5 10-5" /></svg>
    ),
  },
  {
    title: "General enquiries",
    body: "Pricing for a group or trust, a question before you commit, or anything that isn't a live issue? Write to us and we'll get back to you.",
    action: { label: FOOTER_CONTACT.email, href: `mailto:${FOOTER_CONTACT.email}` },
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5Z" /></svg>
    ),
  },
];

/* FAQs, every answer maps to real product behaviour (see the privacy schema
   + onboarding wizard + role scoping). Nothing invented. */
const FAQS = [
  {
    role: "For administrators",
    items: [
      {
        q: "How do I get my school online?",
        a: "Create your admin account and walk through the four-step web wizard, basics, branding, academic structure, then launch. Your teachers and parents can come online the same day.",
      },
      {
        q: "How do teachers get access?",
        a: "You provision teachers as you set up classes and subjects. Each gets their own login and sees exactly the classes they teach, nothing else.",
      },
      {
        q: "Is our data isolated from other schools?",
        a: "Yes. All data is school-scoped. A teacher only sees their assigned classes, a parent only their approved child, and an admin only their own school.",
      },
    ],
  },
  {
    role: "For teachers",
    items: [
      {
        q: "Why can't I see a class I teach?",
        a: "Your home shows only the classes and subjects assigned to you in the school's structure. Ask your admin to check the assignment if one is missing.",
      },
      {
        q: "I entered marks once. Do parents see them automatically?",
        a: "Yes. Marks you enter against an assessment publish to parents and feed analytics in one step. There are no duplicate registers to keep.",
      },
    ],
  },
  {
    role: "For parents",
    items: [
      {
        q: "How do I connect to my child?",
        a: "Download the app, verify your phone, and request a link by your child's roll number. Once the school approves it, your child's day flows to your phone.",
      },
      {
        q: "When does attendance show up?",
        a: "Attendance marked in class appears the same morning. It's the same backend, so there's no overnight sync to wait for.",
      },
    ],
  },
];

export default function SupportPage() {
  return (
    <>
      <PageHeader
        eyebrow="Help & Support"
        crumbs={[{ label: "Home", href: "/" }, { label: "Support" }]}
        title="We're here when you need us."
        lede="Real humans, fast answers, for admins, teachers and parents alike. Most questions are covered below; for anything else, our inbox is genuinely monitored."
      >
        <div className="flex flex-col gap-3 sm:flex-row">
          <Button href={`mailto:${FOOTER_CONTACT.support}`} size="md">
            Email support
          </Button>
          <Button href="#faqs" variant="secondary" size="md">
            Browse FAQs
          </Button>
        </div>
      </PageHeader>

      {/* Channels */}
      <section className="shell py-16 md:py-20">
        <RevealGroup className="grid gap-6 md:grid-cols-3">
          {CHANNELS.map((c) => (
            <RevealItem
              key={c.title}
              className="flex flex-col rounded-xl2 border border-navy/8 bg-white/60 p-7"
            >
              <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-accent/10 text-accent-deep">
                {c.icon}
              </span>
              <h2 className="mt-5 text-lg font-extrabold tracking-tight text-navy-deep">
                {c.title}
              </h2>
              <p className="mt-2.5 flex-1 text-sm leading-relaxed text-ink-2">
                {c.body}
              </p>
              {c.action.href.startsWith("mailto:") ? (
                <a
                  href={c.action.href}
                  className="mt-5 inline-flex items-center gap-1.5 text-sm font-semibold text-accent transition-colors hover:text-accent-deep"
                >
                  {c.action.label}
                  <span aria-hidden>→</span>
                </a>
              ) : (
                <Link
                  href={c.action.href}
                  className="mt-5 inline-flex items-center gap-1.5 text-sm font-semibold text-accent transition-colors hover:text-accent-deep"
                >
                  {c.action.label}
                  <span aria-hidden>→</span>
                </Link>
              )}
            </RevealItem>
          ))}
        </RevealGroup>
      </section>

      {/* Status */}
      <section id="status" className="scroll-mt-24 border-y border-navy/8 bg-white/40 py-12">
        <div className="shell">
          <Reveal className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-center gap-3">
              <span className="relative flex h-2.5 w-2.5">
                <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-teal opacity-60" />
                <span className="relative inline-flex h-2.5 w-2.5 rounded-full bg-teal-deep" />
              </span>
              <div>
                <p className="text-sm font-bold tracking-tight text-navy-deep">
                  All systems operational
                </p>
                <p className="text-[13px] text-ink-3">
                  One backend serving the website and the mobile apps.
                </p>
              </div>
            </div>
            <p className="text-[13px] text-ink-3">
              Planned maintenance is announced in-app before it happens.
            </p>
          </Reveal>
        </div>
      </section>

      {/* FAQs */}
      <section id="faqs" className="scroll-mt-24 py-16 md:py-24">
        <div className="shell">
          <Reveal className="max-w-2xl">
            <p className="eyebrow mb-3">Frequently asked</p>
            <h2 className="display text-3xl leading-[1.08] sm:text-4xl">
              Answers, by who&apos;s asking.
            </h2>
          </Reveal>

          <div className="mt-12 space-y-12">
            {FAQS.map((group) => (
              <Reveal key={group.role}>
                <h3 className="text-[11px] font-bold uppercase tracking-[0.14em] text-accent">
                  {group.role}
                </h3>
                <div className="mt-5 divide-y divide-navy/8 overflow-hidden rounded-xl2 border border-navy/8 bg-white/60">
                  {group.items.map((f) => (
                    <details key={f.q} className="group px-6 py-5">
                      <summary className="flex cursor-pointer list-none items-center justify-between gap-4 text-[15px] font-bold tracking-tight text-navy-deep marker:hidden">
                        {f.q}
                        <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-navy/5 text-ink-2 transition-transform duration-200 ease-out-cubic group-open:rotate-45">
                          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round"><path d="M12 5v14M5 12h14" /></svg>
                        </span>
                      </summary>
                      <p className="mt-3 text-sm leading-relaxed text-ink-2">{f.a}</p>
                    </details>
                  ))}
                </div>
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="shell pb-24">
        <Reveal className="flex flex-col items-center gap-5 rounded-xl2 border border-navy/8 bg-lavender-soft/60 px-6 py-14 text-center">
          <h2 className="display text-2xl sm:text-3xl">Still stuck? Write to us.</h2>
          <p className="max-w-md text-ink-2">
            Tell us your school name and the role you&apos;re signing in as, and a
            real person will get back to you.
          </p>
          <Button href={`mailto:${FOOTER_CONTACT.support}`} size="lg">
            {FOOTER_CONTACT.support}
          </Button>
        </Reveal>
      </section>
    </>
  );
}
