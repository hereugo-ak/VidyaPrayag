import type { Metadata } from "next";
import { Hero } from "@/components/home/Hero";
import { ForSchools } from "@/components/home/ForSchools";
import { ForTeachers } from "@/components/home/ForTeachers";
import { HowItWorks } from "@/components/home/HowItWorks";
import { Testimonials } from "@/components/home/Testimonials";

export const metadata: Metadata = {
  title: "Enroll+, The OS for your campus",
  description:
    "One platform connecting your office, your teachers, and every parent, attendance, results, fees, and messaging in real time. Onboard your school on the web in minutes.",
  alternates: { canonical: "/" },
};

/**
 * Homepage, a hybrid SSG marketing page assembled from real-feature sections.
 * Order: hero, for schools/teachers, how it works, testimonials. The parents
 * story now has its own dedicated /parents page (linked from the header and
 * footer) and is intentionally NOT repeated here, so the homepage moves crisply
 * from the institutional pitch to teacher impact to social proof without
 * duplicating the parent narrative. The closing conversion prompt lives in the
 * footer's onboarding section, so the page carries no separate final-CTA slab.
 */
export default function HomePage() {
  return (
    <>
      <Hero />
      <ForSchools />
      <ForTeachers />
      <HowItWorks />
      <Testimonials />
    </>
  );
}
