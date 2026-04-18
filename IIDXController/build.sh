#!/bin/bash
set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$PROJECT_DIR/build"

ANDROID_JAR="/usr/lib/android-sdk/platforms/android-23/android.jar"
KOTLINC="/opt/kotlin/kotlinc/bin/kotlinc"
KOTLIN_HOME="/opt/kotlin/kotlinc"
AAPT="/usr/bin/aapt"
DX="/usr/lib/android-sdk/build-tools/debian/dx"
APKSIGNER="/usr/bin/apksigner"
ZIPALIGN="/usr/lib/android-sdk/build-tools/debian/zipalign"
KEYSTORE="$BUILD_DIR/debug.keystore"
APK_FINAL="$PROJECT_DIR/IIDXController.apk"

echo "=== IIDX Controller APK Builder ==="
echo ""

# --- Clean ---
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/gen" "$BUILD_DIR/obj" "$BUILD_DIR/apk_content"

# --- Step 1: Generate R.java ---
echo "[1/6] Generating R.java from resources..."
$AAPT package -f -m \
    -J "$BUILD_DIR/gen" \
    -M "$PROJECT_DIR/AndroidManifest.xml" \
    -S "$PROJECT_DIR/res" \
    -I "$ANDROID_JAR"

echo "      R.java generated: $(find "$BUILD_DIR/gen" -name "R.java")"

# --- Step 2: Compile Kotlin + R.java ---
echo "[2/6] Compiling Kotlin sources..."
$KOTLINC \
    $(find "$PROJECT_DIR/src" -name "*.kt") \
    $(find "$BUILD_DIR/gen" -name "*.java") \
    -classpath "$ANDROID_JAR" \
    -jvm-target 1.8 \
    -d "$BUILD_DIR/obj" \
    2>&1

echo "      Compiled classes: $(find "$BUILD_DIR/obj" -name "*.class" | wc -l)"

# --- Step 3: Dex ---
echo "[3/6] Converting bytecode to DEX..."
KOTLIN_STDLIB="$KOTLIN_HOME/lib/kotlin-stdlib.jar"

# Strip Java 9+ multi-release entries from kotlin-stdlib to avoid parse errors
mkdir -p "$BUILD_DIR/kotlin_stripped"
cd "$BUILD_DIR/kotlin_stripped"
jar xf "$KOTLIN_STDLIB"
rm -rf META-INF/versions
jar cf "$BUILD_DIR/kotlin-stdlib-stripped.jar" .
cd "$PROJECT_DIR"

$DX --dex \
    --no-strict \
    --min-sdk-version=26 \
    --output="$BUILD_DIR/apk_content/classes.dex" \
    "$BUILD_DIR/obj" \
    "$BUILD_DIR/kotlin-stdlib-stripped.jar"

ls -lh "$BUILD_DIR/apk_content/classes.dex"

# --- Step 4: Package resources + classes.dex ---
echo "[4/6] Packaging APK (resources + dex)..."
$AAPT package -f \
    -M "$PROJECT_DIR/AndroidManifest.xml" \
    -S "$PROJECT_DIR/res" \
    -I "$ANDROID_JAR" \
    -F "$BUILD_DIR/app-raw.apk"

# Add classes.dex using zip (preserves proper ZIP format)
cp "$BUILD_DIR/app-raw.apk" "$BUILD_DIR/app-with-dex.apk"
cd "$BUILD_DIR/apk_content"
zip -j "$BUILD_DIR/app-with-dex.apk" classes.dex
cd "$PROJECT_DIR"

# --- Step 5: Zipalign (must come BEFORE signing) ---
echo "[5/6] Aligning APK..."
$ZIPALIGN -f 4 "$BUILD_DIR/app-with-dex.apk" "$BUILD_DIR/app-aligned.apk"

# --- Step 6: Sign ---
echo "[6/6] Signing APK..."
if [ ! -f "$KEYSTORE" ]; then
    keytool -genkeypair -v \
        -keystore "$KEYSTORE" \
        -storepass android \
        -alias androiddebugkey \
        -keypass android \
        -keyalg RSA \
        -keysize 2048 \
        -dname "CN=Android Debug,O=Android,C=US" \
        -validity 9999 2>/dev/null
fi

$APKSIGNER sign \
    --ks "$KEYSTORE" \
    --ks-pass pass:android \
    --ks-key-alias androiddebugkey \
    --key-pass pass:android \
    --out "$APK_FINAL" \
    "$BUILD_DIR/app-aligned.apk"

echo ""
echo "=== Build Complete ==="
ls -lh "$APK_FINAL"
