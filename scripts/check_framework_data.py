#!/usr/bin/env python3
"""Validate the framework's own reference data for internal consistency.

The framework asserts numbers in several places at once — the engine spec's prose, the
template table, and workout_templates.json — and those can silently disagree. They did:
four of the six HIIT template durations were wrong on first authoring, and nothing would
have caught it until the phase-06 implementer trusted the table and produced sessions of
the wrong length.

This is the test for the specification. Run it whenever you edit anything in
framework/data/, and as part of phase 06.

Usage:  python3 scripts/check_framework_data.py
Exit:   0 if consistent, 1 otherwise.
"""

from __future__ import annotations

import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
DATA = ROOT / "framework" / "data"

failures: list[str] = []
checks = 0


def check(label: str, condition: bool, detail: str = "") -> None:
    global checks
    checks += 1
    if condition:
        print(f"  ok    {label}")
    else:
        print(f"  FAIL  {label}")
        if detail:
            print(f"        {detail}")
        failures.append(label)


def load(name: str) -> dict:
    with (DATA / name).open() as handle:
        return json.load(handle)


print("Framework reference data consistency")

# --- workout_templates.json -------------------------------------------------------
templates = load("workout_templates.json")

for entry in templates["hiit_templates"]:
    rounds, work, recovery = entry["rounds"], entry["work_seconds"], entry["recovery_seconds"]
    # No recovery after the final work interval — the cool-down serves that purpose.
    computed = rounds * work + (rounds - 1) * recovery
    check(
        f"HIIT template {entry['id']}: main_seconds_needed",
        computed == entry["main_seconds_needed"],
        f"stated {entry['main_seconds_needed']}, computed {computed} "
        f"({rounds}x{work} + {rounds - 1}x{recovery})",
    )

# The smallest template must fit inside the HIIT style's minimum main block, or a
# duration the UI offers cannot actually be generated.
smallest = min(t["main_seconds_needed"] for t in templates["hiit_templates"])
hiit = templates["budget_split"]["styles"]["hiit"]
check(
    "smallest HIIT template fits the style's main_min_seconds",
    smallest <= hiit["main_min_seconds"],
    f"smallest template needs {smallest}s, main_min_seconds is {hiit['main_min_seconds']}s",
)

# Templates must be listed largest-first, because the engine takes the first that fits.
needed = [t["main_seconds_needed"] for t in templates["hiit_templates"]]
check(
    "HIIT templates are ordered largest-first",
    needed == sorted(needed, reverse=True),
    f"order is {needed}",
)

# total_min must equal the sum of its parts, for every style.
for name, style in templates["budget_split"]["styles"].items():
    expected = style["warm_up_min_seconds"] + style["main_min_seconds"] + style["cool_down_min_seconds"]
    check(
        f"style {name}: total_min_seconds equals warm-up + main + cool-down minimums",
        expected == style["total_min_seconds"],
        f"stated {style['total_min_seconds']}, computed {expected}",
    )
    check(
        f"style {name}: warm_up_min <= warm_up_max",
        style["warm_up_min_seconds"] <= style["warm_up_max_seconds"],
    )

bounds = templates["budget_split"]["duration_bounds_seconds"]
check(
    "every style's minimum total fits inside the accepted duration range",
    all(s["total_min_seconds"] <= bounds["max"] for s in templates["budget_split"]["styles"].values()),
)

# --- met_values.json -------------------------------------------------------------
mets = load("met_values.json")

check(
    "every MET entry has a Compendium code",
    all(a.get("code") for a in mets["activities"]),
    ", ".join(a["label"] for a in mets["activities"] if not a.get("code")),
)
check(
    "every MET value is positive and plausible",
    all(0 < a["met"] <= 25 for a in mets["activities"]),
)
approximated = [a for a in mets["activities"] if a.get("approximated_from")]
check(
    "every approximated MET value carries a note explaining it",
    all(a.get("$note") for a in approximated),
    "an approximation without a note is indistinguishable from a measured value",
)

# --- exercise catalogue vs references and MET table -------------------------------
catalogue_path = ROOT / "app" / "src" / "main" / "assets" / "exercises_seed.json"
with catalogue_path.open() as handle:
    catalogue = json.load(handle)

reference_text = (DATA / "references.md").read_text()
known_keys = set(re.findall(r"^### `([a-z0-9]+)`", reference_text, re.MULTILINE))
check("references.md defines at least one citation key", bool(known_keys))

used_keys: set[str] = set()
for exercise in catalogue["exercises"]:
    used_keys.update(exercise.get("evidence_keys", []))
unknown = sorted(used_keys - known_keys)
check(
    "every evidence_key in the catalogue exists in references.md",
    not unknown,
    f"unknown keys: {unknown}",
)

met_values = {a["met"] for a in mets["activities"]}
off_table = sorted({e["met_value"] for e in catalogue["exercises"]} - met_values)
check(
    "every catalogue MET value appears in met_values.json",
    not off_table,
    f"values not in the table: {off_table}",
)

ids = [e["id"] for e in catalogue["exercises"]]
check("catalogue exercise ids are unique", len(ids) == len(set(ids)))

# Spoken cues must fit inside the shortest interval the exercise can appear in (REQ-044).
# Vigorous exercises appear in 30 s HIIT intervals, so they get the tighter limit.
over_limit = []
for exercise in catalogue["exercises"]:
    limit = 14 if exercise["met_value"] >= 8.0 else 20
    words = len(exercise["spoken_instruction"].split())
    if words > limit:
        over_limit.append(f"{exercise['id']} ({words} words, limit {limit})")
check(
    "every spoken_instruction fits its word limit",
    not over_limit,
    "; ".join(over_limit),
)

# Every vigorous exercise needs a stop-if-symptoms safety note (REQ-006).
SYMPTOM_PATTERN = re.compile(r"chest pain|dizz|breathless|symptom", re.IGNORECASE)
missing_symptom_note = [
    exercise["id"]
    for exercise in catalogue["exercises"]
    if exercise["met_value"] >= 8.0
    and not SYMPTOM_PATTERN.search(" ".join(exercise["safety_notes"]))
]
check(
    "every exercise at MET >= 8.0 carries a stop-if-symptoms note",
    not missing_symptom_note,
    ", ".join(missing_symptom_note),
)

# --- the engine's intensity-anchor table mirrors met_values.json ------------------
# The anchor is not derivable from the MET value: a spin class is 9.0 MET anchored at
# Zone 2, the elliptical at the same 9.0 MET is anchored vigorous. So the engine carries
# a (modality, MET) -> anchor lookup, and that lookup is only trustworthy if it agrees
# with the Compendium data it was copied from. It decides which exercises may be warmed
# up on, so drift silently changes generated sessions rather than breaking a build.
anchor_source = (
    ROOT / "domain" / "src" / "main" / "kotlin" / "com" / "visceralfit" / "domain" / "engine"
    / "IntensityAnchor.kt"
).read_text()
kotlin_anchors = {
    (modality.lower(), float(met)): anchor.lower()
    for modality, met, anchor in re.findall(
        r"Anchored\(Modality\.([A-Z_0-9]+), ([0-9.]+), ([A-Z_0-9]+)\)", anchor_source
    )
}
check("the engine declares an intensity-anchor table", bool(kotlin_anchors))

table_anchors = {
    (a["modality"], a["met"]): a["intensity"] for a in mets["activities"] if a["modality"]
}
disagreements = sorted(
    f"{modality} {met}: engine says {anchor}, met_values.json says {table_anchors.get((modality, met))}"
    for (modality, met), anchor in kotlin_anchors.items()
    if table_anchors.get((modality, met)) != anchor
)
check(
    "every engine anchor matches met_values.json",
    not disagreements,
    "; ".join(disagreements),
)
absent = sorted(f"{m} {v}" for (m, v) in table_anchors if (m, v) not in kotlin_anchors)
check(
    "the engine's table covers every modality-specific MET entry",
    not absent,
    f"missing from IntensityAnchor.kt: {absent}",
)

# Every catalogue exercise must resolve to a tabulated anchor. Without this the engine
# falls back to a MET threshold, which is exactly the approximation the lookup exists to
# avoid, and it would do so silently.
untabulated = sorted(
    f"{e['id']} ({e['modality']} {e['met_value']})"
    for e in catalogue["exercises"]
    if (e["modality"], e["met_value"]) not in kotlin_anchors
)
check(
    "every catalogue exercise has a tabulated intensity anchor",
    not untabulated,
    "; ".join(untabulated),
)

# --- the engine's test fixture mirrors the shipped catalogue -----------------------
# :domain is a pure Kotlin module and cannot read app/src/main/assets, so the engine's
# tests use a fixture. A drifted fixture makes the golden-file test a test of a catalogue
# nobody ships, and it would keep passing while doing it.
fixture_source = (
    ROOT / "domain" / "src" / "test" / "kotlin" / "com" / "visceralfit" / "domain" / "engine"
    / "CatalogueFixture.kt"
).read_text()
fixture_rows = re.findall(
    r"^\s{8}([a-z0-9_]+)\|([a-z_]+)\|([a-z]+)\|([0-9.]+)\|([a-z_,]*)$",
    fixture_source,
    re.MULTILINE,
)
fixture = {
    row[0]: (row[1], row[2], float(row[3]), tuple(t for t in row[4].split(",") if t))
    for row in fixture_rows
}
shipped = {
    e["id"]: (
        e["modality"],
        e["difficulty"],
        e["met_value"],
        tuple(sorted(e.get("caution_tags", []))),
    )
    for e in catalogue["exercises"]
}
check(
    "the engine fixture has one row per shipped exercise",
    set(fixture) == set(shipped),
    f"only in fixture: {sorted(set(fixture) - set(shipped))}; "
    f"only in catalogue: {sorted(set(shipped) - set(fixture))}",
)
mismatched = sorted(
    f"{exercise_id}: fixture {fixture[exercise_id]} vs catalogue {shipped[exercise_id]}"
    for exercise_id in set(fixture) & set(shipped)
    if fixture[exercise_id] != shipped[exercise_id]
)
check(
    "every engine fixture row matches the shipped exercise",
    not mismatched,
    "; ".join(mismatched),
)

# --- catalogue balance requirements (08_exercise_library_spec.md §1, §4) -----------
# Stated as minimums in prose, which is how the catalogue drifted below them before.
MODALITY_MINIMUMS = {"floor_pilates": 24, "reformer_pilates": 10, "elliptical": 8, "spin_bike": 12}
by_modality: dict[str, list[dict]] = {}
for exercise in catalogue["exercises"]:
    by_modality.setdefault(exercise["modality"], []).append(exercise)

for modality, minimum in MODALITY_MINIMUMS.items():
    found = len(by_modality.get(modality, []))
    check(f"{modality}: at least {minimum} exercises", found >= minimum, f"found {found}")

for modality in MODALITY_MINIMUMS:
    entries = by_modality.get(modality, [])
    for level in ("beginner", "intermediate", "advanced"):
        found = sum(1 for e in entries if e["difficulty"] == level)
        check(f"{modality}: at least 3 {level} exercises", found >= 3, f"found {found}")
    untagged = sum(1 for e in entries if not e.get("caution_tags"))
    check(
        f"{modality}: at least 4 exercises with no caution tags",
        untagged >= 4,
        f"found {untagged} — a user with several exclusions needs a workable session",
    )

pool_minimums = [
    ("mobility pool (MET <= 2.5)", 6, lambda e: e["met_value"] <= 2.5),
    (
        "warm-up pool (Pilates at MET <= 4.0)",
        6,
        lambda e: e["met_value"] <= 4.0 and e["modality"].endswith("pilates"),
    ),
    (
        "vigorous pool (machine at MET >= 8.0)",
        4,
        lambda e: e["met_value"] >= 8.0 and e["modality"] in ("elliptical", "spin_bike"),
    ),
]
for label, minimum, predicate in pool_minimums:
    found = sum(1 for e in catalogue["exercises"] if predicate(e))
    check(f"{label}: at least {minimum} exercises", found >= minimum, f"found {found}")

# --- documented module count matches settings.gradle.kts --------------------------
# Counts stated in prose drift. This one was wrong (11 stated, 13 actual) until checked.
settings_text = (ROOT / "settings.gradle.kts").read_text()
module_count = len(re.findall(r'^include\("', settings_text, re.MULTILINE))
readme_text = (ROOT / "README.md").read_text()
check(
    "README's module count matches settings.gradle.kts",
    f"({module_count} modules)" in readme_text,
    f"settings.gradle.kts declares {module_count} modules; README says otherwise",
)

# One build file per module, plus the root. TD-0003 tracks this duplication.
build_files = len(list(ROOT.glob("*/build.gradle.kts"))) + len(list(ROOT.glob("*/*/build.gradle.kts"))) + 1
check(
    "one build file per module plus the root",
    build_files == module_count + 1,
    f"{build_files} build files for {module_count} modules",
)

# --- summary ----------------------------------------------------------------------
print()
if failures:
    print(f"{len(failures)} of {checks} checks failed.")
    print("These are inconsistencies in the framework's own specification. Fix the data,")
    print("then fix any prose that repeats the same numbers — framework/07_workout_engine_spec.md")
    print("restates the template table and must agree with it.")
    sys.exit(1)

print(f"All {checks} checks passed.")
