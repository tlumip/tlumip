# RunFuture.R
# Alex Bettinardi 
# 8-18-20
# This script will create a new batch file, run_model_Future.bat.
# The point of this is to start a run at a desired future point
# specifically for future runs / scenarios.
# The idea is that a user will have already run historic years.
# Then they copied over the last two years of that run, updated tstep.csv
# and run build_run.bat (which includes all history and future years).
# And then run this script to generate run_model_Future.bat which won't 
# run the historic years, it will just pick up the run from the specified location
# in the run stream, which is intended to be the future (un-run) years in this case.


# User Step - the point to start the run
StartPoint <- "Starting SI in year 30"

# Get scenario directory
Scen <- choose.dir(getwd())

# Get full run batch file
Run <- readLines(paste(Scen,"\\model\\code\\model_runner\\model_run_batch.bat",sep="")) 

# Identify where to start the run from
ReRunStart <- grep(StartPoint, Run)[1]

# develop and save new "model_run_batch_future.bat" file which is an edited copy of "model_run_batch.bat"
writeLines(Run[c(1:7,ReRunStart:length(Run))], paste(Scen,"\\model\\code\\model_runner\\model_run_batch_future.bat",sep=""))  

# Write out a new run bat file for the updated batch runner
temp <- readLines(paste(Scen,"\\run_model.bat",sep=""))[c(1:2,4:6)]
temp[3] <- gsub("model_run_batch.bat", "model_run_batch_future.bat",temp[3])
writeLines(temp, paste(Scen,"\\run_model_Future.bat",sep=""))

# clean up space
rm(StartPoint, Scen, Run, ReRunStart, temp)                    