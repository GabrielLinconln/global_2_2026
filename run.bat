@echo off
setlocal enabledelayedexpansion

echo ==============================================================
echo   Plantao Clima - Compilando e Iniciando (Java + JDBC + H2)
echo ==============================================================

set "JAVAC_CMD=javac"
set "JAVA_CMD=java"

where javac >nul 2>&1
if errorlevel 1 (
    for /d %%D in ("%USERPROFILE%\.jdks\*") do (
        if exist "%%D\bin\javac.exe" (
            set "JAVAC_CMD=%%D\bin\javac.exe"
            set "JAVA_CMD=%%D\bin\java.exe"
        )
    )
)

if not exist "build\classes" mkdir "build\classes"
if exist "build\sources.txt" del /q "build\sources.txt"

set "ROOT=%CD%\"
for /r "src\main\java" %%F in (*.java) do (
    set "P=%%F"
    set "P=!P:%ROOT%=!"
    set "P=!P:\=/!"
    echo !P!>> "build\sources.txt"
)

"%JAVAC_CMD%" -encoding UTF-8 -cp "lib\h2-2.3.232.jar" -d "build\classes" "@build\sources.txt"
if errorlevel 1 (
    echo [ERRO] Falha na compilacao Java. Verifique se o JDK 17+ esta instalado.
    exit /b 1
)

echo Compilacao concluida! Iniciando servidor em http://localhost:8080 ...
"%JAVA_CMD%" -cp "build\classes;lib\h2-2.3.232.jar" br.com.plantaoclima.Main %*
