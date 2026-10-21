#!/usr/bin/env bash
#
# Fails if any tracked text file contains emoji or decorative pictographic
# characters. Enforces Principle V (Plain-Text Documentation and Code) of the
# project constitution: repository files MUST be plain text.
#
# Typographic punctuation used in the project's prose (for example the arrow
# character U+2192) is intentionally NOT flagged; only emoji and pictographs are.
#
# The file set is the set of files tracked by git, so the gate checks exactly
# what a reviewer sees. Two categories are deliberately skipped:
#   - Vendored spec-kit tooling we do not author and that is overwritten on
#     `specify` upgrades: `.github/skills/**` and `.specify/templates/**`.
#   - Binary files (images, the SQLDelight schema snapshot, the Gradle wrapper
#     jar), which are excluded by a NUL-byte test rather than by extension.
#
# Usage: scripts/check-no-emoji.sh
set -euo pipefail

# Emoji and pictographic Unicode ranges. Deliberately excludes:
#   - U+2190..U+21FF arrows (for example the arrow used in prose)
#   - U+2010..U+205E general punctuation
#   - U+2700..U+271F dingbats used as documentable text glyphs (for example the
#     multiplication X U+2715 quoted in ADR-0011) other than true emoji
#   - U+2315, U+2316, U+2317 legacy computing symbols quoted in project docs
# Only emoji and clearly pictographic characters are flagged.
PATTERN='[\x{1F000}-\x{1FAFF}\x{1F300}-\x{1F5FF}\x{1F600}-\x{1F64F}\x{1F680}-\x{1F6FF}\x{1F900}-\x{1F9FF}\x{2600}-\x{26FF}\x{FE0F}\x{2705}\x{274C}\x{2B00}-\x{2BFF}]'

# Write the NUL-separated list of text-like tracked files to stdout. Callers
# stream this directly (never via command substitution, which drops the NULs).
list_files() {
    git ls-files -z 2>/dev/null |
        grep -zvE '(^|/)(build|\.git|\.gradle|\.kotlin|DerivedData)/' |
        grep -zvE '^\.github/skills/' |
        grep -zvE '^\.specify/templates/' |
        while IFS= read -r -d '' file; do
            [ -f "$file" ] || continue
            # Skip binaries: a NUL byte in the first block marks a binary file.
            grep -Iq . "$file" 2>/dev/null || continue
            printf '%s\0' "$file"
        done
}

# Prefer ripgrep (fast, PCRE2). Fall back to a portable perl scan so the gate
# works on any runner (Linux or macOS) without an extra install. Both paths scan
# the exact same file set and are otherwise equivalent.
scan_with_rg() {
    list_files |
        xargs -0 rg --pcre2 --no-heading --line-number --color never --with-filename \
            "$PATTERN" || true
}

scan_with_perl() {
    list_files |
        while IFS= read -r -d '' file; do
            # -CSD enables UTF-8 I/O so astral codepoints (U+1F...) match; without
            # it perl treats input as bytes and the ranges never match.
            perl -CSD -ne "print \"\$ARGV:\$.:\$_\" if /$PATTERN/;" "$file" 2>/dev/null || true
        done
}

if command -v rg >/dev/null 2>&1; then
    matches=$(scan_with_rg)
elif command -v perl >/dev/null 2>&1; then
    matches=$(scan_with_perl)
else
    echo "check-no-emoji: neither ripgrep (rg) nor perl is available." >&2
    exit 2
fi

if [ -n "$matches" ]; then
    echo "check-no-emoji: emoji or pictographic characters found (constitution Principle V):" >&2
    echo "" >&2
    echo "$matches" >&2
    echo "" >&2
    echo "Remove these glyphs. Use Markdown emphasis or plain words instead." >&2
    exit 1
fi

echo "check-no-emoji: no emoji found. OK."
