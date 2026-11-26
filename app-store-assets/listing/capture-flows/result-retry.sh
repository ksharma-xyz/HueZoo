#!/bin/zsh
# Re-run ios-result.yaml with random SAME/DIFFERENT answers until it ends on
# "MISSION OUTCOME: COMPLETE" with a new personal best, then keep that frame.
# usage: result-retry.sh <udid> <out.png> <width> <height> <BACK_POINT e.g. 11%,7%> [max-attempts]
# Set DISPLAY_ID=1 for the Duo outer display.
set -uo pipefail
udid="$1"; out="$2"; w="$3"; h="$4"; back="$5"; max="${6:-10}"
here="${0:A:h}"
for n in {1..$max}; do
  a=(); for i in 1 2 3 4 5; do (( RANDOM % 2 )) && a+=(-e A$i=SAME) || a+=(-e A$i=DIFFERENT); done
  print "attempt $n: ${a[*]}"
  if "$here/capture-ios.sh" "$udid" "$out" "$w" "$h" ios-result.yaml -e BACK_POINT="$back" "${a[@]}"; then
    print "good result on attempt $n"; exit 0
  fi
  (cd "$here" && maestro --device "$udid" test -e BACK_POINT="$back" ios-back-home.yaml >/dev/null 2>&1)
done
print -u2 "no COMPLETE result in $max attempts"; exit 1
