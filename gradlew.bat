@echo off
setlocal
if exist "%USERPROFILE%\.gradle\wrapper\dists\gradle-8.10.2-bin" (
  gradle wrapper --gradle-version 8.10.2
) else (
  gradle wrapper --gradle-version 8.10.2
)
gradle %*
