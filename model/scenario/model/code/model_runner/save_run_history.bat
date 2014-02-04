@ECHO OFF
:: save_run_history.bat
:: saves the run history in a zip file (adds it to it if it already exists).
:: the following files are saved:
::   model_report.txt
::   model_run_output.txt
:: these are added to the following directory in the zip file:
::  /run/[month]_[day]_[year]__[hour]_[minute]_[second]_[AM/PM]/
:: crf March 18, 2013

SETLOCAL enabledelayedexpansion
::this mess is used to get a nice date time string - ugh
FOR /F "skip=1 tokens=1-6" %%A IN ('WMIC Path Win32_LocalTime Get Day^,Hour^,Minute^,Month^,Second^,Year /Format:table') DO (
    IF %%A LSS 10 SET DAY=0%%A
    IF %%A GTR 9 SET DAY=%%A
    IF %%B LSS 13 SET PERIOD=AM
    IF %%B GTR 12 SET PERIOD=PM
    SET HOUR=%%B
    IF %%B GTR 12 SET /A HOUR=%%B-12
    IF !HOUR! LSS 10 SET HOUR=0!HOUR!
    IF %%C LSS 10 SET MINUTE=0%%C
    IF %%C GTR 9 SET MINUTE=%%C
    IF %%D LSS 10 SET MONTH=0%%D
    IF %%D GTR 9 SET MONTH=%%D
    IF %%E LSS 10 SET SECOND=0%%E
    IF %%E GTR 9 SET SECOND=%%E
    SET YEAR=%%F
    IF NOT DEFINED DT SET DT=!MONTH!_!DAY!_!YEAR!__!HOUR!_!MINUTE!_!SECOND!!PERIOD!
)

::use batch file directory to get to scenario directory and subsequent locations
SET SCEN_DIR=%~dp0..\..\..
SET SZ_EXEC=%SCEN_DIR%\..\model\lib\7za.exe
SET OUT_DIR=%SCEN_DIR%\_run\%DT%

::make an empty zip file if it doesn't already exist
IF NOT EXIST "%SCEN_DIR%\run_history.zip" "%SZ_EXEC%" a -tzip "%SCEN_DIR%\run_history.zip" "*.notexist"

::make a temp directory for zip contents,and add them to zip file
MKDIR "%OUT_DIR%"
IF EXIST "%SCEN_DIR%\model_report.txt" XCOPY /Y "%SCEN_DIR%\model_report.txt" "%OUT_DIR%"
IF EXIST "%SCEN_DIR%\model_run_output.txt" XCOPY /Y "%SCEN_DIR%\model_run_output.txt" "%OUT_DIR%"
"%SZ_EXEC%" u -tzip "%SCEN_DIR%\run_history.zip" "%SCEN_DIR%\_run*"

::bye bye temp dir
RMDIR /S /Q "%SCEN_DIR%\_run"
