@echo off
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0hexplore.ps1" setup %*
exit /b %ERRORLEVEL%
