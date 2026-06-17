import type { Metadata } from "next";
import { Wizard } from "@/components/onboarding/Wizard";

export const metadata: Metadata = {
  title: "Onboard your school | Enroll+",
  description:
    "Set up your school on Enroll+ in five short steps: create your admin account, add your basics, branding and academic structure, then launch.",
  robots: { index: false }, // transactional flow, not a marketing target
};

export default function OnboardingPage() {
  return (
    <div className="shell pb-24 pt-28 md:pt-32">
      <Wizard />
    </div>
  );
}
