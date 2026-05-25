# R8 optimization rules for the calculator app.
#
# `proguard-android-optimize.txt` (referenced from app/build.gradle.kts) provides
# the default Android keep rules. Compose / Room / kotlinx-coroutines ship their
# own consumer rules transitively, so the only thing this file adds is extra
# shrinking aggression — package collapsing and access modification — which gain
# us a few % of dex size without changing visible behavior.

-repackageclasses ''
-allowaccessmodification

# Keep the standard sources for crash stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
