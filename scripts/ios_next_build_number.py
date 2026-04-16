#!/usr/bin/env python3
"""
ios_next_build_number.py
------------------------
Calculates the next safe CFBundleVersion for a TestFlight upload.

Strategy:
  next = max(asc_latest, github_var) + 1

Why both sources?
  - App Store Connect is the ground truth for what Apple has already accepted
    (covers manual Xcode/Transporter uploads that the CI doesn't know about).
  - The GitHub variable acts as a monotonic floor so we never go backwards even
    if the ASC API returns a stale or lower value (e.g. after build expiry clean-up).

Usage (called from CI — all values come from environment variables):
  Required env vars:
    APP_STORE_CONNECT_API_KEY_KEY_ID     — ASC API key ID
    APP_STORE_CONNECT_API_KEY_ISSUER_ID  — ASC issuer UUID
    APP_STORE_CONNECT_API_KEY_KEY        — ASC private key (PEM, may be base64-encoded)
    ASC_APP_ID                           — Numeric App Store Connect app ID (e.g. 6761459879)

  Optional env vars:
    IOS_BUILD_NUMBER_FLOOR               — GitHub repo variable value (integer string)

  Stdout: a single integer — the next build number to use.
  Exit 1 on any error so CI fails loudly rather than silently using a wrong number.
"""

import json
import os
import sys
import time
import urllib.error
import urllib.request

# ---------------------------------------------------------------------------
# Dependencies: PyJWT + cryptography (installed by the CI step before this
# script runs). Both are available on GitHub-hosted macOS runners via pip.
# ---------------------------------------------------------------------------
try:
    import jwt
except ImportError:
    print("ERROR: PyJWT not installed. Run: pip3 install PyJWT cryptography", file=sys.stderr)
    sys.exit(1)


def _asc_jwt(key_id: str, issuer_id: str, private_key: str) -> str:
    """Return a short-lived App Store Connect API JWT."""
    now = int(time.time())
    payload = {
        "iss": issuer_id,
        "iat": now,
        "exp": now + 120,
        "aud": "appstoreconnect-v1",
    }
    return jwt.encode(payload, private_key, algorithm="ES256", headers={"kid": key_id})


def _latest_asc_build(app_id: str, token: str) -> int:
    """
    Query App Store Connect for the highest CFBundleVersion uploaded for *app_id*.
    Returns 0 if no builds exist yet.
    """
    url = (
        "https://api.appstoreconnect.apple.com/v1/builds"
        f"?filter[app]={app_id}"
        "&sort=-version"
        "&limit=1"
        "&fields[builds]=version"
    )
    req = urllib.request.Request(url, headers={"Authorization": f"Bearer {token}"})
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            data = json.loads(resp.read())
    except urllib.error.HTTPError as exc:
        body = exc.read().decode(errors="replace")
        print(f"ERROR: ASC API returned HTTP {exc.code}: {body}", file=sys.stderr)
        sys.exit(1)
    except Exception as exc:  # noqa: BLE001
        print(f"ERROR: ASC API request failed: {exc}", file=sys.stderr)
        sys.exit(1)

    builds = data.get("data", [])
    if not builds:
        return 0

    raw = builds[0]["attributes"]["version"]
    try:
        return int(raw)
    except ValueError:
        print(f"ERROR: ASC returned non-integer version '{raw}'", file=sys.stderr)
        sys.exit(1)


def main() -> None:
    key_id      = os.environ.get("APP_STORE_CONNECT_API_KEY_KEY_ID", "").strip()
    issuer_id   = os.environ.get("APP_STORE_CONNECT_API_KEY_ISSUER_ID", "").strip()
    private_key = os.environ.get("APP_STORE_CONNECT_API_KEY_KEY", "").strip()
    app_id      = os.environ.get("ASC_APP_ID", "").strip()
    floor_raw   = os.environ.get("IOS_BUILD_NUMBER_FLOOR", "0").strip()

    # Validate required inputs
    missing = [n for n, v in [
        ("APP_STORE_CONNECT_API_KEY_KEY_ID",    key_id),
        ("APP_STORE_CONNECT_API_KEY_ISSUER_ID", issuer_id),
        ("APP_STORE_CONNECT_API_KEY_KEY",        private_key),
        ("ASC_APP_ID",                           app_id),
    ] if not v]
    if missing:
        print(f"ERROR: Missing required env vars: {', '.join(missing)}", file=sys.stderr)
        sys.exit(1)

    try:
        floor = int(floor_raw) if floor_raw else 0
    except ValueError:
        print(f"ERROR: IOS_BUILD_NUMBER_FLOOR='{floor_raw}' is not an integer", file=sys.stderr)
        sys.exit(1)

    token      = _asc_jwt(key_id, issuer_id, private_key)
    asc_latest = _latest_asc_build(app_id, token)

    chosen = max(asc_latest, floor)
    next_build = chosen + 1

    print(
        f"  ASC latest    : {asc_latest}\n"
        f"  GitHub floor  : {floor}\n"
        f"  Using max     : {chosen}\n"
        f"  Next build    : {next_build}",
        file=sys.stderr,
    )

    # Only the integer goes to stdout so the caller can capture it cleanly.
    print(next_build)


if __name__ == "__main__":
    main()

