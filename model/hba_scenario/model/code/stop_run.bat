@ECHO OFF
::stop_run.bat - kills all java, R, and python processes
::this is waaay aggressive, so only do this if you mean to!
:: crf March 18, 2013
TASKKILL /IM "java.exe" /F
TASKKILL /IM "Rterm.exe" /F
TASKKILL /IM "R.exe" /F
TASKKILL /IM "python.exe" /F
TASKKILL /IM "Visum*" /F
EXIT /B 0
