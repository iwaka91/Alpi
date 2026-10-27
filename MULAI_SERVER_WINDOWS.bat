@echo off
cd /d "%~dp0"
py -3 --version >nul 2>&1
if errorlevel 1 (
  echo Pasang Python 3.12 atau lebih baru dari python.org terlebih dahulu.
  pause
  exit /b 1
)
if not exist .venv py -3 -m venv .venv
.venv\Scripts\python.exe -m pip install -r server\requirements.txt
if errorlevel 1 (
  echo Instalasi dependensi gagal. Periksa koneksi internet.
  pause
  exit /b 1
)
.venv\Scripts\python.exe server\run.py
pause
