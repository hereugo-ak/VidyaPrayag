"use client";

import { Suspense, useMemo, useRef, useState } from "react";
import { useSearchParams } from "next/navigation";
import { useStudents, useTeachers } from "@/lib/admin/hooks";
import { adminApi } from "@/lib/admin/client";
import { mutate } from "swr";
import { ApiError } from "@/lib/api";
import type { StudentDto, TeacherAccountDto } from "@/lib/admin/types";
import { Card, EmptyState, FadeIn, Avatar, Badge } from "@/components/admin/Primitives";
import { DataTable, type Column } from "@/components/admin/DataTable";
import { Toolbar, Modal, AdminButton } from "@/components/admin/Toolbar";
import { IconPlus, IconTrash, IconPeople } from "@/components/admin/icons";

type Tab = "students" | "teachers";

export default function PeoplePage() {
  return (
    <Suspense fallback={<div className="h-40 animate-pulse rounded-2xl bg-navy/5" />}>
      <PeopleInner />
    </Suspense>
  );
}

function PeopleInner() {
  const params = useSearchParams();
  const initialAdd = params.get("add"); // "student" | "teacher" from dashboard quick-action
  const [tab, setTab] = useState<Tab>(initialAdd === "teacher" ? "teachers" : "students");

  return (
    <div className="space-y-5">
      {/* Tab switch */}
      <div className="inline-flex rounded-xl border border-navy/10 bg-white/70 p-1">
        {(["students", "teachers"] as Tab[]).map((t) => (
          <button
            key={t}
            type="button"
            onClick={() => setTab(t)}
            className={`rounded-lg px-4 py-2 text-[13.5px] font-semibold capitalize transition-colors duration-200 ${
              tab === t ? "bg-navy-deep text-white" : "text-ink-2 hover:text-navy-deep"
            }`}
          >
            {t}
          </button>
        ))}
      </div>

      {tab === "students" ? (
        <StudentsTab autoOpen={initialAdd === "student"} />
      ) : (
        <TeachersTab autoOpen={initialAdd === "teacher"} />
      )}
    </div>
  );
}

// ─────────────────────────── Students ───────────────────────────

function StudentsTab({ autoOpen }: { autoOpen?: boolean }) {
  const [q, setQ] = useState("");
  const [klass, setKlass] = useState("");
  const { data, isLoading } = useStudents();
  const all = useMemo(() => data?.students ?? [], [data]);

  const [addOpen, setAddOpen] = useState(!!autoOpen);
  const [importOpen, setImportOpen] = useState(false);

  const classes = useMemo(
    () => Array.from(new Set(all.map((s) => s.class_name))).sort(),
    [all]
  );

  const filtered = useMemo(() => {
    const needle = q.trim().toLowerCase();
    return all.filter(
      (s) =>
        (!klass || s.class_name === klass) &&
        (!needle ||
          s.full_name.toLowerCase().includes(needle) ||
          s.roll_number.toLowerCase().includes(needle) ||
          s.student_code.toLowerCase().includes(needle))
    );
  }, [all, q, klass]);

  async function refresh() {
    await mutate(["students", "", ""]);
  }

  async function remove(s: StudentDto) {
    if (!confirm(`Remove ${s.full_name}? This deletes their records.`)) return;
    await adminApi.deleteStudent(s.id);
    await refresh();
  }

  const columns: Column<StudentDto>[] = [
    {
      key: "name",
      header: "Student",
      accessor: (r) => r.full_name,
      sortable: true,
      cell: (r) => (
        <div className="flex items-center gap-3">
          <Avatar name={r.full_name} size={32} />
          <div>
            <p className="font-semibold text-navy-deep">{r.full_name}</p>
            <p className="text-[12px] text-ink-3">{r.student_code}</p>
          </div>
        </div>
      ),
    },
    { key: "class", header: "Class", accessor: (r) => r.class_name, sortable: true },
    { key: "section", header: "Section", accessor: (r) => r.section, sortable: true },
    { key: "roll", header: "Roll", accessor: (r) => r.roll_number, sortable: true },
    {
      key: "actions",
      header: "",
      accessor: () => "",
      align: "right",
      cell: (r) => (
        <button
          type="button"
          onClick={() => remove(r)}
          aria-label={`Remove ${r.full_name}`}
          className="rounded-lg p-2 text-ink-3 transition-colors hover:bg-danger/10 hover:text-danger"
        >
          <IconTrash width={17} height={17} />
        </button>
      ),
    },
  ];

  return (
    <FadeIn>
      <Card>
        <div className="border-b border-navy/8 p-4">
          <Toolbar
            query={q}
            onQuery={setQ}
            placeholder="Search name, roll or code…"
            filters={classes.map((c) => ({ value: c, label: c }))}
            active={klass}
            onFilter={setKlass}
            trailing={
              <>
                <AdminButton variant="ghost" onClick={() => setImportOpen(true)}>
                  Import CSV
                </AdminButton>
                <AdminButton onClick={() => setAddOpen(true)}>
                  <IconPlus width={16} height={16} /> Add student
                </AdminButton>
              </>
            }
          />
        </div>

        <DataTable
          columns={columns}
          rows={filtered}
          rowKey={(r) => r.id}
          loading={isLoading && !data}
          initialSort={{ key: "class", dir: "asc" }}
          emptyState={
            <EmptyState
              icon={<IconPeople width={26} height={26} />}
              title={q || klass ? "No students match your search" : "No students yet"}
              hint={
                q || klass
                  ? "Try a different name, class or roll number."
                  : "Add a student, or import a CSV to bring in your whole roster at once."
              }
            />
          }
        />
      </Card>

      <AddStudentModal open={addOpen} onClose={() => setAddOpen(false)} onDone={refresh} />
      <ImportStudentsModal open={importOpen} onClose={() => setImportOpen(false)} onDone={refresh} />
    </FadeIn>
  );
}

function AddStudentModal({
  open,
  onClose,
  onDone,
}: {
  open: boolean;
  onClose: () => void;
  onDone: () => Promise<void>;
}) {
  const [full_name, setName] = useState("");
  const [class_name, setKlass] = useState("");
  const [section, setSection] = useState("A");
  const [roll_number, setRoll] = useState("");
  const [student_code, setCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  function reset() {
    setName(""); setKlass(""); setSection("A"); setRoll(""); setCode(""); setErr(null);
  }

  async function submit() {
    setErr(null);
    if (!full_name.trim() || !class_name.trim() || !roll_number.trim()) {
      setErr("Name, class and roll number are required.");
      return;
    }
    setBusy(true);
    try {
      await adminApi.createStudent({
        full_name: full_name.trim(),
        class_name: class_name.trim(),
        section: section.trim() || undefined,
        roll_number: roll_number.trim(),
        student_code: student_code.trim() || undefined,
      });
      await onDone();
      reset();
      onClose();
    } catch (e) {
      setErr(e instanceof ApiError ? e.message : "Could not add student.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Add a student"
      description="Creates a student record scoped to your school."
      footer={
        <>
          <AdminButton variant="ghost" onClick={onClose}>Cancel</AdminButton>
          <AdminButton onClick={submit} disabled={busy}>
            {busy ? "Adding…" : "Add student"}
          </AdminButton>
        </>
      }
    >
      <div className="grid gap-3.5">
        <ModalField label="Full name" value={full_name} onChange={setName} />
        <div className="grid grid-cols-2 gap-3.5">
          <ModalField label="Class" value={class_name} onChange={setKlass} placeholder="Grade 1" />
          <ModalField label="Section" value={section} onChange={setSection} placeholder="A" />
        </div>
        <div className="grid grid-cols-2 gap-3.5">
          <ModalField label="Roll number" value={roll_number} onChange={setRoll} />
          <ModalField label="Student code (optional)" value={student_code} onChange={setCode} placeholder="auto" />
        </div>
        {err && <p className="text-[13px] font-medium text-danger">{err}</p>}
      </div>
    </Modal>
  );
}

function ImportStudentsModal({
  open,
  onClose,
  onDone,
}: {
  open: boolean;
  onClose: () => void;
  onDone: () => Promise<void>;
}) {
  const fileRef = useRef<HTMLInputElement>(null);
  const [csv, setCsv] = useState("");
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState<string | null>(null);
  const [err, setErr] = useState<string | null>(null);

  function loadFile(file: File) {
    const reader = new FileReader();
    reader.onload = () => setCsv(String(reader.result ?? ""));
    reader.readAsText(file);
  }

  async function submit() {
    setErr(null);
    setResult(null);
    if (!csv.trim()) {
      setErr("Paste CSV text or choose a .csv file first.");
      return;
    }
    setBusy(true);
    try {
      const res = await adminApi.importStudentsCsv(csv);
      setResult(`Imported ${res.inserted} of ${res.total}${res.failed ? ` · ${res.failed} skipped` : ""}.`);
      await onDone();
    } catch (e) {
      setErr(e instanceof ApiError ? e.message : "Import failed.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Import students from CSV"
      description="Header: full_name, class_name, roll_number, section, student_code"
      size="lg"
      footer={
        <>
          <AdminButton variant="ghost" onClick={onClose}>Close</AdminButton>
          <AdminButton onClick={submit} disabled={busy}>
            {busy ? "Importing…" : "Import"}
          </AdminButton>
        </>
      }
    >
      <div className="grid gap-3.5">
        <input
          ref={fileRef}
          type="file"
          accept=".csv,text/csv,text/plain"
          className="sr-only"
          onChange={(e) => {
            const f = e.target.files?.[0];
            if (f) loadFile(f);
            e.target.value = "";
          }}
        />
        <AdminButton variant="ghost" onClick={() => fileRef.current?.click()}>
          Choose .csv file
        </AdminButton>
        <textarea
          value={csv}
          onChange={(e) => setCsv(e.target.value)}
          rows={8}
          placeholder={"full_name,class_name,roll_number,section,student_code\nAarav Sharma,Grade 1,1,A,STU-0001"}
          className="w-full rounded-xl border border-navy/12 bg-white/80 p-3 font-mono text-[12.5px] text-ink outline-none focus:border-accent focus:bg-white"
        />
        {result && <p className="text-[13px] font-medium text-teal-deep">{result}</p>}
        {err && <p className="text-[13px] font-medium text-danger">{err}</p>}
      </div>
    </Modal>
  );
}

// ─────────────────────────── Teachers ───────────────────────────

function TeachersTab({ autoOpen }: { autoOpen?: boolean }) {
  const [q, setQ] = useState("");
  const { data, isLoading } = useTeachers();
  const all = useMemo(() => data?.teachers ?? [], [data]);
  const [addOpen, setAddOpen] = useState(!!autoOpen);

  const filtered = useMemo(() => {
    const needle = q.trim().toLowerCase();
    return all.filter(
      (t) =>
        !needle ||
        t.name.toLowerCase().includes(needle) ||
        (t.email ?? "").toLowerCase().includes(needle) ||
        (t.phone ?? "").toLowerCase().includes(needle)
    );
  }, [all, q]);

  async function refresh() {
    await mutate("teachers");
  }

  async function remove(t: TeacherAccountDto) {
    if (!confirm(`Remove ${t.name}? They will lose access immediately.`)) return;
    await adminApi.deleteTeacher(t.id);
    await refresh();
  }

  const columns: Column<TeacherAccountDto>[] = [
    {
      key: "name",
      header: "Teacher",
      accessor: (r) => r.name,
      sortable: true,
      cell: (r) => (
        <div className="flex items-center gap-3">
          <Avatar name={r.name} size={32} />
          <span className="font-semibold text-navy-deep">{r.name}</span>
        </div>
      ),
    },
    {
      key: "contact",
      header: "Sign-in",
      accessor: (r) => r.email ?? r.phone ?? "",
      cell: (r) =>
        r.email ? (
          <span className="text-ink-2">{r.email}</span>
        ) : r.phone ? (
          <span className="text-ink-2">
            {r.phone} <Badge tone="neutral">OTP</Badge>
          </span>
        ) : (
          <span className="text-ink-3">—</span>
        ),
    },
    {
      key: "actions",
      header: "",
      accessor: () => "",
      align: "right",
      cell: (r) => (
        <button
          type="button"
          onClick={() => remove(r)}
          aria-label={`Remove ${r.name}`}
          className="rounded-lg p-2 text-ink-3 transition-colors hover:bg-danger/10 hover:text-danger"
        >
          <IconTrash width={17} height={17} />
        </button>
      ),
    },
  ];

  return (
    <FadeIn>
      <Card>
        <div className="border-b border-navy/8 p-4">
          <Toolbar
            query={q}
            onQuery={setQ}
            placeholder="Search teachers…"
            trailing={
              <AdminButton onClick={() => setAddOpen(true)}>
                <IconPlus width={16} height={16} /> Add teacher
              </AdminButton>
            }
          />
        </div>

        <DataTable
          columns={columns}
          rows={filtered}
          rowKey={(r) => r.id}
          loading={isLoading && !data}
          initialSort={{ key: "name", dir: "asc" }}
          emptyState={
            <EmptyState
              icon={<IconPeople width={26} height={26} />}
              title={q ? "No teachers match your search" : "No teachers yet"}
              hint={
                q
                  ? "Try a different name, email or phone."
                  : "Add your first teacher — they'll appear in the faculty roster and analytics."
              }
            />
          }
        />
      </Card>

      <AddTeacherModal open={addOpen} onClose={() => setAddOpen(false)} onDone={refresh} />
    </FadeIn>
  );
}

function AddTeacherModal({
  open,
  onClose,
  onDone,
}: {
  open: boolean;
  onClose: () => void;
  onDone: () => Promise<void>;
}) {
  const [name, setName] = useState("");
  const [identifier, setIdentifier] = useState("");
  const [initial_password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const isEmail = identifier.includes("@");

  function reset() {
    setName(""); setIdentifier(""); setPassword(""); setErr(null);
  }

  async function submit() {
    setErr(null);
    if (!name.trim() || !identifier.trim()) {
      setErr("Name and an email or phone are required.");
      return;
    }
    if (isEmail && !initial_password.trim()) {
      setErr("An initial password is required when provisioning by email.");
      return;
    }
    setBusy(true);
    try {
      await adminApi.createTeacher({
        name: name.trim(),
        identifier: identifier.trim(),
        initial_password: initial_password.trim() || undefined,
      });
      await onDone();
      reset();
      onClose();
    } catch (e) {
      setErr(e instanceof ApiError ? e.message : "Could not add teacher.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Add a teacher"
      description="Provisions a teacher account and adds them to your faculty roster."
      footer={
        <>
          <AdminButton variant="ghost" onClick={onClose}>Cancel</AdminButton>
          <AdminButton onClick={submit} disabled={busy}>
            {busy ? "Adding…" : "Add teacher"}
          </AdminButton>
        </>
      }
    >
      <div className="grid gap-3.5">
        <ModalField label="Full name" value={name} onChange={setName} />
        <ModalField
          label="Email or phone"
          value={identifier}
          onChange={setIdentifier}
          placeholder="teacher@school.edu or 9876543210"
        />
        {isEmail && (
          <ModalField
            label="Initial password"
            value={initial_password}
            onChange={setPassword}
            type="text"
          />
        )}
        <p className="text-[12px] text-ink-3">
          {isEmail
            ? "Email teachers sign in with this password and are asked to change it on first login."
            : "Phone teachers sign in with a one-time code (OTP) — no password needed."}
        </p>
        {err && <p className="text-[13px] font-medium text-danger">{err}</p>}
      </div>
    </Modal>
  );
}

// ─────────────────────────── shared field ───────────────────────────

function ModalField({
  label,
  value,
  onChange,
  placeholder,
  type = "text",
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
  type?: string;
}) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-[11px] font-semibold uppercase tracking-wide text-ink-3">
        {label}
      </span>
      <input
        type={type}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className="w-full rounded-xl border border-navy/12 bg-white/80 px-3.5 py-2.5 text-[14px] text-ink outline-none transition-colors duration-200 placeholder:text-ink-placeholder focus:border-accent focus:bg-white"
      />
    </label>
  );
}
