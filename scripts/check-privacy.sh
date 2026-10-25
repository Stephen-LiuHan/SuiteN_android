#!/usr/bin/env bash
# Fails if OCR text from books could reach logs or the local database.
# OCR text may only exist in memory during AI processing (see SuiteN_doc/spec/04_ai_spec.md).
set -euo pipefail
cd "$(dirname "$0")/.."

SRC=app/src/main
status=0

# 1. Log calls that interpolate OCR text itself (logging only its length is allowed)
if grep -rnP 'Log\.[a-z]\(.*\$\{?(ocrText|extractedText|recognizedText)(?![?!]?\.length)' "$SRC"; then
  echo "ERROR: OCR text is written to a log. Log its length only." >&2
  status=1
fi

# 2. println / print of OCR text
if grep -rnP '\bprintln?\(.*(ocrText|extractedText|recognizedText)' "$SRC"; then
  echo "ERROR: OCR text is printed." >&2
  status=1
fi

# 3. OCR text fields in Room entities or sync DTOs
if grep -rniP '\b(val|var)\s+(ocr\w*|extractedText|recognizedText)\b' "$SRC/kotlin/space/suiten/data"; then
  echo "ERROR: an entity or DTO has an OCR text field. OCR text must never be stored or synced." >&2
  status=1
fi

if [ "$status" -eq 0 ]; then
  echo "Privacy check passed: no OCR text in logs, entities, or sync DTOs."
fi
exit "$status"
