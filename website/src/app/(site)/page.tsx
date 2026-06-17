import type { Metadata } from "next";
import { Hero } from "@/components/home/Hero";
import { ForSchools } from "@/components/home/ForSchools";
import { ForParents } from "@/components/home/ForParents";
import { ForTeachers } from "@/components/home/ForTeachers";
import { HowItWorks } from "@/components/home/HowItWorks";
import { Testimonials } from "@/components/home/Testimonials";

export const metadata: Metadata = {
  title: "Enroll+, The operating system for your school",
  description:
    "One platform connecting your office, your teachers, and every parent, attendance, results, fees, and messaging in real time. Onboard your school on the web in minutes.",
  alternates: { canonical: "/" },
};

/**
 * Homepage, a hybrid SSG marketing page assembled from real-feature sections.
 * Order: hero, for schools/parents/teachers, how it works, then the partner
 * schools statement (a confident pre-launch moment, not fake social proof).
 * The closing conversion prompt lives in the footer's onboarding section, so
 * the page carries no separate full-bleed final-CTA slab.
 */
export default function HomePage() {
  return (
    <>
      <Hero />
      <ForSchools />
      <ForParents />
      <ForTeachers />
      <HowItWorks />
      <Testimonials />
    </>
  );
}
