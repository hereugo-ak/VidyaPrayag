import type { Metadata } from "next";
import { Hero } from "@/components/home/Hero";
import { SocialProof } from "@/components/home/SocialProof";
import { ForSchools } from "@/components/home/ForSchools";
import { ForParents } from "@/components/home/ForParents";
import { ForTeachers } from "@/components/home/ForTeachers";
import { HowItWorks } from "@/components/home/HowItWorks";
import { Testimonials } from "@/components/home/Testimonials";

export const metadata: Metadata = {
  title: "Enroll+ — The operating system for your school",
  description:
    "One platform connecting your office, your teachers, and every parent — attendance, results, fees, and messaging in real time. Onboard your school on the web in minutes.",
  alternates: { canonical: "/" },
};

/**
 * Homepage — hybrid SSG marketing page. Assembled from real-feature sections.
 * Order follows the brief: hero → social proof → for schools/parents/teachers
 * → how it works → testimonials (honest placeholder). The closing conversion
 * prompt lives in the footer's onboarding strip, so the page no longer carries
 * a separate full-bleed final-CTA slab.
 */
export default function HomePage() {
  return (
    <>
      <Hero />
      <SocialProof />
      <ForSchools />
      <ForParents />
      <ForTeachers />
      <HowItWorks />
      <Testimonials />
    </>
  );
}
