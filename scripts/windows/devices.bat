@echo off
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0hexplore.ps1" devices %*
exit /b %ERRORLEVEL%
