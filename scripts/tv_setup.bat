@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
REM TV 런처 셋업 자동화 (Windows)
REM 사용법: scripts\tv_setup.bat <TV_IP> [런처패키지] [--debloat] [--restore]

set "TV_IP=%~1"
set "LP=%~2"
if "%LP%"=="" set "LP=com.spocky.projengmenu"
if "%TV_IP%"=="" ( echo 사용법: %~nx0 ^<TV_IP^> [런처패키지] [--debloat] [--restore] & exit /b 1 )
set DEBLOAT=0& set RESTORE=0
for %%A in (%*) do (
  if "%%A"=="--debloat" set DEBLOAT=1
  if "%%A"=="--restore" set RESTORE=1
)
set "DIR=%~dp0"
set "LIST=%DIR%..\app\src\main\assets\bloat_packages.txt"
set "APKS=%DIR%..\apks"
echo %TV_IP% | find ":" >nul
if errorlevel 1 (set "T=%TV_IP%:5555") else (set "T=%TV_IP%")

where adb >nul 2>&1 || ( echo [오류] adb 없음 & exit /b 1 )
echo [1/4] 연결: %T%
adb connect %T%
timeout /t 2 >nul
for /f "delims=" %%S in ('adb -s %T% get-state 2^>nul') do set "ST=%%S"
if not "%ST%"=="device" ( echo [오류] TV에서 디버깅 허용 팝업 확인 & exit /b 1 )

if %RESTORE%==1 (
  echo [복구] 패키지 다시 활성화
  for /f "usebackq eol=# delims=" %%P in ("%LIST%") do adb -s %T% shell pm enable --user 0 %%P
  exit /b 0
)

echo [2/4] APK 설치
for %%F in ("%APKS%\*.apk") do ( echo  → %%~nxF & adb -s %T% install -r -d "%%F" )

echo [3/4] 기본 홈 앱 지정 시도: %LP%
set "COMP="
for /f "delims=" %%C in ('adb -s %T% shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME -p %LP%') do set "COMP=%%C"
echo !COMP! | find "/" >nul
if errorlevel 1 (
  echo  ✗ 런처 컴포넌트를 찾지 못함: %LP%
) else (
  adb -s %T% shell cmd package set-home-activity --user 0 !COMP!
)

if %DEBLOAT%==1 (
  echo [4/4] 광고/추적 패키지 비활성화
  for /f "usebackq eol=# delims=" %%P in ("%LIST%") do ( echo  - %%P & adb -s %T% shell pm disable-user --user 0 %%P )
) else ( echo [4/4] 건너뜀 ^(--debloat 로 활성화^) )
adb disconnect %T% >nul 2>&1
echo 완료
endlocal
