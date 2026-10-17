#!/bin/sh
# Fail open: a Gradle error must not block the agent turn.
cat >/dev/null || true
./gradlew spotlessApply || exit 0
