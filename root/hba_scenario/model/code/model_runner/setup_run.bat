@ECHO OFF
:: Setup a model run as defined by the tstps.csv file.
:: Essentially calls model/code/model_runner/build_run.py
::   See that file for more information
:: crf March 18, 2013

::push into batch file directory
pushd "%~dp0"

::move backwards to scenario base directory
cd ..\..\..
set temp_dir=%CD%
cd ..
SET root_dir=%CD%

::remove root_dir from temp_dir to get scenario name
CALL SET scen_name=%%temp_dir:%root_dir%\=%%

::back to batch file directory and run program
cd "%~dp0"
%root_dir%\model\lib\Python27\python.exe build_run.py %root_dir% %scen_name% "%root_dir%\%scen_name%\model\config\tsteps.csv"

::return to original directory
popd
