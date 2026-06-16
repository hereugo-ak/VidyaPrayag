import type { Metadata } from "next";
import { LegalLayout, LegalSection } from "@/components/legal/LegalLayout";

export const metadata: Metadata = {
  title: "Privacy Policy — Enroll+",
  description:
    "How Enroll+ collects, uses and protects the data of schools, administrators, teachers, parents and students. Written to reflect exactly what the platform stores.",
  alternates: { canonical: "/privacy" },
};

const UPDATED = "16 June 2026";

export default function PrivacyPage() {
  return (
    <LegalLayout title="Privacy Policy" updated={UPDATED}>
      <p>
        This policy explains what information Enroll+ (&ldquo;we&rdquo;, &ldquo;the
        platform&rdquo;) collects, why we collect it, and the choices you have. It is
        written to reflect the data the platform actually stores — nothing is collected
        that isn&apos;t described here. Enroll+ is operated as a single backend serving both
        the website and the mobile apps; the records below are shared across both.
      </p>

      <LegalSection title="1. Who this applies to">
        <p>
          Enroll+ serves four kinds of people, each with a distinct data footprint:
        </p>
        <ul>
          <li><strong>School administrators</strong> — create the school account and manage it.</li>
          <li><strong>Teachers</strong> — provisioned by their school; record attendance and marks.</li>
          <li><strong>Parents</strong> — connect to their child by roll number and view progress.</li>
          <li><strong>Students &amp; non-teaching staff</strong> — roster records managed by the school (students and non-teaching staff do not log in).</li>
        </ul>
      </LegalSection>

      <LegalSection title="2. Account & identity data">
        <p>
          When an account is created (an admin via web onboarding, or a teacher/parent in
          the app) we store, in our <code>app_users</code> record:
        </p>
        <ul>
          <li>Full name</li>
          <li>Phone number and/or email address (used as your login identifier)</li>
          <li>A securely hashed password (we never store the plaintext)</li>
          <li>Your role (admin, teacher, parent or staff) and the school you belong to</li>
          <li>Language preference, an optional profile photo URL, and verification flags</li>
          <li>Timestamps for account creation, updates and last login</li>
        </ul>
      </LegalSection>

      <LegalSection title="3. School data">
        <p>
          During onboarding the administrator provides institutional details stored in our
          <code> schools</code> and related tables: school name, board, medium, school type,
          contact email and phone, address (city, district, state, pincode), brand colour and
          logo, and the academic structure — classes, sections and subjects.
        </p>
      </LegalSection>

      <LegalSection title="4. Student & child data">
        <p>
          Schools maintain a student roster (<code>students</code>) containing each student&apos;s
          name, a unique student code, class, section and roll number, and an optional photo.
          Parents may register their child (<code>children</code>) with a name, optional date of
          birth, gender, current grade and interests. When a parent requests a connection it is
          recorded in <code>parent_child_links</code>; once the school approves it, the child is
          matched to the school&apos;s canonical student record by roll number. A parent only ever
          sees a child whose link has been approved.
        </p>
      </LegalSection>

      <LegalSection title="5. Operational records">
        <p>
          As the school uses the platform, staff create operational records tied to students
          and classes. Each lives in a specific table, and we store only the fields needed to
          provide that feature:
        </p>
        <ul>
          <li><strong>Attendance</strong> (<code>attendance_records</code>) — per-student, per-day status marked by teachers.</li>
          <li><strong>Marks &amp; results</strong> (<code>assessment_marks</code>, <code>assessments</code>, <code>exam_results</code>) — scores entered against an assessment, plus published exam results.</li>
          <li><strong>Fees</strong> (<code>fee_records</code>) — a per-child ledger of paid, due and overdue line items.</li>
          <li><strong>Messaging</strong> (<code>messages</code>, <code>message_threads</code>, <code>announcements</code>, <code>whatsapp_logs</code>) — audience-scoped announcements, message threads, and delivery logs.</li>
          <li><strong>Notifications</strong> (<code>notifications</code>) — in-app alerts generated for you by school activity.</li>
          <li><strong>Leave, PTMs &amp; coursework</strong> (<code>leave_requests</code>, <code>ptm_events</code>, <code>homework</code>, <code>syllabus_units</code>) — leave applications, parent-teacher meetings, homework and syllabus tracking.</li>
          <li><strong>Admissions</strong> (<code>admission_enquiries</code>) — prospects in the admissions pipeline.</li>
        </ul>
        <p>
          Analytics shown in the product — class and teacher performance, per-student trends,
          syllabus coverage, and the early-warning list of at-risk students — are computed
          directly from these real entries. We do not fabricate, estimate or invent any figure;
          where the underlying data is absent, the product says so rather than showing a made-up
          number.
        </p>
      </LegalSection>

      <LegalSection title="6. Technical & security data">
        <p>
          To keep accounts secure we store session tokens (<code>user_sessions</code>),
          one-time-password records for phone/email verification (<code>auth_otps</code> and
          <code> otp_delivery_attempts</code>), and device push tokens (<code>device_tokens</code>)
          used to deliver the <code>notifications</code> described above. We log request
          timestamps for security and abuse-prevention purposes.
        </p>
      </LegalSection>

      <LegalSection title="7. How we use data">
        <ul>
          <li>To operate the platform — authenticate you and show the right scoped data.</li>
          <li>To deliver announcements and notifications you or your school configure.</li>
          <li>To compute the analytics and rollups your school relies on.</li>
          <li>To keep accounts secure and prevent misuse.</li>
        </ul>
        <p>We do not sell personal data, and we do not use student data for advertising.</p>
      </LegalSection>

      <LegalSection title="8. Data sharing & scope">
        <p>
          All data is <strong>school-scoped</strong>. A teacher only sees the classes they are
          assigned; a parent only sees their own approved child; an admin sees only their own
          school. We share data with infrastructure providers (database and messaging delivery)
          strictly to operate the service, and with authorities only where required by law.
        </p>
      </LegalSection>

      <LegalSection title="9. Retention & deletion">
        <p>
          Roster records (students, non-teaching staff) are soft-deletable by the school and
          retained while the account is active. A school administrator can request export or
          deletion of their school&apos;s data; on account closure we delete or anonymise personal
          data within a reasonable period, subject to legal retention requirements.
        </p>
      </LegalSection>

      <LegalSection title="10. Children's privacy">
        <p>
          Student and child records are created and controlled by the responsible school and
          the linked parent — not by the child directly. Parents control their child&apos;s
          connection and can request its removal at any time.
        </p>
      </LegalSection>

      <LegalSection title="11. Your choices">
        <ul>
          <li>Access and correct your profile information from within the app.</li>
          <li>Parents can disconnect a child link; admins can remove roster records.</li>
          <li>Contact your school administrator, or us, to request export or deletion.</li>
        </ul>
      </LegalSection>

      <LegalSection title="12. Cookies & local storage">
        <p>
          On the website we keep cookies and local storage to a minimum — essentially what is
          needed to keep you signed in and to remember preferences. We do not run third-party
          advertising trackers. The full detail lives in our{" "}
          <a href="/cookies">Cookie Policy</a>.
        </p>
      </LegalSection>

      <LegalSection title="13. Changes to this policy">
        <p>
          We may update this policy as the platform evolves. Material changes will be
          communicated through the app or website. The &ldquo;last updated&rdquo; date above
          always reflects the current version.
        </p>
      </LegalSection>
    </LegalLayout>
  );
}
