#!/bin/sh
# Run Open Liberty dev mode on JDK 25 so build and server bytecode always match.
#
# Why this exists: Homebrew's `mvn` defaults to JDK 26, but the Open Liberty server
# runs on JDK 25. Liberty dev mode's incremental hot-recompile ignores
# `maven.compiler.release` and emits JDK 26 bytecode (class file v70) that the JDK 25
# server cannot load -> UnsupportedClassVersionError -> HTTP 500. Forcing JAVA_HOME to
# JDK 25 here makes the compiler produce v69 bytecode the server loads fine.
#
# Usage:  export OPENAI_API_KEY=sk-...   (once per terminal)
#         ./dev.sh
export JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home"
# Put JDK 25 first on PATH so ANY compiler liberty:dev forks (incremental hot-recompile) is JDK 25,
# not Homebrew's openjdk 26 — otherwise incremental recompiles emit v70 bytecode the JDK 25 server
# cannot load (UnsupportedClassVersionError). This is what the JAVA_HOME line alone does not fix.
export PATH="$JAVA_HOME/bin:$PATH"

# Load local secrets (API keys) if present — gitignored, never committed.
if [ -f ./.env.local ]; then
  . ./.env.local
fi

if [ -z "$OPENAI_API_KEY" ]; then
  echo "WARNING: OPENAI_API_KEY is not set in this shell — the LLM calls will fail."
  echo "  Run:  export OPENAI_API_KEY=sk-...   then re-run ./dev.sh"
fi

echo "Using JAVA_HOME=$JAVA_HOME"
exec mvn clean liberty:dev
