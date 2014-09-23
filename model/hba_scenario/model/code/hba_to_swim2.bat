@ECHO OFF
:: This batch program copies the necessary input and bootstrap files from the
::  current HBA svn directory to the scenario that this program resides in
:: The HBA repository that holds these files is located at:
::  http://svn.hbaspecto.com/svn/pecas/PECASOregon/C05
:: crf March 18, 2013
SET USAGE=usage: hba_to_swim2.bat hba_directory

:: get hba directory, exit if it is not there
IF [%1]==[] GOTO ARG_ERROR
SET HBA_DIR=%1
IF NOT EXIST %HBA_DIR% GOTO ARG_ERROR

::set scenario directory
pushd "%~dp0"
cd ..\..
set TLUMIP_DIR=%CD%
popd

ECHO FROM DIR: %HBA_DIR%
ECHO TO DIR: %TLUMIP_DIR%

:: code files
CALL:copy_file AllYears\Code model\code\aa censusdata_r2348.jar
CALL:copy_file AllYears\Code model\code\aa common-base_r2753.jar
CALL:copy_file AllYears\Code model\code\aa log4j-1.2.9.jar
CALL:copy_file AllYears\Code model\code\aa mtj.jar
CALL:copy_file AllYears\Code model\code\aa or124.jar
CALL:copy_file AllYears\Code model\code\aa oregonPP_r2777.jar
CALL:copy_file AllYears\Code model\code\aa PecasV2.7_r2753.jar
CALL:copy_file AllYears\Code model\code\aa postgresql-8.4-701.jdbc4.jar
CALL:copy_file AllYears\Code model\code\aa ref_r1573.jar
CALL:copy_file AllYears\Code model\code\aa simple_orm_r1504.jar

::bootstrap files
CALL:copy_file 2008 outputs\t18 ActivityLocations.csv
::CALL:copy_file 2009 outputs\t19 ActivityLocations.csv
::CALL:copy_file 2009 outputs\t19 ActivitySummary.csv
::CALL:copy_file 2009 outputs\t19 MakeUse.csv
CALL:copy_file 2009 inputs\t0 ActivityConstraintsI.csv
CALL:copy_file_to 2009\aa_inputs outputs\t19 FloorspaceI.csv AgForestFloorspace.csv
CALL:copy_file 2009\aa_inputs outputs\t19 FloorspaceI.csv
CALL:copy_file 2009\aa_inputs outputs\t19 FloorspaceInventory.csv
CALL:copy_file 2009\aa_inputs outputs\t19 Increments.csv
CALL:copy_file 2009\aa_inputs outputs\t19 Increments_Matrix.csv

::ned files - Carl/NED should control this, not HBA
::CALL:copy_file 2009\ned_outputs outputs\t19 activity_forecast.csv
::CALL:copy_file 2009\ned_outputs outputs\t19 construction_forecast.csv
::CALL:copy_file 2009\ned_outputs outputs\t19 government_forecast.csv
::CALL:copy_file 2009\ned_outputs outputs\t19 population_forecast.csv
::CALL:copy_file 2009\ned_outputs outputs\t19 trade_forecast.csv

::parameters
CALL:copy_file AllYears\Inputs inputs\parameters ActivitiesI.csv
CALL:copy_file AllYears\Inputs inputs\parameters ActivityTotalsI.csv
CALL:copy_file AllYears\Inputs inputs\parameters ActivitiesZonalValuesI.csv
CALL:copy_file AllYears\Inputs inputs\parameters ActivitySizeTermsI.csv
CALL:copy_file AllYears\Inputs inputs\parameters AgForestFloorspace.csv
::CALL:copy_file AllYears\Inputs inputs\parameters alpha2beta.csv
CALL:copy_file AllYears\Inputs inputs\parameters CommoditiesI.csv
CALL:copy_file AllYears\Inputs inputs\parameters ExchangeImportExportI.csv
CALL:copy_file AllYears\Inputs inputs\parameters FloorspaceSupplyI.csv
CALL:copy_file AllYears\Inputs inputs\parameters FloorspaceZonesI.csv
CALL:copy_file AllYears\Inputs inputs\parameters HistogramsI.csv
CALL:copy_file AllYears\Inputs inputs\parameters PECASZonesI.csv
CALL:copy_file AllYears\Inputs inputs\parameters TechnologyOptionsI.csv
CALL:copy_file_to AllYears\Inputs inputs\t0 WorldZoneExternalStationDistancesAA.csv WorldZoneExternalStationDistances.csv

:: logsums that aa needs
::b4mcls_beta.zmx
::b5mcls_beta.zmx
::b8mcls_beta.zmx
::c4mcls_beta.zmx
::o4mcls_beta.zmx
::s4mcls_beta.zmx
::w1mcls_beta.zmx
::w4mcls_beta.zmx
::w7mcls_beta.zmx

:: skims that aa needs
::betaopautodist.zmx
::betaopautofftime.zmx
::betaopautotime.zmx
::betaopautotoll.zmx
::betaoptrk1dist.zmx
::betaoptrk1fftime.zmx
::betaoptrk1time.zmx
::betaoptrk1toll.zmx
::betapkautodist.zmx
::betapkautofftime.zmx
::betapkautotime.zmx
::betapkautotoll.zmx
::betapktrk1dist.zmx
::betapktrk1fftime.zmx
::betapktrk1time.zmx
::betapktrk1toll.zmx

GOTO END

:ARG_ERROR
ECHO HBA directory argument missing or does not exist
ECHO %USAGE%
GOTO END

:copy_file
COPY /Y %HBA_DIR%\%~1\%~3 %TLUMIP_DIR%\%~2\%~3
GOTO:EOF

:copy_file_to
COPY /Y %HBA_DIR%\%~1\%~3 %TLUMIP_DIR%\%~2\%~4
GOTO:EOF

:END
