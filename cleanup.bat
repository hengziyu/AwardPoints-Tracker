@echo off
setlocal

REM --- Change to the script's directory to ensure files are found ---
cd /d "%~dp0"

echo Cleaning up generated files from: %cd%
echo.

REM --- Delete log file ---
if exist "app.log" (
    echo Deleting log file: app.log
    del "app.log"
)

REM --- Delete generated Excel files ---
if exist "Awards_Summary.xlsx" (
    echo Deleting Excel file: Awards_Summary.xlsx
    del "Awards_Summary.xlsx"
)
if exist "Raw_Source.xlsx" (
    echo Deleting Excel file: Raw_Source.xlsx
    del "Raw_Source.xlsx"
)
if exist "Student_Awards.xlsx" (
    echo Deleting Excel file: Student_Awards.xlsx
    del "Student_Awards.xlsx"
)

REM --- Delete database file ---
if exist "student.db" (
    echo Deleting database: student.db
    del "student.db"
)

REM --- Delete snapshot files ---
if exist "snapshot_*.json" (
    echo Deleting JSON snapshots...
    del /q "snapshot_*.json"
)
if exist "snapshot_*.json.gz" (
    echo Deleting GZIP snapshots...
    del /q "snapshot_*.json.gz"
)

REM --- Delete target directory ---
if exist "target" (
    echo Deleting build directory: target
    rmdir /s /q "target"
)

echo.
echo Cleanup complete. The project directory has been restored to its original state.
pause
endlocal

