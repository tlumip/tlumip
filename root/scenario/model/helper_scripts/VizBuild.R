# VizBuild.R
# Alex Bettinardi 
# 10-27-21

# In 2021 SWIM was improved such that VIZ databases now build throughout 
# the SWIM run and don't have to wait until the end step - greatly speeding
# up the last Viz build step.  However, the current implementation fails if
# any situation that trips up the SWIM run.  So if SWIM doesn't run cleanly
# through to the end, this script is needed to finalize the SWIM database

# This script must be run in the scenario folder where the viz is desired. 

# reminder to user - you might need to hand clear db, zip files, and log files from a failed run

# Get full run batch file
Run <- readLines("model\\code\\model_runner\\model_run_batch.bat") 

# Get Header
Head <- Run[1:7]

# Remove Header
Run <- Run[8:length(Run)]

# get Viz years
VizYears <- grep("start \"\" cmd ",Run)

# Update lines
Run <- gsub("start \"\" cmd ","cmd ",Run)

# Indentify the last line of the viz build and run the remainder
Tail <- grep("Starting VIZ",Run)
Tail <- Run[Tail[length(Tail)]:length(Run)]

# develop and save new "run_restart.bat" file which is an edited copy of "model_run_batch.bat"
writeLines(c(Head,Run[sort(c(VizYears,VizYears-1,VizYears+1))],Tail), "Viz_Build.bat")  
