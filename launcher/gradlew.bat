@echo off
setlocal
for %%I in ("%~dp0..") do set "APP_HOME=%%~fI"
"%JAVA_HOME%\bin\java.exe" -classpath "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain -p "%APP_HOME%\launcher" %*
exit /b %ERRORLEVEL%
