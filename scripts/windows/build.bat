@echo off
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0hexplore.ps1" build %*
exit /b %ERRORLEVEL%
