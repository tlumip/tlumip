cd %~dp0..\..
set year=t29
set min_core=10
set max_core=20
set inc_core=2
..\model\lib\Python27\python.exe model\code\visum\test_visum_multiprocessor.py "%~dp0..\..\outputs\%year%\ta.properties" %min_core% %max_core% %inc_core%
