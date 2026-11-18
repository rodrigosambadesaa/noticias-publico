# Java4Shell (CVE-2026-114938)

This Android project uses an OpenJDK HotSpot JDK in CI/build/release jobs;
the installed Android application runs on ART, not that JVM.

GitHub Actions now sets `JAVA_TOOL_OPTIONS=-XX:-UseBASE64Intrinsics` to
disable the affected hardware-accelerated Base64 decoder in Java tooling.
This workaround is not a substitute for updating Java.

For locally running Gradle or self-hosted runners, use a **vendor-confirmed
fixed JDK**. Until patched, apply that flag before launching Java and
restart existing daemons. Confirm `UseBASE64Intrinsics = false` with
`java -XX:+PrintFlagsFinal -version`. On Windows PowerShell use
`$env:JAVA_TOOL_OPTIONS = '-XX:-UseBASE64Intrinsics'`.

See https://www.cert.europa.eu/publications/security-advisories/2026-021/
and https://adoptium.net/news/2026/11/temurin-november-2026-cspu-java4shell/.
