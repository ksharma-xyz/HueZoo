#!/bin/zsh
# Run a Maestro flow (optional) on exactly one simulator, then take the final pixels with simctl.
# usage: capture-ios.sh <udid> <output.png> <width> <height> [flow.yaml|-] [maestro -e KEY=VALUE ...]
#   flow "-" = no flow, just screenshot the current state (used for 01-home).
#   Set DISPLAY_ID=1 for the iPhone Duo outer display.
# Same idea as KRAIL store-listing/framework/capture-ios.sh: one target sim, Maestro pinned to
# its UDID (other running sims are left alone), pixels from simctl (never from Maestro), dimensions verified, no resizing.
set -euo pipefail
(( $# >= 5 )) || { print -u2 "usage: $0 <udid> <out.png> <w> <h> <flow.yaml|-> [-e K=V ...]"; exit 2; }
udid="$1"; output="$2"; ew="$3"; eh="$4"; flow="$5"; shift 5
here="${0:A:h}"

xcrun simctl boot "$udid" 2>/dev/null || true
xcrun simctl bootstatus "$udid" -b >/dev/null

if [[ "$flow" != "-" ]]; then
  (cd "$here" && maestro --device "$udid" test "$@" "$flow")
fi
sleep 2   # let the last animation settle

mkdir -p "${output:h}"
disp=(); [[ -n "${DISPLAY_ID:-}" ]] && disp=(--display="$DISPLAY_ID")
for attempt in 1 2 3 4 5 6; do
  xcrun simctl io "$udid" screenshot "${disp[@]}" "$output" >/dev/null 2>&1
  # Duo outer display: the camera circle flickers into some frames (pure black disc at 1255,140).
  [[ -n "${DISPLAY_ID:-}" ]] || break
  sips -s format bmp "$output" --out "$output.bmp" >/dev/null
  px="$(python3 -I "$here/pixel.py" "$output.bmp" 1255 140)"; rm -f "$output.bmp"
  [[ "$px" != "0 0 0" ]] && break
  print "camera circle in frame (attempt $attempt), retaking"; sleep 1.5
done

aw="$(sips -g pixelWidth "$output" | awk '/pixelWidth/ {print $2}')"
ah="$(sips -g pixelHeight "$output" | awk '/pixelHeight/ {print $2}')"
if [[ "$aw" != "$ew" || "$ah" != "$eh" ]]; then
  print -u2 "WRONG SIZE ${aw}x${ah}; expected ${ew}x${eh}: $output"; exit 1
fi
print "captured $output ${aw}x${ah}"
