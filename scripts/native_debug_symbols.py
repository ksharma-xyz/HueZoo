#!/usr/bin/env python3
"""
Zips Huezoo's stripped native debug symbols and drops them in ~/Downloads.

Run after a release build:
    python3 scripts/native_debug_symbols.py

Upload the resulting zip to:
    Google Play Console → your app → Android vitals → Deobfuscation files
"""

import os
import sys
import zipfile
from datetime import datetime
from pathlib import Path

# ── Paths ─────────────────────────────────────────────────────────────────────

REPO_ROOT = Path(__file__).resolve().parent.parent

SYMBOLS_DIR = (
    REPO_ROOT
    / "androidApp/build/intermediates/stripped_native_libs"
    / "release/stripReleaseDebugSymbols/out/lib"
)

DOWNLOADS = Path.home() / "Downloads"

ABIS = ["armeabi-v7a", "arm64-v8a", "x86", "x86_64"]

# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    # Verify the symbols directory exists
    if not SYMBOLS_DIR.exists():
        print(f"❌  Symbols directory not found:\n   {SYMBOLS_DIR}")
        print("\n   Run a release build first:")
        print("   ./gradlew :androidApp:assembleRelease\n")
        sys.exit(1)

    # Collect .so files across all ABIs
    so_files = []
    for abi in ABIS:
        abi_dir = SYMBOLS_DIR / abi
        if abi_dir.exists():
            found = list(abi_dir.glob("*.so"))
            so_files.extend(found)
            print(f"   {abi}: {len(found)} .so file(s)")
        else:
            print(f"   {abi}: (not present)")

    if not so_files:
        print("\n❌  No .so files found — did the release build complete successfully?")
        sys.exit(1)

    # Build output filename with timestamp
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    zip_name = f"huezoo-native-debug-symbols-{timestamp}.zip"
    zip_path = DOWNLOADS / zip_name

    # Create zip, preserving abi/libname.so structure
    with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as zf:
        for so_file in so_files:
            # Store as armeabi-v7a/libfoo.so etc.
            arcname = so_file.relative_to(SYMBOLS_DIR)
            zf.write(so_file, arcname)

    size_kb = zip_path.stat().st_size // 1024
    print(f"\n✅  {zip_name}  ({size_kb} KB)")
    print(f"   Saved to: {zip_path}")
    print("\n   Upload at: Play Console → Android vitals → Deobfuscation files")


if __name__ == "__main__":
    main()
