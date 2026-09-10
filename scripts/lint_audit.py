#!/usr/bin/env python3
"""Parse AGP lint XML reports and surface API-level issues introduced by the
minSdk 35 -> 33 backport.

Collects issues whose id indicates "referenced API is newer than minSdk"
(NewApi, InlinedApi) plus a few related ones, then writes a markdown summary
to GITHUB_STEP_SUMMARY (if present) and a plain-text file for artifact upload.
"""
import glob
import os
import sys
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict

INTERESTING = {
    "NewApi",
    "InlinedApi",
    "OverrideConcrete",
    "UnsafeOptInUsageError",
    "AndroidLintNewApi",
}

reports = sorted(glob.glob("**/lint-results*.xml", recursive=True))

if not reports:
    msg = "No lint XML reports found - lint likely failed before writing output."
    print(msg)
    if os.path.exists("lint.log"):
        tail = open("lint.log", encoding="utf-8", errors="ignore").read()[-3000:]
        print("--- lint.log tail ---")
        print(tail)
    sys.exit(0)

found = defaultdict(list)
counts = Counter()

for path in reports:
    try:
        root = ET.parse(path).getroot()
    except ET.ParseError as e:
        print("Failed to parse %s: %s" % (path, e))
        continue
    for issue in root.iter("issue"):
        iid = issue.get("id", "")
        if iid not in INTERESTING:
            continue
        counts[iid] += 1
        loc = issue.find("location")
        msg = (issue.get("message") or "").strip()
        if loc is not None:
            f = loc.get("file", "")
            line = loc.get("line", "")
            if f.startswith(os.getcwd()):
                f = os.path.relpath(f, os.getcwd())
            found[iid].append((f, line, msg))
        else:
            found[iid].append(("", "", msg))

lines = []
lines.append("### Android 13 backport - lint API audit")
lines.append("")
lines.append("Reports parsed: %d" % len(reports))
lines.append("")

if not counts:
    lines.append("No NewApi / InlinedApi issues found. minSdk 33 looks clean.")
else:
    lines.append("| Issue | Count |")
    lines.append("|:---|---:|")
    for iid, n in counts.most_common():
        lines.append("| %s | %d |" % (iid, n))
    lines.append("")

    for iid in counts:
        entries = found[iid]
        lines.append("#### %s (%d)" % (iid, len(entries)))
        lines.append("")
        # Deduplicate by (file, line, message)
        seen = set()
        shown = 0
        for f, line, msg in entries:
            key = (f, line, msg[:80])
            if key in seen:
                continue
            seen.add(key)
            short = msg.split("\n")[0][:160]
            if f:
                lines.append("- `%s:%s` %s" % (f, line, short))
            else:
                lines.append("- %s" % short)
            shown += 1
            if shown >= 120:
                lines.append("- ... (truncated, see artifact for full list)")
                break
        lines.append("")

text = "\n".join(lines)
print(text)

out = os.environ.get("GITHUB_STEP_SUMMARY")
if out:
    with open(out, "a", encoding="utf-8") as fh:
        fh.write(text + "\n")

with open("lint-api-audit.txt", "w", encoding="utf-8") as fh:
    fh.write(text + "\n")
