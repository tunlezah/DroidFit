#!/usr/bin/env bash
# Architecture compliance check — the executable form of framework/05_architecture.md §9.
#
# These are the rules that cannot be enforced by the compiler or by detekt, but that a
# reviewer would otherwise have to check by hand every time. Run before every commit,
# alongside `./gradlew qualityCheck`.
#
# Note the `--include=*.kt` plus comment-stripping: naive greps for these patterns hit the
# framework's own documentation about them (the manifest comment explaining why there is no
# INTERNET permission matches a grep for INTERNET). Every check below looks at code only.

set -uo pipefail
cd "$(dirname "$0")/.." || exit 1

FAILURES=0
CODE_DIRS=(app core data domain feature-workout feature-history feature-progress feature-settings)

fail() {
    printf '  FAIL  %s\n' "$1"
    FAILURES=$((FAILURES + 1))
}
pass() { printf '  ok    %s\n' "$1"; }

# Strips // line comments, /* */ block comments and XML comments, so documentation about a
# banned pattern does not register as a use of it.
strip_comments() {
    perl -0777 -pe 's{/\*.*?\*/|<!--.*?-->}{}gs; s{^\s*//.*$}{}gm; s{^\s*\*.*$}{}gm'
}

check_absent() {
    local label="$1" pattern="$2"
    shift 2
    local hits
    hits=$(grep -rlE "$pattern" "$@" 2>/dev/null | while read -r f; do
        strip_comments < "$f" | grep -qE "$pattern" && echo "$f"
    done)
    if [ -n "$hits" ]; then
        fail "$label"
        printf '        %s\n' $hits
    else
        pass "$label"
    fi
}

echo "Architecture compliance (framework/05_architecture.md §9)"

# 1. domain/ must stay Android-free. Structurally enforced by the module applying
#    kotlin-jvm, but check anyway so a plugin change does not silently open the door.
check_absent "domain/ has no android.* import" \
    '^import android(x)?\.' --include='*.kt' domain/src

# 2-3. Feature isolation.
check_absent "no feature-* depends on :data" \
    'projects\.data' --include='build.gradle.kts' feature-workout feature-history feature-progress feature-settings

check_absent "no feature-* depends on another feature" \
    'projects\.feature[A-Z]' --include='build.gradle.kts' feature-workout feature-history feature-progress feature-settings

# 4. The design system must not know about business types.
check_absent "core:designsystem does not depend on :domain" \
    'projects\.domain' --include='build.gradle.kts' core/designsystem

# 5. Dispatchers are injected, so every coroutine can be driven by a test scheduler.
DISPATCHER_HITS=$(grep -rlE 'Dispatchers\.(IO|Default|Main)' --include='*.kt' "${CODE_DIRS[@]}" 2>/dev/null \
    | grep -v '^core/common/' || true)
if [ -n "$DISPATCHER_HITS" ]; then
    fail "no Dispatchers.* reference outside core:common"
    printf '        %s\n' $DISPATCHER_HITS
else
    pass "no Dispatchers.* reference outside core:common"
fi

check_absent "no GlobalScope" \
    'GlobalScope' --include='*.kt' "${CODE_DIRS[@]}"

# 6. Stateless screens stay previewable and testable without a Hilt graph (ADR-0010).
SCREEN_HITS=$(grep -rln 'hiltViewModel()' --include='*.kt' "${CODE_DIRS[@]}" 2>/dev/null | while read -r f; do
    # A hiltViewModel() call inside a `fun XxxScreen(` body, rather than in XxxRoute.
    awk '/^internal fun [A-Za-z]+Screen\(/,/^}/' "$f" | grep -q 'hiltViewModel()' && echo "$f"
done)
if [ -n "$SCREEN_HITS" ]; then
    fail "no hiltViewModel() inside a *Screen composable"
    printf '        %s\n' $SCREEN_HITS
else
    pass "no hiltViewModel() inside a *Screen composable"
fi

# 7. The offline guarantee (D-0009). Checks the SOURCE manifest; the merged manifest must
#    also be checked before a release, because a library can add a permission by merging.
#    See framework/agents/security_privacy_agent.md §Verification.
check_absent "no INTERNET permission in the source manifest" \
    'uses-permission[^>]*android\.permission\.INTERNET' app/src/main/AndroidManifest.xml

# 8. User training history cannot be regenerated; there is no cloud copy by design.
check_absent "no fallbackToDestructiveMigration" \
    'fallbackToDestructiveMigration' --include='*.kt' "${CODE_DIRS[@]}"

# 9. Versions live only in the catalogue.
VERSION_HITS=$(grep -rlE '(implementation|api|ksp|testImplementation)\([^)]*:[0-9]+\.[0-9]+' \
    --include='build.gradle.kts' . 2>/dev/null || true)
if [ -n "$VERSION_HITS" ]; then
    fail "no hard-coded dependency version in a module build file"
    printf '        %s\n' $VERSION_HITS
else
    pass "no hard-coded dependency version in a module build file"
fi

# 10. Colours come from the theme, so dark and AMOLED modes work everywhere.
COLOUR_HITS=$(grep -rlE 'Color\(0x' --include='*.kt' "${CODE_DIRS[@]}" 2>/dev/null \
    | grep -v '^core/designsystem/' || true)
if [ -n "$COLOUR_HITS" ]; then
    fail "no literal Color(0x…) outside core:designsystem"
    printf '        %s\n' $COLOUR_HITS
else
    pass "no literal Color(0x…) outside core:designsystem"
fi

echo
if [ "$FAILURES" -eq 0 ]; then
    echo "All compliance checks passed."
else
    echo "$FAILURES compliance check(s) failed."
    echo "Each corresponds to a rule in framework/05_architecture.md or an ADR in"
    echo "project_memory/architecture_decisions.md. Fix the cause; if you believe the rule"
    echo "should change, record it as an ADR first."
fi
exit "$FAILURES"
