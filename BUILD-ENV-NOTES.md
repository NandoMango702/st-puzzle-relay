# Build environment notes (sandbox-specific, 2026-10-21)

These notes exist because this sandbox's networking breaks the default
Gradle/SDK tooling. Nothing here modifies the app source.

## Installed locations
- JDK 17 (Temurin 17.0.20.1): `/home/hatch/jdk17`
- Android SDK: `/home/hatch/android-sdk`
  - platform-tools (r37.0.1), platforms/android-36, build-tools/36.0.0,
    build-tools/35.0.0 (AGP 8.13 defaults to 35.0.0), cmake/3.22.1,
    ndk/29.0.14206865 (exact pinned version), cmdline-tools/latest
- Gradle 8.13 (manual install, wrapper dist download avoided):
  `/home/hatch/gradle-8.13/gradle-8.13`
- Gradle user home: `/home/hatch/.gradle` (MUST export GRADLE_USER_HOME;
  Java reports user.home=/root because we run as root, so the default
  would be /root/.gradle and miss our proxy config)
- Proxy config: `/home/hatch/.gradle/gradle.properties`
- `local.properties` in this repo: `sdk.dir=/home/hatch/android-sdk`

## Sandbox networking quirks and workarounds
1. Egress goes through an authenticating proxy at
   `hatch-egress-proxy:3128` (IPv6-only, user `hatch-runtime`).
   sdkmanager's HttpURLConnection stack crashes on it
   (NoSuchElementException in doTunneling), so SDK packages were
   installed manually: curl the zips from
   `https://dl.google.com/android/repository/...` (URLs taken from
   `repository2-1.xml`) and unzip into the SDK tree. Licenses were then
   accepted with `yes | sdkmanager --licenses` once the proxy worked
   (see below).
2. The proxy MITMs TLS with "Hatch Sandbox Egress CA". Its cert was
   imported into the JDK truststore:
   `keytool -importcert -alias hatch-sandbox-egress-ca -file <ca.pem> \
     -keystore ~/jdk17/lib/security/cacerts -storepass changeit`
   (Re-do this if the JDK is ever reinstalled.)
3. JVM TCP from an IPv6 source to IPv4-mapped loopback (::ffff:127.0.0.1)
   completes the handshake but the data never arrives, which silently
   kills the Gradle daemon handshake. Fix: run every JVM with
   `-Djava.net.preferIPv4Stack=true` (via `_JAVA_OPTIONS`), forcing
   pure IPv4 loopback for daemon communication.
4. Because of (3), the JVM cannot reach the IPv6-only egress proxy
   directly. A socat bridge translates:
   `socat TCP4-LISTEN:3129,bind=127.0.0.1,fork,reuseaddr \
     'TCP6:[fd8b:4f84:7d32:99::1]:3128'`
   Gradle's proxy settings point at `127.0.0.1:3129`. **The bridge must
   be running before any Gradle invocation.** Run it as a background
   process and verify with a CONNECT probe.

## Known build invocation
```bash
export JAVA_HOME=~/jdk17 GRADLE_USER_HOME=/home/hatch/.gradle \
  ANDROID_HOME=~/android-sdk ANDROID_SDK_ROOT=~/android-sdk \
  _JAVA_OPTIONS="-Djava.net.preferIPv4Stack=true" \
  PATH=~/jdk17/bin:~/gradle-8.13/gradle-8.13/bin:$PATH
cd ~/workspace/puzzle-app
gradle assembleDebug --no-configuration-cache --console=plain
```
Notes:
- `--no-configuration-cache`: the project's configuration cache is
  enabled, but `app/build.gradle.kts` runs `git merge-base simon/main main`
  for the version suffix; with no `simon` remote the command fails and the
  configuration-cache store chokes on the failed ValueSource. Disabling
  the cache lets the try/catch fall back to the "UNOFFICIAL" suffix.
  (Alternative: `git remote add simon https://git.tartarus.org/simon/puzzles.git`
  and fetch, for proper versioning.)
- First successful build took ~6 min (dependency downloads + NDK compile
  of all puzzles for all ABIs).
- Baseline APK (unmodified upstream):
  `app/build/outputs/apk/debug/app-debug.apk`
