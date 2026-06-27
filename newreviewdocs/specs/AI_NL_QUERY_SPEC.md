# AI Natural Language Query — Technical Specification

> **Document status:** Implementation-ready blueprint
> **Last updated:** 2026-06-27
> **Prerequisites:** `AI_INFRASTRUCTURE_SPEC.md`

---

## 1. Feature Overview

Admin can ask questions in natural language ("How many students in Grade 5 have attendance below 75%?") and get answers with data from the school's database. LLM converts the question to a structured query, executes it, and formats the response.

### Goals

- Admin asks natural language questions about school data
- LLM converts question to structured query parameters
- Backend executes safe, parameterized query against existing tables
- Response formatted as natural language answer + optional data table
- No raw SQL generation (safety: structured query layer, not direct SQL)

---

## 2. Current System Assessment

- No NL query exists
- All data access via specific API endpoints
- Rich data available: students, attendance, marks, fees, homework, announcements

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR-1 | Admin types question in search bar or dedicated NL query screen |
| FR-2 | LLM parses question into structured intent (entity, filter, aggregation, sort) |
| FR-3 | Backend executes structured query (NOT raw SQL — predefined query templates) |
| FR-4 | Response: natural language answer + optional table/chart data |
| FR-5 | Query history saved per user |
| FR-6 | Suggested questions on empty state |
| FR-7 | Rate limited (10 queries per minute) |

---

## 4. Database Design

### 4.1 New Table: `ai_nl_queries`

```sql
CREATE TABLE ai_nl_queries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID NOT NULL,
    user_id         UUID NOT NULL,
    question        TEXT NOT NULL,
    parsed_intent   TEXT NOT NULL,                 -- JSON: {entity, filters, aggregation, sort}
    answer_text     TEXT NOT NULL,                 -- natural language answer
    result_data     TEXT,                          -- JSON: table data (if applicable)
    model_used      VARCHAR(64),
    tokens_used     INTEGER,
    latency_ms      INTEGER,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_ai_nl_queries_user ON ai_nl_queries(user_id, created_at DESC);
```

---

## 5. Backend Architecture

### 5.1 NlQueryService

```kotlin
class NlQueryService(private val aiService: AiService) {
    suspend fun query(schoolId: UUID, userId: UUID, question: String): NlQueryResult {
        // 1. Call LLM to parse question into structured intent
        val intent = aiService.complete(schoolId, userId, "nl_query", "parse_intent_v1",
            mapOf("question" to question, "available_entities" to ENTITY_DESCRIPTIONS))
        // 2. Validate intent (entity exists, filters are valid)
        // 3. Execute query via QueryExecutor
        val data = queryExecutor.execute(schoolId, intent)
        // 4. Call LLM to format natural language answer from data
        val answer = aiService.complete(schoolId, userId, "nl_query", "format_answer_v1",
            mapOf("question" to question, "data" to Json.encodeToString(data)))
        // 5. Store and return
    }
}
```

### 5.2 QueryExecutor

Predefined query templates (NOT raw SQL):

```kotlin
class QueryExecutor {
    suspend fun execute(schoolId: UUID, intent: ParsedIntent): QueryResult {
        return when (intent.entity) {
            "students" -> queryStudents(schoolId, intent.filters, intent.aggregation)
            "attendance" -> queryAttendance(schoolId, intent.filters, intent.aggregation)
            "marks" -> queryMarks(schoolId, intent.filters, intent.aggregation)
            "fees" -> queryFees(schoolId, intent.filters, intent.aggregation)
            "homework" -> queryHomework(schoolId, intent.filters, intent.aggregation)
            else -> throw NlQueryException("Unknown entity: ${intent.entity}")
        }
    }
}
```

### 5.3 Intent Schema

```json
{
  "entity": "attendance",
  "filters": [
    {"field": "class", "op": "eq", "value": "Grade 5"},
    {"field": "attendance_rate", "op": "lt", "value": 75}
  ],
  "aggregation": "count",
  "group_by": null,
  "sort": {"field": "attendance_rate", "order": "asc"}
}
```

---

## 6. API Contracts

```
POST /api/v1/school/ai/query
{
  "question": "How many students in Grade 5 have attendance below 75%?"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "answer": "There are 8 students in Grade 5 with attendance below 75%. The lowest is Rahul Kumar at 62%.",
    "table": {
      "columns": ["Student", "Attendance %"],
      "rows": [
        ["Rahul Kumar", "62%"],
        ["Priya Sharma", "68%"],
        ...
      ]
    }
  }
}
```

```
GET /api/v1/school/ai/query/history
```

---

## 7. Security

- No raw SQL generation — LLM produces structured intent, backend executes predefined templates
- Query executor validates all filter values (prevents injection)
- School-scoped: all queries filtered by `school_id` from JWT
- Rate limited: 10 queries/minute per user
- Query history is user-scoped

---

## 8. Acceptance Criteria

- [ ] Admin can ask natural language questions
- [ ] Questions about students, attendance, marks, fees answered correctly
- [ ] Response includes natural language answer + optional data table
- [ ] No raw SQL executed (structured query layer only)
- [ ] Query history saved
- [ ] Suggested questions shown on empty state
- [ ] Rate limiting enforced

---

## 9. Implementation Roadmap

| Phase | Duration | Tasks |
|---|---|---|
| 1 | 1 day | DB migration, Exposed table |
| 2 | 2 days | Intent parser (LLM prompt + response parsing) |
| 3 | 3 days | QueryExecutor (predefined templates for 5 entities) |
| 4 | 1 day | Answer formatter (LLM prompt) |
| 5 | 1 day | API endpoint + history |
| 6 | 2 days | Client UI (search bar, results, history, suggestions) |
| 7 | 1 day | Tests |

---

## 10. File-Level Impact Analysis

| File | Change Type | Description |
|---|---|---|
| `server/.../db/Tables.kt` | Add | `AiNlQueriesTable` |
| `server/.../feature/ai/nlquery/NlQueryService.kt` | New | Core service |
| `server/.../feature/ai/nlquery/QueryExecutor.kt` | New | Predefined query templates |
| `server/.../feature/ai/nlquery/NlQueryRouting.kt` | New | API endpoints |
| `docs/db/migration_042_ai_nl_query.sql` | New | DDL |
| `composeApp/.../ui/v2/screens/admin/NlQueryScreen.kt` | New | Query UI |
