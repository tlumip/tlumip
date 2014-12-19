@ECHO OFF
::report.bat
::this just echos out the date time message
::where message is arguments to the batch file
FOR /f "tokens=* delims= " %%a IN ('date /t') DO (set mydate=%%a)
FOR /f "tokens=* delims= " %%a IN ('time /t') DO (set mytime=%%a)
echo %mydate% %mytime% - %*
