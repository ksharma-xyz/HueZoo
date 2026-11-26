# On-device Color Memory player (runs under adb shell, no host round trips; screencap is slow on a loaded host).
# usage: sh cm.sh PAx PAy PBx PBy SAMEx SAMEy DIFFx DIFFy ROUNDS WRONG_FROM
# Phases (measured on the phone emulator): memorize ~3 s (chamber A shows its colour, B shows something else),
# decision (A sealed and dark, B live), reveal ~1 s after the answer (A and B both shown).
# The script remembers chamber A's latest colour while A is lit, and answers in the decision phase.
# If no colour was seen for a round (screencap too slow / phase not rendered) it answers SAME blind.
# From round WRONG_FROM on it answers the opposite of the truth on purpose (a correct answer at DeltaE <= 1.0 turns the
# result banner to LEGENDARY instead of COMPLETE).
W=1080; [ -n "$W_PX" ] && W=$W_PX
PAx=$1; PAy=$2; PBx=$3; PBy=$4; SX=$5; SY=$6; DX=$7; DY=$8; ROUNDS=$9; WF=${10}
F=/data/local/tmp/s.raw
px() { dd if=$F bs=4 skip=$((3 + $2 * W + $1)) count=1 2>/dev/null | od -An -tu1; }
dark() { set -- $1; [ -n "$1" ] && [ $1 -lt 30 ] && [ $2 -lt 30 ] && [ $3 -lt 40 ]; }
lit() { set -- $1; [ -n "$1" ] && { [ $1 -ge 80 ] || [ $2 -ge 80 ] || [ $3 -ge 80 ]; }; }
absd() { d=$(($1 - $2)); [ $d -lt 0 ] && d=$((0 - d)); echo $d; }
mem=""; n=0; t=0
while [ $n -lt $ROUNDS ] && [ $t -lt 400 ]; do
  screencap $F; t=$((t + 1))
  a=$(px $PAx $PAy); b=$(px $PBx $PBy)
  if lit "$a"; then
    mem="$a"; echo "A lit $a"
  elif dark "$a" && lit "$b"; then
    n=$((n + 1))
    if [ -n "$mem" ]; then
      set -- $mem; m1=$1; m2=$2; m3=$3; set -- $b
      same=0
      [ $(absd $m1 $1) -le 2 ] && [ $(absd $m2 $2) -le 2 ] && [ $(absd $m3 $3) -le 2 ] && same=1
      [ $n -ge $WF ] && same=$((1 - same))
      echo "round $n mem=$mem b=$b -> same=$same"
    else
      same=1; echo "round $n blind b=$b -> same=1"
    fi
    if [ $same -eq 1 ]; then input tap $SX $SY; else input tap $DX $DY; fi
    mem=""; sleep 2
  fi
done
echo "answered $n"
