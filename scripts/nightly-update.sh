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
ALLOWED_TOOLS="Bash Read Edit WebFetch WebSearch"

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
You are the dependency update agent for the Android Kotlin project at $REPO_DIR.

Goal: bump the safe entries in gradle/libs.versions.toml to their latest
stable versions. Only edit that one file.

Steps:
1. Read gradle/libs.versions.toml and list every entry under [versions].
2. For each entry, look up the latest STABLE version from the appropriate
   source (Maven Central for Kotlin/JetBrains, Google Maven for androidx,
   GitHub Releases for plugins). Use WebFetch / WebSearch to query. Use a
   research workflow if multiple entries share a constraint.
3. Cross-check candidates against known constraints for THIS project:
   - AGP is pinned to 9.2.x; do NOT bump it.
   - Compose versions are pinned together (compose + material3 +
     material3-adaptive + compose-bom); verify they move as a set or
     none move.
   - Detekt 2.0.0-alpha.3 and ktlint 14.2.0 are intentional; do NOT bump.
   - Bumping a major Kotlin version requires a corresponding bump to
     kotlin-compose and ksp; treat them as a set.
4. For each safe bump, edit gradle/libs.versions.toml to set the new
   version. Keep edits minimal — one version string per Edit.
5. Exit 0 if you changed anything, exit 1 if nothing was safe to bump
   (the rest of the script will still run a build to verify the
   no-op case is still clean).

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

Steps:
1. Read the most recent log under $LOG_DIR (sort by name, newest is the
   last one). Identify the root cause from ktlint / detekt / lintRelease
   / compile / test output.
2. Use a research workflow if the error is ambiguous — query upstream
   release notes / GitHub issues / stackoverflow for the offending
   library+version.
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
