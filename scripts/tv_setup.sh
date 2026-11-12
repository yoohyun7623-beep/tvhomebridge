#!/usr/bin/env bash
# TV 런처 셋업 자동화 (macOS/Linux)
# 사용법: ./scripts/tv_setup.sh <TV_IP> [런처패키지] [--debloat] [--restore]
# 예시  : ./scripts/tv_setup.sh 192.168.0.25 com.spocky.projengmenu --debloat
# 준비  : TvHomeBridge/apks/ 폴더에 브릿지 APK + 런처 APK 넣기
set -euo pipefail

TV_IP="${1:?사용법: $0 <TV_IP> [런처패키지] [--debloat] [--restore]}"
LAUNCHER_PKG="${2:-com.spocky.projengmenu}"
shift $(( $# >= 2 ? 2 : $# ))
DEBLOAT=0; RESTORE=0
for a in "$@"; do
  case "$a" in --debloat) DEBLOAT=1;; --restore) RESTORE=1;; esac
done

DIR="$(cd "$(dirname "$0")" && pwd)"
LIST="$DIR/../app/src/main/assets/bloat_packages.txt"
APKS="$DIR/../apks"
[[ "$TV_IP" == *:* ]] && T="$TV_IP" || T="$TV_IP:5555"
ADB="adb -s $T"

command -v adb >/dev/null || { echo "[오류] adb 없음 (platform-tools 설치)"; exit 1; }
pkgs() { grep -vE '^\s*(#|$)' "$LIST" | tr -d '\r'; }

echo "[1/4] 연결: $T"
adb connect "$T"; sleep 2
[[ "$($ADB get-state 2>/dev/null)" == "device" ]] || { echo "[오류] TV에서 디버깅 허용 팝업 확인"; exit 1; }

if [[ $RESTORE -eq 1 ]]; then
  echo "[복구] 비활성화했던 패키지 다시 활성화"
  while read -r p; do echo " - $p"; $ADB shell pm enable --user 0 "$p" </dev/null || true; done < <(pkgs)
  exit 0
fi

echo "[2/4] APK 설치"
shopt -s nullglob
for f in "$APKS"/*.apk; do echo " → $(basename "$f")"; $ADB install -r -d "$f" || echo "   ✗ 실패"; done

echo "[3/4] 기본 홈 앱 지정 시도: $LAUNCHER_PKG"
COMP="$($ADB shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME -p "$LAUNCHER_PKG" </dev/null | tail -n1 | tr -d '\r')"
if [[ "$COMP" == */* ]]; then
  $ADB shell cmd package set-home-activity --user 0 "$COMP" </dev/null \
    && echo " ✔ 기본 홈 지정: $COMP" || echo " ✗ 지정 실패 → TV 설정에서 수동 지정"
else
  echo " ✗ 런처 컴포넌트를 찾지 못함(패키지명 확인): $LAUNCHER_PKG"
fi

if [[ $DEBLOAT -eq 1 ]]; then
  echo "[4/4] 광고/추적 패키지 비활성화"
  while read -r p; do echo " - $p"; $ADB shell pm disable-user --user 0 "$p" </dev/null || true; done < <(pkgs)
else
  echo "[4/4] 건너뜀 (--debloat 로 활성화)"
fi
adb disconnect "$T" >/dev/null 2>&1 || true
echo "완료"
