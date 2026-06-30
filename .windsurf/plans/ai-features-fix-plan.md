# AI Features Fix Plan

## Summary
Fix three AI features (PEWS, AI Tutor, AI Report Card) and create a cost sheet.

## Tasks

### 1. PEWS Default Language: "hi" → "en"
- **ParentDraftService.kt**: `DraftResult.language` default, `generateDraft` signature, `systemPrompt` text, `sendParentMessage` fallback, `deterministicDraft` branch order
- **ActModule.kt**: route default `lang` parameter
- **CaseFile.kt**: `ParentDraft.language` default
- **CaseworkerService.kt**: system prompt text
- **TeacherPewsViewModel.kt**: `generateParentDraft` default
- **PewsStudentDetailViewModel.kt**: `generateParentDraft` default
- **TeacherPewsScreenV2.kt**: fallback "HI" → "EN"
- **PewsStudentDetailScreenV2.kt**: fallback "HI" → "EN"

### 2. PEWS Language Selector in UI
- Add language dropdown (EN/HI/MR/TA/TE/BN) to parent draft generation in:
  - TeacherPewsScreenV2.kt (mobile teacher)
  - PewsStudentDetailScreenV2.kt (mobile admin)
  - PewsStudentPanel.tsx (admin web)

### 3. PEWS Admin Agentic Interventions (PewsStudentPanel.tsx)
- Replace hardcoded 3 buttons (Improved/No change/Dismiss) with:
  - AI-suggested actions from Case File plan steps (with confirm dialog)
  - Rule-based fallback actions from action type mapping
  - Execute action on confirmation
  - Mark outcome after execution

### 4. AI Tutor Subject Loading Fix (SenseRouting.kt)
- Fix class code matching: try exact, then normalized (strip "Class ", "Std ", etc.)
- Also try matching by class name if code fails

### 5. AI Tutor Subject-Free Chat + Crash Prevention
- TutorChatViewModel: allow askDoubt with null/empty subjectId
- TutorChatScreen: don't crash when no subject selected, show "General" option

### 6. AI Report Card: Fix EditDraftRequest Mismatch
- Shared model: `editedDraft` → `draftJson` (to match server)

### 7. AI Report Card: Language Defaults "hi" → "en"
- ReportCardModels.kt: DraftDto.language, ParentReport.language
- ReportCardDraftRepository.kt: insert default language

### 8. AI Report Card: Verify DTO Sync
- Check all field names/types between shared and server models

### 9. Cost Sheet
- Create AI_FEATURES_COST_SHEET.md and AI_FEATURES_COST_SHEET.csv
