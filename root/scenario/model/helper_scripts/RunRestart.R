# RunRestart.R
# Alex Bettinardi 
# 1-5-17
# This script can create a new batch file, "run_restart.bat", if a model crashes unexpectidily.
# This script automates the manual restart process described at:  https://github.com/pbsag/tlumip/wiki/running-scenarios#restarting-a-failed-model-run
# This script does not actually start the run, it just creates the restart batch file.
# To restart a run, run this script, which will crate a "run_restart.bat" file in the directory choosen (to rerun).
# Then open a command window in the scenario directory (the sameway one would for a normal start),
# and then run "run_restart.bat" instead of "run_model.bat" (which would rerun the model from the start/beginning).

# User Step - identify the "model_report.txt" file from the failed scenario, which will also mark the scenario to restart
FileLoc <- choose.files(default = "model_report.txt", caption = "Choose the 'model_report.txt' file from the scenario that failed",
                       multi=F, filters = Filters["txt",])
                       
# Read "model_report.txt" history to find fail point
Report <- readLines(FileLoc)

# Find last module run from the model report
if(length(grep("Model run error", Report[length(Report)]))==1){
  LastLine <- unlist(strsplit(Report[length(Report)-1], " - "))[2]
} else {
  LastLine <- unlist(strsplit(Report[length(Report)], " - "))[2]
}

# Get scenario directory
Scen <- gsub("model_report.txt", "",FileLoc)

# Get full run batch file
Run <- readLines(paste(Scen,"model\\code\\model_runner\\model_run_batch.bat",sep="")) 

# Identify where to start the run from
ReRunStart <- grep(LastLine, Run)[1]

# develop and save new "run_restart.bat" file which is an edited copy of "model_run_batch.bat"
writeLines(Run[c(1:7,ReRunStart:length(Run))], paste(Scen,"run_restart.bat",sep=""))  

# clean up space
rm(FileLoc, Report, LastLine, Scen, Run, ReRunStart)                    