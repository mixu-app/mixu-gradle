# mixu-gradle quality gates. Run via `just check`.
set shell := ["bash", "-euo", "pipefail", "-c"]

check: lint-md build

lint-md:
    markdownlint-cli2 --config .markdownlint-cli2.jsonc

# Compiling the sample is the only real proof the conventions still work.
build:
    @if [ -x ./gradlew ]; then ./gradlew --no-daemon --console=plain build; \
    else echo "MISSING TOOL: ./gradlew (run `gradle wrapper --gradle-version 9.8.2` once); gate cannot run."; exit 1; fi
