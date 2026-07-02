"use client";

import { useMemo, useState } from "react";
import { useSchoolClasses, useSchoolSubjects, useTeachers, useTimetable } from "@/lib/admin/hooks";
import { adminApi } from "@/lib/admin/client";
import { mutate } from "swr";
import { ApiError } from "@/lib/api";
import type {
  SchoolClassDto,
  SchoolSubjectDto,
  CreatePeriodRequest,
  UpdatePeriodRequest,
  BulkPeriodItem,
} from "@/lib/admin/types";
import { Card, EmptyState, FadeIn, Badge } from "@/components/admin/Primitives";
import { DataTable, type Column } from "@/components/admin/DataTable";
import { Toolbar, Modal, AdminButton } from "@/components/admin/Toolbar";
import { IconPlus, IconTrash, IconBook, IconCalendar } from "@/components/admin/icons";

type Tab = "classes" | "subjects" | "timetable";

const WEEKDAYS = ["", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

export default function AcademicsPage() {
  const [tab, setTab] = useState<Tab>("classes");

  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-2xl font-bold text-navy-deep">Academics</h1>
        <p className="text-[14px] text-ink-3 mt-0.5">
          Manage classes, subjects, and the weekly timetable.
        </p>
      </div>

      <div className="inline-flex rounded-xl border border-navy/10 bg-white/70 p-1">
        {(["classes", "subjects", "timetable"] as Tab[]).map((t) => (
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

      {tab === "classes" && <ClassesTab />}
      {tab === "subjects" && <SubjectsTab />}
      {tab === "timetable" && <TimetableTab />}
    </div>
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

// ─────────────────────────── Classes ───────────────────────────

function ClassesTab() {
  const { data, isLoading } = useSchoolClasses();
  const all = useMemo(() => data?.classes ?? [], [data]);
  const [addOpen, setAddOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<SchoolClassDto | null>(null);

  async function refresh() {
    await mutate("school-classes");
  }

  async function remove(c: SchoolClassDto) {
    if (!confirm(`Delete class ${c.name} (${c.code})? This also deletes all subjects in it.`)) return;
    try {
      await adminApi.deleteSchoolClass(c.id);
      await refresh();
    } catch (e) {
      alert(e instanceof ApiError ? e.message : "Could not delete class.");
    }
  }

  const columns: Column<SchoolClassDto>[] = [
    {
      key: "name",
      header: "Class",
      accessor: (r) => r.name,
      sortable: true,
      cell: (r) => (
        <div>
          <p className="font-semibold text-navy-deep">{r.name}</p>
          <p className="text-[12px] text-ink-3">{r.code}</p>
        </div>
      ),
    },
    {
      key: "sections",
      header: "Sections",
      accessor: (r) => r.sections.join(", "),
      cell: (r) => (
        <div className="flex flex-wrap gap-1">
          {r.sections.map((s) => (
            <Badge key={s}>{s}</Badge>
          ))}
        </div>
      ),
    },
    {
      key: "subject_count",
      header: "Subjects",
      accessor: (r) => r.subject_count.toString(),
      sortable: true,
    },
    {
      key: "actions",
      header: "",
      accessor: () => "",
      align: "right",
      cell: (r) => (
        <div className="flex items-center justify-end gap-1">
          <button
            type="button"
            onClick={() => setEditTarget(r)}
            className="rounded-lg px-3 py-1.5 text-[12.5px] font-medium text-navy-deep transition-colors hover:bg-navy/5"
          >
            Edit
          </button>
          <button
            type="button"
            onClick={() => remove(r)}
            aria-label={`Delete ${r.name}`}
            className="rounded-lg p-2 text-ink-3 transition-colors hover:bg-danger/10 hover:text-danger"
          >
            <IconTrash width={17} height={17} />
          </button>
        </div>
      ),
    },
  ];

  return (
    <FadeIn>
      <Card>
        <div className="border-b border-navy/8 p-4">
          <Toolbar
            query=""
            onQuery={() => {}}
            placeholder=""
            trailing={
              <AdminButton onClick={() => setAddOpen(true)}>
                <IconPlus width={16} height={16} /> Add class
              </AdminButton>
            }
          />
        </div>
        <DataTable
          columns={columns}
          rows={all}
          rowKey={(r) => r.id}
          loading={isLoading && !data}
          initialSort={{ key: "name", dir: "asc" }}
          emptyState={
            <EmptyState
              icon={<IconBook width={26} height={26} />}
              title="No classes yet"
              hint="Add a class to start defining subjects and timetable."
            />
          }
        />
      </Card>

      <AddClassModal open={addOpen} onClose={() => setAddOpen(false)} onDone={refresh} />
      {editTarget && (
        <EditClassModal target={editTarget} onClose={() => setEditTarget(null)} onDone={refresh} />
      )}
    </FadeIn>
  );
}

function AddClassModal({
  open,
  onClose,
  onDone,
}: {
  open: boolean;
  onClose: () => void;
  onDone: () => Promise<void>;
}) {
  const [code, setCode] = useState("");
  const [name, setName] = useState("");
  const [sections, setSections] = useState("A");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  function reset() {
    setCode(""); setName(""); setSections("A"); setErr(null);
  }

  async function submit() {
    setErr(null);
    if (!code.trim() || !name.trim()) {
      setErr("Code and name are required.");
      return;
    }
    setBusy(true);
    try {
      await adminApi.createSchoolClass({
        code: code.trim(),
        name: name.trim(),
        sections: sections.split(",").map((s) => s.trim()).filter(Boolean),
      });
      await onDone();
      reset();
      onClose();
    } catch (e) {
      setErr(e instanceof ApiError ? e.message : "Could not add class.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Add a class"
      description="Create a new class with sections for your school."
      footer={
        <>
          <AdminButton variant="ghost" onClick={onClose}>Cancel</AdminButton>
          <AdminButton onClick={submit} disabled={busy}>
            {busy ? "Adding…" : "Add class"}
          </AdminButton>
        </>
      }
    >
      <div className="grid gap-3.5">
        <ModalField label="Class name" value={name} onChange={setName} placeholder="Grade 1" />
        <ModalField label="Code" value={code} onChange={setCode} placeholder="G1" />
        <ModalField label="Sections (comma-separated)" value={sections} onChange={setSections} placeholder="A, B, C" />
        {err && <p className="text-[13px] font-medium text-danger">{err}</p>}
      </div>
    </Modal>
  );
}

function EditClassModal({
  target,
  onClose,
  onDone,
}: {
  target: SchoolClassDto;
  onClose: () => void;
  onDone: () => Promise<void>;
}) {
  const [code, setCode] = useState(target.code);
  const [name, setName] = useState(target.name);
  const [sections, setSections] = useState(target.sections.join(", "));
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  async function submit() {
    setErr(null);
    if (!code.trim() || !name.trim()) {
      setErr("Code and name are required.");
      return;
    }
    setBusy(true);
    try {
      await adminApi.updateSchoolClass(target.id, {
        code: code.trim(),
        name: name.trim(),
        sections: sections.split(",").map((s) => s.trim()).filter(Boolean),
      });
      await onDone();
      onClose();
    } catch (e) {
      setErr(e instanceof ApiError ? e.message : "Could not update class.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal
      open={true}
      onClose={onClose}
      title="Edit class"
      description={`Update ${target.name} (${target.code})`}
      footer={
        <>
          <AdminButton variant="ghost" onClick={onClose}>Cancel</AdminButton>
          <AdminButton onClick={submit} disabled={busy}>
            {busy ? "Saving…" : "Save changes"}
          </AdminButton>
        </>
      }
    >
      <div className="grid gap-3.5">
        <ModalField label="Class name" value={name} onChange={setName} />
        <ModalField label="Code" value={code} onChange={setCode} />
        <ModalField label="Sections (comma-separated)" value={sections} onChange={setSections} />
        {err && <p className="text-[13px] font-medium text-danger">{err}</p>}
      </div>
    </Modal>
  );
}

// ─────────────────────────── Subjects ───────────────────────────

function SubjectsTab() {
  const { data: classData } = useSchoolClasses();
  const classes = useMemo(() => classData?.classes ?? [], [classData]);
  const [selectedClassId, setSelectedClassId] = useState<string | null>(null);
  const { data: subjData, isLoading } = useSchoolSubjects(selectedClassId);
  const subjects = useMemo(() => subjData?.subjects ?? [], [subjData]);
  const [addOpen, setAddOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<SchoolSubjectDto | null>(null);

  const selectedClass = classes.find((c) => c.id === selectedClassId);

  async function refresh() {
    if (selectedClassId) await mutate(["school-subjects", selectedClassId]);
    await mutate("school-classes");
  }

  async function remove(s: SchoolSubjectDto) {
    if (!confirm(`Delete subject ${s.name} (${s.code})?`)) return;
    try {
      await adminApi.deleteSchoolSubject(s.id);
      await refresh();
    } catch (e) {
      alert(e instanceof ApiError ? e.message : "Could not delete subject.");
    }
  }

  const columns: Column<SchoolSubjectDto>[] = [
    {
      key: "name",
      header: "Subject",
      accessor: (r) => r.name,
      sortable: true,
      cell: (r) => (
        <div>
          <p className="font-semibold text-navy-deep">{r.name}</p>
          <p className="text-[12px] text-ink-3">{r.code}</p>
        </div>
      ),
    },
    {
      key: "actions",
      header: "",
      accessor: () => "",
      align: "right",
      cell: (r) => (
        <div className="flex items-center justify-end gap-1">
          <button
            type="button"
            onClick={() => setEditTarget(r)}
            className="rounded-lg px-3 py-1.5 text-[12.5px] font-medium text-navy-deep transition-colors hover:bg-navy/5"
          >
            Edit
          </button>
          <button
            type="button"
            onClick={() => remove(r)}
            className="rounded-lg p-2 text-ink-3 transition-colors hover:bg-danger/10 hover:text-danger"
          >
            <IconTrash width={17} height={17} />
          </button>
        </div>
      ),
    },
  ];

  return (
    <FadeIn>
      <Card>
        <div className="border-b border-navy/8 p-4">
          <div className="flex items-center gap-3">
            <select
              value={selectedClassId ?? ""}
              onChange={(e) => setSelectedClassId(e.target.value || null)}
              className="rounded-lg border border-navy/15 bg-white px-3 py-2 text-[14px] font-medium text-navy-deep outline-none focus:border-navy-deep"
            >
              <option value="">Select a class…</option>
              {classes.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} ({c.code})
                </option>
              ))}
            </select>
            {selectedClass && (
              <AdminButton onClick={() => setAddOpen(true)}>
                <IconPlus width={16} height={16} /> Add subject
              </AdminButton>
            )}
          </div>
        </div>

        {selectedClassId ? (
          <DataTable
            columns={columns}
            rows={subjects}
            rowKey={(r) => r.id}
            loading={isLoading && !subjData}
            initialSort={{ key: "name", dir: "asc" }}
            emptyState={
              <EmptyState
                icon={<IconBook width={26} height={26} />}
                title="No subjects yet"
                hint="Add a subject to this class to start building your timetable."
              />
            }
          />
        ) : (
          <div className="p-10">
            <EmptyState
              icon={<IconBook width={26} height={26} />}
              title="Select a class"
              hint="Choose a class above to view and manage its subjects."
            />
          </div>
        )}
      </Card>

      {selectedClass && (
        <AddSubjectModal
          classId={selectedClass.id}
          open={addOpen}
          onClose={() => setAddOpen(false)}
          onDone={refresh}
        />
      )}
      {editTarget && (
        <EditSubjectModal
          target={editTarget}
          onClose={() => setEditTarget(null)}
          onDone={refresh}
        />
      )}
    </FadeIn>
  );
}

function AddSubjectModal({
  classId,
  open,
  onClose,
  onDone,
}: {
  classId: string;
  open: boolean;
  onClose: () => void;
  onDone: () => Promise<void>;
}) {
  const [name, setName] = useState("");
  const [code, setCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  function reset() {
    setName(""); setCode(""); setErr(null);
  }

  async function submit() {
    setErr(null);
    if (!name.trim() || !code.trim()) {
      setErr("Subject name and code are required.");
      return;
    }
    setBusy(true);
    try {
      await adminApi.createSchoolSubject(classId, { name: name.trim(), code: code.trim() });
      await onDone();
      reset();
      onClose();
    } catch (e) {
      setErr(e instanceof ApiError ? e.message : "Could not add subject.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Add a subject"
      description="Create a new subject for this class."
      footer={
        <>
          <AdminButton variant="ghost" onClick={onClose}>Cancel</AdminButton>
          <AdminButton onClick={submit} disabled={busy}>
            {busy ? "Adding…" : "Add subject"}
          </AdminButton>
        </>
      }
    >
      <div className="grid gap-3.5">
        <ModalField label="Subject name" value={name} onChange={setName} placeholder="Mathematics" />
        <ModalField label="Code" value={code} onChange={setCode} placeholder="MATH" />
        {err && <p className="text-[13px] font-medium text-danger">{err}</p>}
      </div>
    </Modal>
  );
}

function EditSubjectModal({
  target,
  onClose,
  onDone,
}: {
  target: SchoolSubjectDto;
  onClose: () => void;
  onDone: () => Promise<void>;
}) {
  const [name, setName] = useState(target.name);
  const [code, setCode] = useState(target.code);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  async function submit() {
    setErr(null);
    if (!name.trim() || !code.trim()) {
      setErr("Subject name and code are required.");
      return;
    }
    setBusy(true);
    try {
      await adminApi.updateSchoolSubject(target.id, { name: name.trim(), code: code.trim() });
      await onDone();
      onClose();
    } catch (e) {
      setErr(e instanceof ApiError ? e.message : "Could not update subject.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal
      open={true}
      onClose={onClose}
      title="Edit subject"
      description={`Update ${target.name} (${target.code})`}
      footer={
        <>
          <AdminButton variant="ghost" onClick={onClose}>Cancel</AdminButton>
          <AdminButton onClick={submit} disabled={busy}>
            {busy ? "Saving…" : "Save changes"}
          </AdminButton>
        </>
      }
    >
      <div className="grid gap-3.5">
        <ModalField label="Subject name" value={name} onChange={setName} />
        <ModalField label="Code" value={code} onChange={setCode} />
        {err && <p className="text-[13px] font-medium text-danger">{err}</p>}
      </div>
    </Modal>
  );
}

// ─────────────────────────── Timetable ───────────────────────────

function TimetableTab() {
  const { data: ttData, isLoading } = useTimetable();
  const { data: teacherData } = useTeachers();
  const teachers = useMemo(() => teacherData?.teachers ?? [], [teacherData]);
  const [classFilter, setClassFilter] = useState("");
  const [addOpen, setAddOpen] = useState(false);
  const [bulkOpen, setBulkOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<{ id: string; weekday: number; start_time: string; end_time: string; room: string } | null>(null);

  const weekdays = useMemo(() => ttData?.weekdays ?? [], [ttData]);
  const classes = useMemo(() => ttData?.classes ?? [], [ttData]);

  const filteredWeekdays = useMemo(() => {
    if (!classFilter) return weekdays;
    return weekdays
      .map((wd) => ({ ...wd, periods: wd.periods.filter((p) => p.class_name === classFilter) }))
      .filter((wd) => wd.periods.length > 0);
  }, [weekdays, classFilter]);

  async function refresh() {
    await mutate(["timetable", ""]);
  }

  async function deletePeriod(id: string, label: string) {
    if (!confirm(`Remove ${label} from the timetable?`)) return;
    try {
      await adminApi.deletePeriod(id);
      await refresh();
    } catch (e) {
      alert(e instanceof ApiError ? e.message : "Could not delete period.");
    }
  }

  return (
    <FadeIn>
      <Card>
        <div className="border-b border-navy/8 p-4">
          <div className="flex items-center gap-3">
            <select
              value={classFilter}
              onChange={(e) => setClassFilter(e.target.value)}
              className="rounded-lg border border-navy/15 bg-white px-3 py-2 text-[14px] font-medium text-navy-deep outline-none focus:border-navy-deep"
            >
              <option value="">All classes</option>
              {classes.map((c) => (
                <option key={c} value={c}>{c}</option>
              ))}
            </select>
            <AdminButton onClick={() => setAddOpen(true)}>
              <IconPlus width={16} height={16} /> Add period
            </AdminButton>
            <AdminButton variant="ghost" onClick={() => setBulkOpen(true)}>
              <IconCalendar width={16} height={16} /> Bulk add
            </AdminButton>
          </div>
        </div>

        {isLoading && !ttData ? (
          <div className="h-40 animate-pulse rounded-2xl bg-navy/5" />
        ) : filteredWeekdays.length === 0 ? (
          <div className="p-10">
            <EmptyState
              icon={<IconCalendar width={26} height={26} />}
              title="No timetable entries"
              hint="Add periods to build your weekly schedule. The timetable is a recurring pattern keyed by weekday."
            />
          </div>
        ) : (
          <div className="overflow-x-auto p-4">
            <div
              className="grid gap-4"
              style={{ gridTemplateColumns: `repeat(${filteredWeekdays.length}, minmax(220px, 1fr))` }}
            >
              {filteredWeekdays.map((wd) => (
                <div key={wd.weekday}>
                  <h3 className="mb-2 text-[13px] font-bold uppercase tracking-wide text-ink-3">
                    {WEEKDAYS[wd.weekday] ?? `Day ${wd.weekday}`}
                  </h3>
                  <div className="space-y-2">
                    {wd.periods.map((p) => (
                      <div
                        key={p.id}
                        className="rounded-xl border border-navy/8 bg-white p-3 transition-shadow hover:shadow-sm"
                      >
                        <div className="flex items-start justify-between gap-2">
                          <div className="min-w-0 flex-1">
                            <p className="truncate text-[13.5px] font-semibold text-navy-deep">
                              {p.subject}
                            </p>
                            <p className="text-[12px] text-ink-3">
                              {p.class_name}-{p.section}
                            </p>
                            <p className="text-[12px] text-ink-3">
                              {p.start_time}–{p.end_time}
                            </p>
                            {p.teacher_name && (
                              <p className="mt-1 text-[11.5px] font-medium text-ink-2">
                                {p.teacher_name}
                              </p>
                            )}
                            {p.room && (
                              <p className="text-[11px] text-ink-3">Room: {p.room}</p>
                            )}
                          </div>
                          <div className="flex shrink-0 flex-col gap-1">
                            <button
                              type="button"
                              onClick={() => setEditTarget({
                                id: p.id,
                                weekday: wd.weekday,
                                start_time: p.start_time,
                                end_time: p.end_time,
                                room: p.room,
                              })}
                              className="rounded-lg p-1.5 text-ink-3 transition-colors hover:bg-navy/5 hover:text-navy-deep"
                              aria-label="Edit period"
                            >
                              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" /><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" /></svg>
                            </button>
                            <button
                              type="button"
                              onClick={() => deletePeriod(p.id, `${p.subject} (${p.class_name})`)}
                              className="rounded-lg p-1.5 text-ink-3 transition-colors hover:bg-danger/10 hover:text-danger"
                              aria-label="Delete period"
                            >
                              <IconTrash width={14} height={14} />
                            </button>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </Card>

      <AddPeriodModal
        open={addOpen}
        onClose={() => setAddOpen(false)}
        onDone={refresh}
        teachers={teachers}
        classes={classes}
      />
      <BulkCreateModal
        open={bulkOpen}
        onClose={() => setBulkOpen(false)}
        onDone={refresh}
        teachers={teachers}
        classes={classes}
      />
      {editTarget && (
        <EditPeriodModal
          target={editTarget}
          onClose={() => setEditTarget(null)}
          onDone={refresh}
        />
      )}
    </FadeIn>
  );
}

function AddPeriodModal({
  open,
  onClose,
  onDone,
  teachers,
  classes,
}: {
  open: boolean;
  onClose: () => void;
  onDone: () => Promise<void>;
  teachers: { id: string; name: string }[];
  classes: string[];
}) {
  const [teacherId, setTeacherId] = useState("");
  const [className, setClassName] = useState("");
  const [section, setSection] = useState("A");
  const [subject, setSubject] = useState("");
  const [weekday, setWeekday] = useState("1");
  const [startTime, setStartTime] = useState("09:00");
  const [endTime, setEndTime] = useState("09:45");
  const [room, setRoom] = useState("");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  function reset() {
    setTeacherId(""); setClassName(""); setSection("A"); setSubject("");
    setWeekday("1"); setStartTime("09:00"); setEndTime("09:45"); setRoom("");
    setErr(null);
  }

  async function submit() {
    setErr(null);
    if (!teacherId || !className.trim() || !subject.trim()) {
      setErr("Teacher, class, and subject are required.");
      return;
    }
    setBusy(true);
    try {
      await adminApi.createPeriod({
        teacher_id: teacherId,
        class_name: className.trim(),
        section: section.trim() || "A",
        subject: subject.trim(),
        weekday: parseInt(weekday, 10),
        start_time: startTime,
        end_time: endTime,
        room: room.trim(),
      } as CreatePeriodRequest);
      await onDone();
      reset();
      onClose();
    } catch (e) {
      setErr(e instanceof ApiError ? e.message : "Could not add period.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Add a period"
      description="Assign a teacher and subject to a weekly slot."
      footer={
        <>
          <AdminButton variant="ghost" onClick={onClose}>Cancel</AdminButton>
          <AdminButton onClick={submit} disabled={busy}>
            {busy ? "Adding…" : "Add period"}
          </AdminButton>
        </>
      }
    >
      <div className="grid gap-3.5">
        <label className="block">
          <span className="mb-1.5 block text-[11px] font-semibold uppercase tracking-wide text-ink-3">Teacher</span>
          <select
            value={teacherId}
            onChange={(e) => setTeacherId(e.target.value)}
            className="w-full rounded-xl border border-navy/12 bg-white/80 px-3.5 py-2.5 text-[14px] text-ink outline-none focus:border-accent"
          >
            <option value="">Select a teacher…</option>
            {teachers.map((t) => (
              <option key={t.id} value={t.id}>{t.name}</option>
            ))}
          </select>
        </label>
        <div className="grid grid-cols-2 gap-3.5">
          <label className="block">
            <span className="mb-1.5 block text-[11px] font-semibold uppercase tracking-wide text-ink-3">Class</span>
            <select
              value={className}
              onChange={(e) => setClassName(e.target.value)}
              className="w-full rounded-xl border border-navy/12 bg-white/80 px-3.5 py-2.5 text-[14px] text-ink outline-none focus:border-accent"
            >
              <option value="">Select…</option>
              {classes.map((c) => (
                <option key={c} value={c}>{c}</option>
              ))}
            </select>
          </label>
          <ModalField label="Section" value={section} onChange={setSection} placeholder="A" />
        </div>
        <ModalField label="Subject" value={subject} onChange={setSubject} placeholder="Mathematics" />
        <div className="grid grid-cols-2 gap-3.5">
          <label className="block">
            <span className="mb-1.5 block text-[11px] font-semibold uppercase tracking-wide text-ink-3">Weekday</span>
            <select
              value={weekday}
              onChange={(e) => setWeekday(e.target.value)}
              className="w-full rounded-xl border border-navy/12 bg-white/80 px-3.5 py-2.5 text-[14px] text-ink outline-none focus:border-accent"
            >
              {WEEKDAYS.slice(1).map((d, i) => (
                <option key={i + 1} value={i + 1}>{d}</option>
              ))}
            </select>
          </label>
          <ModalField label="Room (optional)" value={room} onChange={setRoom} placeholder="101" />
        </div>
        <div className="grid grid-cols-2 gap-3.5">
          <ModalField label="Start time" value={startTime} onChange={setStartTime} placeholder="09:00" />
          <ModalField label="End time" value={endTime} onChange={setEndTime} placeholder="09:45" />
        </div>
        {err && <p className="text-[13px] font-medium text-danger">{err}</p>}
      </div>
    </Modal>
  );
}

// ─────────────────────────── Edit Period ───────────────────────────

function EditPeriodModal({
  target,
  onClose,
  onDone,
}: {
  target: { id: string; weekday: number; start_time: string; end_time: string; room: string };
  onClose: () => void;
  onDone: () => Promise<void>;
}) {
  const [weekday, setWeekday] = useState(String(target.weekday));
  const [startTime, setStartTime] = useState(target.start_time);
  const [endTime, setEndTime] = useState(target.end_time);
  const [room, setRoom] = useState(target.room);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  async function submit() {
    setErr(null);
    if (!startTime.trim() || !endTime.trim()) {
      setErr("Start and end time are required.");
      return;
    }
    setBusy(true);
    try {
      await adminApi.updatePeriod(target.id, {
        weekday: parseInt(weekday, 10),
        start_time: startTime,
        end_time: endTime,
        room: room.trim(),
      } as UpdatePeriodRequest);
      await onDone();
      onClose();
    } catch (e) {
      setErr(e instanceof ApiError ? e.message : "Could not update period.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal
      open={true}
      onClose={onClose}
      title="Edit period"
      description="Update the time, weekday, or room for this period."
      footer={
        <>
          <AdminButton variant="ghost" onClick={onClose}>Cancel</AdminButton>
          <AdminButton onClick={submit} disabled={busy}>
            {busy ? "Saving…" : "Save changes"}
          </AdminButton>
        </>
      }
    >
      <div className="grid gap-3.5">
        <label className="block">
          <span className="mb-1.5 block text-[11px] font-semibold uppercase tracking-wide text-ink-3">Weekday</span>
          <select
            value={weekday}
            onChange={(e) => setWeekday(e.target.value)}
            className="w-full rounded-xl border border-navy/12 bg-white/80 px-3.5 py-2.5 text-[14px] text-ink outline-none focus:border-accent"
          >
            {WEEKDAYS.slice(1).map((d, i) => (
              <option key={i + 1} value={i + 1}>{d}</option>
            ))}
          </select>
        </label>
        <div className="grid grid-cols-2 gap-3.5">
          <ModalField label="Start time" value={startTime} onChange={setStartTime} placeholder="09:00" />
          <ModalField label="End time" value={endTime} onChange={setEndTime} placeholder="09:45" />
        </div>
        <ModalField label="Room" value={room} onChange={setRoom} placeholder="101" />
        {err && <p className="text-[13px] font-medium text-danger">{err}</p>}
      </div>
    </Modal>
  );
}

// ─────────────────────────── Bulk Create Periods ───────────────────────────

function BulkCreateModal({
  open,
  onClose,
  onDone,
  teachers,
  classes,
}: {
  open: boolean;
  onClose: () => void;
  onDone: () => Promise<void>;
  teachers: { id: string; name: string }[];
  classes: string[];
}) {
  const [weekday, setWeekday] = useState("1");
  const [rows, setRows] = useState<BulkPeriodItem[]>([
    { teacher_id: "", class_name: "", section: "A", subject: "", start_time: "09:00", end_time: "09:45", room: "" },
  ]);
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState<string | null>(null);
  const [err, setErr] = useState<string | null>(null);

  function reset() {
    setWeekday("1");
    setRows([{ teacher_id: "", class_name: "", section: "A", subject: "", start_time: "09:00", end_time: "09:45", room: "" }]);
    setResult(null);
    setErr(null);
  }

  function updateRow(idx: number, patch: Partial<BulkPeriodItem>) {
    setRows((prev) => prev.map((r, i) => (i === idx ? { ...r, ...patch } : r)));
  }

  function addRow() {
    setRows((prev) => [...prev, { teacher_id: "", class_name: "", section: "A", subject: "", start_time: "09:00", end_time: "09:45", room: "" }]);
  }

  function removeRow(idx: number) {
    setRows((prev) => prev.filter((_, i) => i !== idx));
  }

  async function submit() {
    setErr(null);
    setResult(null);
    if (rows.length === 0) {
      setErr("Add at least one period.");
      return;
    }
    setBusy(true);
    try {
      const res = await adminApi.bulkCreatePeriods({
        weekday: parseInt(weekday, 10),
        periods: rows,
      });
      setResult(`Created ${res.created_count} of ${res.created_count + res.error_count} periods${res.errors.length ? `. Errors: ${res.errors.join("; ")}` : ""}`);
      await onDone();
      if (res.error_count === 0) {
        reset();
        onClose();
      }
    } catch (e) {
      setErr(e instanceof ApiError ? e.message : "Bulk create failed.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Bulk add periods"
      description="Add multiple periods for a single weekday at once."
      footer={
        <>
          <AdminButton variant="ghost" onClick={onClose}>Cancel</AdminButton>
          <AdminButton onClick={submit} disabled={busy}>
            {busy ? "Creating…" : `Create ${rows.length} period${rows.length !== 1 ? "s" : ""}`}
          </AdminButton>
        </>
      }
    >
      <div className="grid gap-3.5">
        <label className="block">
          <span className="mb-1.5 block text-[11px] font-semibold uppercase tracking-wide text-ink-3">Weekday</span>
          <select
            value={weekday}
            onChange={(e) => setWeekday(e.target.value)}
            className="w-full rounded-xl border border-navy/12 bg-white/80 px-3.5 py-2.5 text-[14px] text-ink outline-none focus:border-accent"
          >
            {WEEKDAYS.slice(1).map((d, i) => (
              <option key={i + 1} value={i + 1}>{d}</option>
            ))}
          </select>
        </label>

        <div className="space-y-2">
          {rows.map((row, idx) => (
            <div key={idx} className="rounded-xl border border-navy/8 bg-navy/2 p-3">
              <div className="mb-2 flex items-center justify-between">
                <span className="text-[11px] font-semibold uppercase tracking-wide text-ink-3">Period {idx + 1}</span>
                {rows.length > 1 && (
                  <button
                    type="button"
                    onClick={() => removeRow(idx)}
                    className="rounded-lg p-1 text-ink-3 transition-colors hover:bg-danger/10 hover:text-danger"
                  >
                    <IconTrash width={14} height={14} />
                  </button>
                )}
              </div>
              <div className="grid grid-cols-2 gap-2">
                <label className="block">
                  <span className="mb-1 block text-[10px] font-semibold uppercase text-ink-3">Teacher</span>
                  <select
                    value={row.teacher_id}
                    onChange={(e) => updateRow(idx, { teacher_id: e.target.value })}
                    className="w-full rounded-lg border border-navy/12 bg-white px-2.5 py-2 text-[13px] outline-none focus:border-accent"
                  >
                    <option value="">Select…</option>
                    {teachers.map((t) => (
                      <option key={t.id} value={t.id}>{t.name}</option>
                    ))}
                  </select>
                </label>
                <label className="block">
                  <span className="mb-1 block text-[10px] font-semibold uppercase text-ink-3">Class</span>
                  <select
                    value={row.class_name}
                    onChange={(e) => updateRow(idx, { class_name: e.target.value })}
                    className="w-full rounded-lg border border-navy/12 bg-white px-2.5 py-2 text-[13px] outline-none focus:border-accent"
                  >
                    <option value="">Select…</option>
                    {classes.map((c) => (
                      <option key={c} value={c}>{c}</option>
                    ))}
                  </select>
                </label>
              </div>
              <div className="mt-2 grid grid-cols-3 gap-2">
                <input
                  type="text"
                  value={row.section}
                  onChange={(e) => updateRow(idx, { section: e.target.value })}
                  placeholder="Sec"
                  className="rounded-lg border border-navy/12 bg-white px-2.5 py-2 text-[13px] outline-none focus:border-accent"
                />
                <input
                  type="text"
                  value={row.subject}
                  onChange={(e) => updateRow(idx, { subject: e.target.value })}
                  placeholder="Subject"
                  className="rounded-lg border border-navy/12 bg-white px-2.5 py-2 text-[13px] outline-none focus:border-accent"
                />
                <input
                  type="text"
                  value={row.room}
                  onChange={(e) => updateRow(idx, { room: e.target.value })}
                  placeholder="Room"
                  className="rounded-lg border border-navy/12 bg-white px-2.5 py-2 text-[13px] outline-none focus:border-accent"
                />
              </div>
              <div className="mt-2 grid grid-cols-2 gap-2">
                <input
                  type="text"
                  value={row.start_time}
                  onChange={(e) => updateRow(idx, { start_time: e.target.value })}
                  placeholder="09:00"
                  className="rounded-lg border border-navy/12 bg-white px-2.5 py-2 text-[13px] outline-none focus:border-accent"
                />
                <input
                  type="text"
                  value={row.end_time}
                  onChange={(e) => updateRow(idx, { end_time: e.target.value })}
                  placeholder="09:45"
                  className="rounded-lg border border-navy/12 bg-white px-2.5 py-2 text-[13px] outline-none focus:border-accent"
                />
              </div>
            </div>
          ))}
        </div>

        <AdminButton variant="ghost" onClick={addRow}>
          <IconPlus width={14} height={14} /> Add another period
        </AdminButton>

        {result && <p className="text-[13px] font-medium text-accent-deep">{result}</p>}
        {err && <p className="text-[13px] font-medium text-danger">{err}</p>}
      </div>
    </Modal>
  );
}
