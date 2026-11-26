F=/data/local/tmp/s.raw; W=1080
px() { dd if=$F bs=4 skip=$((3 + $2 * W + $1)) count=1 2>/dev/null | od -An -tu1; }
i=0; ans=0
while [ $i -lt 60 ]; do
  screencap $F; i=$((i+1))
  a=$(px 300 1500); b=$(px 800 1500)
  echo "$i A:$a B:$b"
  set -- $a
  if [ "$1" -lt 30 ] && [ "$2" -lt 30 ]; then
    set -- $b
    if [ "$1" -ge 80 ] || [ "$2" -ge 80 ] || [ "$3" -ge 80 ]; then
      echo "ANSWER SAME"; input tap 283 2239; sleep 1; ans=$((ans+1))
    fi
  fi
  [ $ans -ge 3 ] && [ $i -gt 40 ] && break
done
