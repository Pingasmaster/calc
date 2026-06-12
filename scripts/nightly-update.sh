#!/usr/bin/env bash
#
# Nightly dependency update + build + commit + push for /home/user/temp/calc.
#
# Invoked by the calc-nightly-update.service systemd unit at 02:00.
#
# Phases:
#   1. Snapshot the version catalog.
#   2. Spawn a Claude agent (`mini`) to research latest dep versions and
#      bump the safe ones in gradle/libs.versions.toml.
#   3. Run ./build.sh (ktlintCheck + detekt + lintRelease + assemble* +
#      testDebugUnitTest, with abortOnError = true). On failure, loop
#      up to MAX_ITER times: spawn a Claude agent to revert the
#      MINIMUM set of dep bumps to restore a green build.
#   4. If the build is green and the working tree has changes, commit
#      and push to origin/master.
#
# On unrecoverable failure the working tree is restored to the
# snapshotted catalog; nothing is committed; nothing is pushed.

set -euo pipefail

# -----------------------------------------------------------------------------
# Paths & logging
# -----------------------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
LOG_DIR="${XDG_STATE_HOME:-$HOME/.local/state}/calc-nightly-update"
SNAP_DIR="${XDG_RUNTIME_DIR:-/tmp}/calc-nightly-update"
mkdir -p "$LOG_DIR" "$SNAP_DIR"
RUN_ID="$(date -u +%Y%m%dT%H%M%SZ)"
LOG="$LOG_DIR/nightly-update.${RUN_ID}.log"
exec >>"$LOG" 2>&1
echo
echo "=================================================================="
echo "Nightly run starting: $RUN_ID"
echo "REPO: $REPO_DIR"
echo "=================================================================="

# -----------------------------------------------------------------------------
# Resolve `mini` (the user's bashrc alias for `claude`)
#
# `mini` in ~/.bashrc expands (after the claude alias) to:
#   claude --allow-dangerously-skip-permissions --permission-mode plan \
#          --settings ~/.claude/minimax-settings.json
#
# Two problems for a non-interactive shell:
#   (a) .bashrc's standard `[[ $- != *i* ]] && return` guard fires before
#       any alias line, so the alias is invisible here.
#   (b) `--permission-mode plan` is read-only — the agent would only
#       propose changes, never make them. Useless for an autonomous run.
#       We substitute `bypassPermissions` so the agent can actually
#       edit files at 02:00 without a human at the keyboard.
#
# We solve both by defining `mini` as a shell function that mirrors the
# alias but uses bypassPermissions. Keep this in sync if you change the
# alias.
# -----------------------------------------------------------------------------
CLAUDE_BIN="/home/user/.local/bin/claude"
CLAUDE_SETTINGS="$HOME/.claude/minimax-settings.json"
if [ ! -x "$CLAUDE_BIN" ]; then
  echo "FATAL: claude binary not found at $CLAUDE_BIN" >&2
  exit 1
fi
if [ ! -f "$CLAUDE_SETTINGS" ]; then
  echo "WARN: settings file $CLAUDE_SETTINGS not found; continuing without it." >&2
  CLAUDE_SETTINGS=""
fi

mini() {
  local -a args=(
    --allow-dangerously-skip-permissions
    --permission-mode bypassPermissions
  )
  if [ -n "$CLAUDE_SETTINGS" ]; then
    args+=( --settings "$CLAUDE_SETTINGS" )
  fi
  args+=( "$@" )
  "$CLAUDE_BIN" "${args[@]}"
}
export -f mini

# Belt-and-suspenders: scope the agent's tool surface so even with
# --allow-dangerously-skip-permissions it can only touch what we expect.
# Workflow + Agent are included so the agent can launch the workflow-
# driven dep audit the user asked for (fan-out research, fan-in apply).
ALLOWED_TOOLS="Bash Read Edit Write Workflow Agent WebFetch WebSearch"

# -----------------------------------------------------------------------------
# Acquire a separate nightly lock so two nightly runs don't trample
# each other. We deliberately do NOT take build.sh's .build.lock —
# build.sh manages that itself and a single nightly run needs to call
# build.sh many times (once per iteration of the recovery loop).
# -----------------------------------------------------------------------------
NIGHTLY_LOCK="$SNAP_DIR/nightly.lock"
exec 9>"$NIGHTLY_LOCK"
if ! flock -n 9; then
  echo "Another nightly run is already in progress. Exiting cleanly."
  exit 0
fi
cleanup() {
  rm -f "$SNAP_DIR/libs.versions.toml.snapshot"
  if [ -n "${DID_STASH:-}" ]; then
    echo "Restoring auto-stash."
    git -C "$REPO_DIR" stash pop >/dev/null 2>&1 || echo "WARN: stash pop failed; resolve manually."
  fi
}
trap cleanup EXIT

# -----------------------------------------------------------------------------
# Working-tree guard. The nightly script's job is dep updates, not
# cleaning up after a human. If the tree is dirty, refuse to run
# (default) or stash + restore (opt-in via DIRTY_TREE_POLICY=stash).
# -----------------------------------------------------------------------------
DIRTY_TREE_POLICY="${DIRTY_TREE_POLICY:-error}"
cd "$REPO_DIR"
if ! git diff --quiet HEAD 2>/dev/null || [ -n "$(git ls-files --others --exclude-standard)" ]; then
  case "$DIRTY_TREE_POLICY" in
    error)
      echo "Working tree is dirty (uncommitted changes or untracked files)."
      git status --short
      echo "Commit or stash your in-progress work before the nightly runs."
      exit 0
      ;;
    stash)
      echo "Working tree is dirty; stashing before run."
      git stash push -u -m "nightly-update.sh auto-stash @ $(date -u +%FT%TZ)" >/dev/null
      DID_STASH=1
      ;;
    ignore)
      echo "WARN: working tree is dirty and DIRTY_TREE_POLICY=ignore."
      ;;
    *)
      echo "Unknown DIRTY_TREE_POLICY=$DIRTY_TREE_POLICY (accepted: error, stash, ignore)"
      exit 2
      ;;
  esac
fi

# -----------------------------------------------------------------------------
# Sync with origin. Fast-forward only — never destroy local work.
# -----------------------------------------------------------------------------
git fetch origin
if ! git pull --ff-only; then
  echo "Non-FF pull (local commits or diverged). Leaving for manual review."
  exit 1
fi

# -----------------------------------------------------------------------------
# Snapshot the version catalog so Phase 3 can revert individual [versions]
# keys rather than the whole file.
# -----------------------------------------------------------------------------
cp gradle/libs.versions.toml "$SNAP_DIR/libs.versions.toml.snapshot"
echo "Snapshot: $SNAP_DIR/libs.versions.toml.snapshot"

# -----------------------------------------------------------------------------
# Phase 1: dep research & update
# -----------------------------------------------------------------------------
echo
echo "=== Phase 1: dep research & update ==="
set +e
mini -p "
You are the dependency update agent for the Android Kotlin project at
$REPO_DIR.

Goal: bump the safe entries in gradle/libs.versions.toml to their latest
stable versions. Only edit that one file.

MANDATORY: You MUST launch a Workflow via the Workflow tool. The
user has explicitly asked for a workflow-driven audit. Do not do the
research serially with WebFetch/WebSearch by yourself — that defeats
the whole point. The Workflow tool takes a JS script inline; the
script uses agent() and parallel() to fan out work.

IMPORTANT: do NOT pass a `schema` option to agent(). The
StructuredOutput validator has a known issue with top-level array
schemas (it wraps responses in an object, failing the schema). Have
each subagent return PLAIN TEXT — one finding per line, formatted
like "lib=<name> current=<v> latest=<v> breaking=<notes or none>".
The Apply phase parses that text.

Template (adapt to this project):

  export const meta = {
    name: 'dep-update-audit',
    description: 'Fan-out dep research, fan-in apply',
    phases: [{title: 'Research'}, {title: 'Apply'}],
  };

  phase('Research');
  const findings = await parallel([
    () => agent(
      'Read gradle/libs.versions.toml. For each [versions] entry that
       comes from Maven Central (Kotlin, JetBrains, kotlinx,
       coroutines, etc.), look up the latest STABLE version on Maven
       Central via WebFetch (try
       https://repo1.maven.org/maven2/<path-to-maven-metadata.xml>).
       Return a plain-text list, ONE FINDING PER LINE in the exact
       format:
         lib=<name> current=<v> latest=<v> breaking=<notes or none>
       Lines starting with "#" are comments. Do not return JSON.',
      {phase: 'Research'}
    ),
    () => agent(
      'Read gradle/libs.versions.toml. For each [versions] entry that
       comes from Google Maven (androidx, material3, room, datastore,
       activity-compose, lifecycle, etc.), look up the latest STABLE
       version on Google Maven via WebFetch (try
       https://dl.google.com/android/maven2/<path>/maven-metadata.xml).
       Return a plain-text list, ONE FINDING PER LINE in the exact
       format:
         lib=<name> current=<v> latest=<v> breaking=<notes or none>
       Lines starting with "#" are comments. Do not return JSON.',
      {phase: 'Research'}
    ),
    () => agent(
      'Read gradle/libs.versions.toml. For each [versions] entry that
       comes from GitHub Releases (ktlint, detekt, compose-rules,
       slack-lint-checks, slack-compose-lints, dependency-analysis,
       etc.), look up the latest release on GitHub via WebFetch (the
       GitHub Releases API endpoint for the upstream project).
       Return a plain-text list, ONE FINDING PER LINE in the exact
       format:
         lib=<name> current=<v> latest=<v> breaking=<notes or none>
       Lines starting with "#" are comments. Do not return JSON.',
      {phase: 'Research'}
    ),
  ]);

  phase('Apply');
  // Parse the text findings (each agent returns a string). For each
  // line "lib=X current=A latest=B breaking=...", decide whether the
  // bump is safe per the project constraints below. For each safe
  // bump, Edit gradle/libs.versions.toml (one Edit per version
  // string). Return { bumped: [...], skipped: [{lib, reason}] }.

Project constraints (apply during the Apply phase):
- AGP is pinned to 9.2.x; do NOT bump it.
- Compose versions are pinned together (compose + material3 +
  material3-adaptive + compose-bom); verify they move as a set or
  none move.
- Detekt 2.0.0-alpha.3 and ktlint 14.2.0 are intentional; do NOT bump.
- Bumping a major Kotlin version requires a corresponding bump to
  kotlin-compose and ksp; treat them as a set.

After the workflow returns, exit 0 if anything was bumped, exit 1 if
nothing was safe to bump (the rest of the script will still run a build
to verify the no-op case is still clean).

Do NOT touch any other file. Do NOT commit or push. Do NOT add new
dependencies, only bump existing ones.
" \
  --add-dir "$REPO_DIR" \
  --allowedTools "$ALLOWED_TOOLS"
PHASE1_RC=$?
set -e
echo "Phase 1 exit code: $PHASE1_RC"

# -----------------------------------------------------------------------------
# Phase 2 + 3: build, with up to MAX_ITER rounds of failure recovery.
# -----------------------------------------------------------------------------
echo
echo "=== Phase 2: build + recovery ==="
MAX_ITER=5
for i in $(seq 1 $MAX_ITER); do
  echo
  echo "--- build attempt $i/$MAX_ITER ---"
  if ./build.sh; then
    echo "Build PASS on attempt $i."
    break
  fi
  if [ "$i" -eq "$MAX_ITER" ]; then
    echo "Build FAILED after $MAX_ITER attempts. Reverting all dep bumps this run."
    cp "$SNAP_DIR/libs.versions.toml.snapshot" gradle/libs.versions.toml
    if ./build.sh; then
      echo "Restored snapshot builds clean. Aborting nightly run with no commit."
    else
      echo "Even snapshot fails to build — pre-existing broken state, not touching."
    fi
    exit 1
  fi

  echo
  echo "=== Phase 3: failure recovery (iter $i) ==="
  set +e
  mini -p "
You are the build-failure recovery agent for the Android Kotlin project at
$REPO_DIR. The latest './build.sh' run failed.

Goal: get back to a green build with the MINIMUM set of changes reverted.
That almost always means reverting a single [versions] key in
gradle/libs.versions.toml — not editing source code.

MANDATORY: You MUST launch a Workflow via the Workflow tool to
diagnose the failure. The user has explicitly asked for workflow-
driven diagnosis. Do not read the log and decide on your own — that
defeats the whole point. The Workflow tool takes a JS script inline.

IMPORTANT: do NOT pass a `schema` option to agent() (the same
StructuredOutput array-vs-object quirk as Phase 1). Have each
subagent return PLAIN TEXT.

Template (adapt to this project):

  export const meta = {
    name: 'build-failure-diagnosis',
    description: 'Fan-out log analysis, fan-in apply',
    phases: [{title: 'Diagnose'}, {title: 'Apply'}],
  };

  phase('Diagnose');
  // Three parallel reads of the most recent log under $LOG_DIR,
  // each focused on one phase of the build:
  //   1) ktlintCheck / detekt
  //   2) lintRelease
  //   3) assembleDebug+assembleRelease / testDebugUnitTest
  // For each, return a plain-text one-line summary like:
  //   failing_phase=<name> root_cause=<one line> offending_dep=<lib or none> evidence=<file:line or log excerpt>
  const diagnosis = await parallel([
    () => agent('Read the most recent log under $LOG_DIR (sort by
                  name, newest is the last). Focus on the
                  ktlintCheck and detekt output. If the error mentions
                  a library+version, that is the offending_dep. Return
                  a plain-text single line in the exact format:
                    failing_phase=<phase> root_cause=<one line>
                    offending_dep=<lib key in libs.versions.toml or none>
                    evidence=<short quote or file:line>',
                 {phase: 'Diagnose'}),
    () => agent('Read the most recent log under $LOG_DIR. Focus on
                  the lintRelease output. If the error mentions a
                  library+version, that is the offending_dep. Return
                  a plain-text single line in the same format as
                  agent #1.',
                 {phase: 'Diagnose'}),
    () => agent('Read the most recent log under $LOG_DIR. Focus on
                  assembleDebug+assembleRelease and testDebugUnitTest
                  output. If the error mentions a library+version,
                  that is the offending_dep. Return a plain-text
                  single line in the same format as agent #1.',
                 {phase: 'Diagnose'}),
  ]);

  phase('Apply');
  // Pick the most specific diagnosis. If offending_dep is non-null
  // and points to a single [versions] key, restore ONLY that key
  // from $SNAP_DIR/libs.versions.toml.snapshot. If multiple keys
  // are causally linked, restore them as a set. If the cause is
  // unambiguously a project-source bug introduced by a dep bump,
  // fix the minimum source to pass lint/compile and document the
  // fix.

Steps (semantic — adapt as needed):
3. If the cause is a single dep bump, restore ONLY that one [versions]
   key in gradle/libs.versions.toml to its value from
   $SNAP_DIR/libs.versions.toml.snapshot. To do this, read both files
   with Read, find the offending key, and Edit the working file to set
   it back to the snapshotted value. Do not touch other keys.
4. If multiple bumps are implicated, revert only the ones that are
   causally linked (e.g. compose + material3 + material3-adaptive move
   together; revert them as a set).
5. Only if the cause is unambiguously a project-source bug introduced
   by a dep bump, fix the source — and only the minimum to pass
   lint/compile. Document what you fixed.
6. Exit 0 once you have made any change (revert or fix). Exit 1 only if
   you cannot identify the cause at all.

Do NOT commit or push. Do NOT bump any other dep. Do NOT add new code.
" \
    --add-dir "$REPO_DIR" \
    --allowedTools "$ALLOWED_TOOLS"
  PHASE3_RC=$?
  set -e
  echo "Phase 3 exit code: $PHASE3_RC"
  if [ "$PHASE3_RC" -ne 0 ]; then
    echo "Recovery agent could not identify cause. Reverting all dep bumps this run."
    cp "$SNAP_DIR/libs.versions.toml.snapshot" gradle/libs.versions.toml
    exit 1
  fi
done

# -----------------------------------------------------------------------------
# Phase 4: commit & push
# -----------------------------------------------------------------------------
echo
echo "=== Phase 4: commit & push ==="
if git diff --quiet; then
  echo "No changes to commit."
  echo "Nightly run complete (no-op)."
  exit 0
fi

git add -A
SUMMARY="$(git diff --cached --stat | tail -5)"
# Keep the existing git identity (the user already has user.name/email
# configured in this repo per the harness). The -c flags are belt-and-
# suspenders in case git is run from a context without those set.
GIT_AUTHOR_NAME="$(git config user.name)"
GIT_AUTHOR_EMAIL="$(git config user.email)"
git -c user.name="$GIT_AUTHOR_NAME" \
    -c user.email="$GIT_AUTHOR_EMAIL" \
    commit -m "chore(deps): nightly update (${RUN_ID})

${SUMMARY}
"
echo "Commit created."

# Push only if upstream exists and the push will succeed.
if ! git rev-parse --abbrev-ref --symbolic-full-name '@{u}' >/dev/null 2>&1; then
  echo "No upstream configured; skipping push. Commit left locally."
  exit 0
fi

# Use the user's ed25519 key directly so this works without ssh-agent.
# ssh-agent is not always running at 02:00, especially on a remote
# session.
GIT_SSH_COMMAND="ssh -i $HOME/.ssh/id_ed25519 -o IdentitiesOnly=yes -o StrictHostKeyChecking=accept-new" \
  git push
echo "Push complete."
echo "Nightly run complete."
