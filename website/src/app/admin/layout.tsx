import type { Metadata } from "next";
import { AdminAuthProvider } from "@/lib/admin/session";
import { AdminShell } from "@/components/admin/AdminShell";

export const metadata: Metadata = {
  title: "Admin",
  robots: { index: false, follow: false },
};

/**
 * Authenticated admin section. Wrapped in the auth provider + protected shell;
 * the marketing Header/Footer do not appear here (separate route group).
 */
export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return (
    <AdminAuthProvider>
      <AdminShell>{children}</AdminShell>
    </AdminAuthProvider>
  );
}
