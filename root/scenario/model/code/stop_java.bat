@ECHO OFF
::stop_java.bat
::this kills all java processes
:: crf March 18, 2013
TASKKILL /IM "java.exe" /F
EXIT /B 0
