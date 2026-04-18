#!/bin/bash
set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$PROJECT_DIR/build"

ANDROID_JAR="/usr/lib/android-sdk/platforms/android-23/android.jar"
KOTLINC="/opt/kotlin/kotlinc/bin/kotlinc"
KOTLIN_HOME="/opt/kotlin/kotlinc"
AAPT2="/usr/bin/aapt2"
DX="/usr/lib/android-sdk/build-tools/debian/dx"
APKSIGNER="/usr/bin/apksigner"
ZIPALIGN="/usr/lib/android-sdk/build-tools/debian/zipalign"
KEYSTORE="$BUILD_DIR/debug.keystore"
APK_FINAL="$PROJECT_DIR/IIDXController.apk"

echo "=== 専コン接続アプリ APK Builder ==="
echo ""

# --- Clean ---
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/gen" "$BUILD_DIR/obj" "$BUILD_DIR/compiled_res"

# --- Step 1: Compile resources with aapt2 ---
echo "[1/6] Compiling resources with aapt2..."
$AAPT2 compile --dir "$PROJECT_DIR/res" -o "$BUILD_DIR/compiled_res.zip"

echo "[1/6] Linking resources and generating R.java..."
$AAPT2 link \
    -o "$BUILD_DIR/app-resources.apk" \
    -I "$ANDROID_JAR" \
    --manifest "$PROJECT_DIR/AndroidManifest.xml" \
    "$BUILD_DIR/compiled_res.zip" \
    --java "$BUILD_DIR/gen"

echo "      R.java: $(find "$BUILD_DIR/gen" -name "R.java")"

# --- Step 2: Compile sources ---
# 2a. Compile R.java with javac first (kotlinc does NOT output .class for Java files)
echo "[2/6] Compiling R.java with javac..."
javac \
    -classpath "$ANDROID_JAR" \
    -source 1.8 -target 1.8 \
    -d "$BUILD_DIR/obj" \
    $(find "$BUILD_DIR/gen" -name "*.java")
echo "      R classes: $(find "$BUILD_DIR/obj" -name "R*.class" | wc -l)"

# 2b. Compile Kotlin sources, with R class already on the classpath
echo "[2/6] Compiling Kotlin sources..."
$KOTLINC \
    $(find "$PROJECT_DIR/src" -name "*.kt") \
    -classpath "$ANDROID_JAR:$BUILD_DIR/obj" \
    -jvm-target 1.8 \
    -d "$BUILD_DIR/obj" 2>&1

echo "      Total compiled: $(find "$BUILD_DIR/obj" -name "*.class" | wc -l) classes"

# --- Step 3: Dex ---
echo "[3/6] Converting bytecode to DEX..."

# Strip Java 9+ multi-release entries from Kotlin stdlib to avoid parse errors
mkdir -p "$BUILD_DIR/kotlin_stripped"
cd "$BUILD_DIR/kotlin_stripped"
jar xf "$KOTLIN_HOME/lib/kotlin-stdlib.jar"
rm -rf META-INF/versions
jar cf "$BUILD_DIR/kotlin-stdlib-stripped.jar" .
cd "$PROJECT_DIR"

$DX --dex \
    --no-strict \
    --min-sdk-version=26 \
    --output="$BUILD_DIR/classes.dex" \
    "$BUILD_DIR/obj" \
    "$BUILD_DIR/kotlin-stdlib-stripped.jar"

ls -lh "$BUILD_DIR/classes.dex"

# --- Step 4: Add classes.dex UNCOMPRESSED to APK ---
# APK Signature Scheme v2/v3 requires DEX to be stored uncompressed and 4-byte aligned
echo "[4/6] Adding uncompressed DEX to APK..."
cp "$BUILD_DIR/app-resources.apk" "$BUILD_DIR/app-with-dex.apk"

python3 - <<PYEOF
import zipfile, os

apk_path = "$BUILD_DIR/app-with-dex.apk"
dex_path = "$BUILD_DIR/classes.dex"

with zipfile.ZipFile(apk_path, 'a', allowZip64=False) as zf:
    info = zipfile.ZipInfo('classes.dex')
    info.compress_type = zipfile.ZIP_STORED  # MUST be uncompressed for APK v2/v3
    with open(dex_path, 'rb') as f:
        zf.writestr(info, f.read())

print("      classes.dex added (uncompressed)")
PYEOF

# --- Step 5: Zipalign (must come BEFORE signing) ---
echo "[5/6] Aligning APK to 4-byte boundaries..."
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

# Verify
echo ""
apksigner verify --verbose "$APK_FINAL" 2>&1 | grep -E "Verif|ERROR" | head -10
