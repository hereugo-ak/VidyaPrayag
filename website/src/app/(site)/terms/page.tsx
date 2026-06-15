import type { Metadata } from "next";
import { LegalLayout, LegalSection } from "@/components/legal/LegalLayout";

export const metadata: Metadata = {
  title: "Terms of Service — Enroll+",
  description:
    "The terms governing the use of Enroll+ by schools, administrators, teachers and parents.",
  alternates: { canonical: "/terms" },
};

const UPDATED = "15 June 2026";

export default function TermsPage() {
  return (
    <LegalLayout title="Terms of Service" updated={UPDATED}>
      <p>
        These terms govern your use of Enroll+. By creating an account or using the
        website or apps, you agree to them. If you are accepting on behalf of a school,
        you confirm you are authorised to bind that school.
      </p>

      <LegalSection title="1. The service">
        <p>
          Enroll+ is a school-management platform providing administration, teaching and
          parent-facing tools across a website and mobile apps backed by a single system.
          We provide the service on an ongoing basis and may add, change or remove features
          to improve it.
        </p>
      </LegalSection>

      <LegalSection title="2. Accounts & roles">
        <ul>
          <li>A school administrator creates the school account through web onboarding.</li>
          <li>Administrators provision teacher and staff access; teachers set their own password on first login.</li>
          <li>Parents create their own account and connect to a child, subject to school approval.</li>
          <li>You are responsible for keeping your credentials secure and for activity under your account.</li>
        </ul>
      </LegalSection>

      <LegalSection title="3. Acceptable use">
        <p>You agree not to:</p>
        <ul>
          <li>Access data outside your role&apos;s scope or attempt to bypass access controls.</li>
          <li>Upload unlawful, harmful or infringing content, or misuse messaging features.</li>
          <li>Disrupt, probe or overload the service, or reverse-engineer it.</li>
          <li>Use the platform for any purpose other than legitimate school operations.</li>
        </ul>
      </LegalSection>

      <LegalSection title="4. School & user content">
        <p>
          Schools and users retain ownership of the data they enter (rosters, attendance,
          marks, messages and similar). You grant us the limited rights needed to host,
          process and display that data to deliver the service. You are responsible for the
          accuracy and lawfulness of the data you enter and for having the right to enter it.
        </p>
      </LegalSection>

      <LegalSection title="5. Privacy">
        <p>
          Our handling of personal data is described in the{" "}
          <a href="/privacy">Privacy Policy</a>, which forms part of these terms. Schools
          act as the controllers of their students&apos; data; we process it on their behalf to
          operate the service.
        </p>
      </LegalSection>

      <LegalSection title="6. Fees">
        <p>
          Paid plans are quoted per school. Fees, billing cycle and any applicable taxes are
          set out in your order or agreement with us. Non-payment may result in suspension of
          access after reasonable notice.
        </p>
      </LegalSection>

      <LegalSection title="7. Availability & support">
        <p>
          We work to keep the service available and secure but do not guarantee uninterrupted
          operation. We may perform maintenance and will aim to minimise disruption. Support is
          provided through the channels described in your agreement.
        </p>
      </LegalSection>

      <LegalSection title="8. Suspension & termination">
        <p>
          You may stop using the service at any time. We may suspend or terminate access for
          breach of these terms or unlawful use. On termination we handle your data as set out
          in the Privacy Policy.
        </p>
      </LegalSection>

      <LegalSection title="9. Disclaimers & liability">
        <p>
          The service is provided &ldquo;as is&rdquo; to the extent permitted by law. We are
          not liable for indirect or consequential losses, and our total liability is limited
          to the fees paid for the service in the preceding twelve months.
        </p>
      </LegalSection>

      <LegalSection title="10. Changes & contact">
        <p>
          We may update these terms; material changes will be communicated through the app or
          website. Continued use after changes take effect constitutes acceptance. Questions
          about these terms can be raised through your school administrator or our support
          channels.
        </p>
      </LegalSection>
    </LegalLayout>
  );
}
