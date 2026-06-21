import { Header } from "@/components/Header";
import { Footer } from "@/components/Footer";

/**
 * Marketing/onboarding shell, the public site. The authenticated admin app
 * lives under /admin with its own chrome (sidebar, no marketing header/footer).
 */
export default function SiteLayout({ children }: { children: React.ReactNode }) {
  return (
    <>
      <a
        href="#main"
        className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-[60] focus:rounded-full focus:bg-navy-deep focus:px-5 focus:py-2.5 focus:text-sm focus:font-semibold focus:text-white"
      >
        Skip to content
      </a>
      <Header />
      <main id="main">{children}</main>
      <Footer />
    </>
  );
}
