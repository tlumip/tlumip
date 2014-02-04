@ECHO OFF
echo. 2>"C:\models\tlumip\swim2_beta\scenario_ts2visum\model_report.txt"
cmd /C ""C:\models\tlumip\swim2_beta\scenario_ts2visum\model\code\report.bat" *****Model run started*****>>"C:\models\tlumip\swim2_beta\scenario_ts2visum\model_report.txt" 2>&1"
cmd /C ""C:\models\tlumip\swim2_beta\scenario_ts2visum\model\code\report.bat" Starting SL in year 20>>"C:\models\tlumip\swim2_beta\scenario_ts2visum\model_report.txt" 2>&1"
"C:\models\tlumip\swim2_beta\model\lib\java7\jre\bin\java.exe" -cp "C:\models\tlumip\swim2_beta\scenario_ts2visum\model\code\tlumip.jar;C:\models\tlumip\swim2_beta\scenario_ts2visum\model\code\xmlrpc-2.0.jar;C:\models\tlumip\swim2_beta\scenario_ts2visum\model\code\commons-codec-1.3.jar;C:\models\tlumip\swim2_beta\scenario_ts2visum\model\code;C:\models\tlumip\swim2_beta\scenario_ts2visum\model\config" -Xmx3000m "-Dlog4j.configuration=info_log4j.xml" -server com.pb.tlumip.ao.ModelEntry SL "property_file=C:/models/tlumip/swim2_beta/scenario_ts2visum/outputs/t20/sl_generate_select_link_data.properties"
IF %ERRORLEVEL% NEQ 0 GOTO MODEL_ERROR
cmd /C ""C:\models\tlumip\swim2_beta\scenario_ts2visum\model\code\report.bat" Finished SL>>"C:\models\tlumip\swim2_beta\scenario_ts2visum\model_report.txt" 2>&1"
cmd /C ""C:\models\tlumip\swim2_beta\scenario_ts2visum\model\code\report.bat" Starting SL in year 20>>"C:\models\tlumip\swim2_beta\scenario_ts2visum\model_report.txt" 2>&1"
"C:\models\tlumip\swim2_beta\model\lib\java7\jre\bin\java.exe" -cp "C:\models\tlumip\swim2_beta\scenario_ts2visum\model\code\tlumip.jar;C:\models\tlumip\swim2_beta\scenario_ts2visum\model\code\xmlrpc-2.0.jar;C:\models\tlumip\swim2_beta\scenario_ts2visum\model\code\commons-codec-1.3.jar;C:\models\tlumip\swim2_beta\scenario_ts2visum\model\code;C:\models\tlumip\swim2_beta\scenario_ts2visum\model\config" -Xmx3000m "-Dlog4j.configuration=info_log4j.xml" -server com.pb.tlumip.ao.ModelEntry SL "property_file=C:/models/tlumip/swim2_beta/scenario_ts2visum/outputs/t20/sl_append_select_link_to_trips.properties"
IF %ERRORLEVEL% NEQ 0 GOTO MODEL_ERROR
cmd /C ""C:\models\tlumip\swim2_beta\scenario_ts2visum\model\code\report.bat" Finished SL>>"C:\models\tlumip\swim2_beta\scenario_ts2visum\model_report.txt" 2>&1"
cmd /C ""C:\models\tlumip\swim2_beta\scenario_ts2visum\model\code\report.bat" *****Model run finished*****>>"C:\models\tlumip\swim2_beta\scenario_ts2visum\model_report.txt" 2>&1"
GOTO END


:MODEL_ERROR
cmd /C ""C:\models\tlumip\swim2_beta\scenario_ts2visum\model\code\report.bat" *****Model run error (finished abnormally)*****>>"C:\models\tlumip\swim2_beta\scenario_ts2visum\model_report.txt" 2>&1"


:END
CALL "C:\models\tlumip\swim2_beta\scenario_ts2visum\model\code\stop_run.bat"
