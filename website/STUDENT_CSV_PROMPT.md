# Student CSV — onboarding upload

This document gives you (a) a copy-paste **prompt** for any LLM (ChatGPT, Claude,
Gemini…) that generates a ready-to-upload student CSV, and (b) the exact column
contract the Enroll+ onboarding importer expects.

Upload happens in **Onboarding → Import students** (the step after *Academic
structure*) or later in **Admin → People → Students → Import CSV**. Both funnel
into the same server endpoint, `POST /api/v1/school/students/import`, whose CSV
parser is the single source of truth.

---

## Column contract (what the importer reads)

Header row (case-insensitive, order-independent, extra columns ignored):

```
full_name,class_name,roll_number,section,student_code
```

| Column         | Required | Notes                                                                                  |
| -------------- | :------: | -------------------------------------------------------------------------------------- |
| `full_name`    |   ✅ yes  | Student's full name. Aliases accepted: `name`, `student_name`.                         |
| `class_name`   |   ✅ yes  | The class/grade exactly as you set it in the Academic step (e.g. `Grade 1`). Aliases: `class`, `grade`. |
| `roll_number`  |   ✅ yes  | Roll/registration number within the class. Aliases: `roll`, `roll_no`, `rollno`.       |
| `section`      |   ⬜ no   | Section letter (e.g. `A`, `B`). Defaults to `A` when blank. Alias: `sec`.              |
| `student_code` |   ⬜ no   | Your own unique admission/student code. **Auto-generated** (unique) when blank. Must be unique across the school. Aliases: `code`, `admission_no`, `admission_number`. |

**Rules the server enforces**

- A row missing `full_name`, `class_name`, **or** `roll_number` is skipped and
  reported as a per-row error — the rest of the file still imports (partial
  import, never all-or-nothing).
- `student_code` must be unique. A duplicate code is skipped and reported; leave
  it blank to let the server mint a guaranteed-unique code.
- Quoted fields and embedded commas are handled (RFC-4180 style), so
  `"Sharma, Aarav"` is one field.
- Blank lines are ignored.

> Tip: the wizard's **Download template** button gives you a correctly-headed
> starter file, and a **live preview** shows exactly how many rows will import
> (and which rows need attention) before you commit.

---

## The prompt (generates ≥ 20 students)

Copy everything in the box below into your LLM of choice. Replace the bracketed
bits with your school's real classes if you want them to match.

```
Generate a CSV of at least 20 students for an Indian K-12 school, ready to
upload into a school-management system. Output ONLY raw CSV text (no markdown
fences, no commentary, no extra blank lines).

Use EXACTLY this header as the first line:
full_name,class_name,roll_number,section,student_code

Rules for the data:
- Produce at least 20 student rows (aim for 24).
- full_name: realistic, diverse Indian student names (mix of genders and
  regions). No titles, no quotes unless a name truly contains a comma.
- class_name: use these classes only: "Grade 1", "Grade 2", "Grade 3".
  Spread students roughly evenly across them.
- roll_number: integers starting at 1, restarting per (class_name, section)
  combination, with no gaps.
- section: use "A" or "B". Keep section sizes balanced.
- student_code: a unique code per student in the format STU-0001, STU-0002,
  … incrementing with no duplicates and no gaps.
- Every row must have a non-empty full_name, class_name and roll_number.
- Do not include a trailing comma or extra columns.

Return the CSV now.
```

If your school uses different class names, change the `class_name` list in the
prompt to match the classes you created in the **Academic structure** step —
the importer keys students to classes by this exact text.

---

## Worked sample (24 students)

A valid file produced by the prompt above is checked in next to this doc as
[`sample-students.csv`](./sample-students.csv). It imports cleanly against a
school that has `Grade 1`, `Grade 2`, and `Grade 3`. First few lines:

```
full_name,class_name,roll_number,section,student_code
Aarav Sharma,Grade 1,1,A,STU-0001
Diya Patel,Grade 1,2,A,STU-0002
Vivaan Gupta,Grade 1,3,A,STU-0003
…
```
