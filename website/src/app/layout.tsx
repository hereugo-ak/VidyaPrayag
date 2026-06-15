import type { Metadata, Viewport } from "next";
import { Plus_Jakarta_Sans, DM_Mono } from "next/font/google";
import "./globals.css";
import { Header } from "@/components/Header";
import { Footer } from "@/components/Footer";

const jakarta = Plus_Jakarta_Sans({
  subsets: ["latin"],
  weight: ["400", "500", "600", "700", "800"],
  variable: "--font-jakarta",
  display: "swap",
});

const dmMono = DM_Mono({
  subsets: ["latin"],
  weight: ["400", "500"],
  variable: "--font-dmmono",
  display: "swap",
});

const SITE_URL = "https://enrollplus.app";

export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  title: {
    default: "Enroll+ — The operating system for your school",
    template: "%s · Enroll+",
  },
  description:
    "Enroll+ connects your school office, your teachers, and every parent on one platform — attendance, results, fees, and messaging in real time. Onboard your school in minutes.",
  keywords: [
    "school management software",
    "school ERP India",
    "parent teacher app",
    "school attendance",
    "CBSE ICSE school platform",
    "Enroll+",
  ],
  openGraph: {
    title: "Enroll+ — The operating system for your school",
    description:
      "One platform connecting your office, your teachers, and every parent. Onboard your school in minutes.",
    url: SITE_URL,
    siteName: "Enroll+",
    type: "website",
  },
  twitter: {
    card: "summary_large_image",
    title: "Enroll+ — The operating system for your school",
    description:
      "One platform connecting your office, your teachers, and every parent.",
  },
  robots: { index: true, follow: true },
};

export const viewport: Viewport = {
  themeColor: "#E6E6FA",
  width: "device-width",
  initialScale: 1,
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className={`${jakarta.variable} ${dmMono.variable}`}>
      <body>
        <Header />
        <main id="main">{children}</main>
        <Footer />
      </body>
    </html>
  );
}
