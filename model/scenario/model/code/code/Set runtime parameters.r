# Set the runtime parameters that will be used by CT. We will use a dictionary
# to store them, saved to a binary file that can be read by other modules.
RTP <- new.env()

# GLOBAL PARAMETERS
#-------------------
# The global parameters are used by both the macro-level (commodity flow) and
# micro-level (truck tour) components of CT. The target year changes depending
# upon which year of the simulation is being run, as does the working folder.
# The remaining parameters remain constant for any given model run, but can be
# changed on the fly.
RTP[["Version"]] <- "1.0"
RTP[["Random_Seed"]] <- 5702
RTP[["Extended_Trace"]] <- FALSE   # Show which list element we are working on?
RTP[["Target_Year"]] <- 2012   # What year are we working on in the FAF?

# Define the folder that we are working in, as well as where we store the 
# parameter files and static data used by CT.
RTP[["CT_Core_Data"]] <- "/Users/rick/Models/swim2/ctdev/CT3E/data/"
RTP[["Working_Folder"]] <- "/Users/rick/Models/swim2/ctdev/outputs/t20/"
RTP[["Code_Depository"]] <- "/Users/rick/Models/swim2/ctdev/CT3E/code/"

# FAF-RELATED PARAMETERS
#------------------------
# The FAF is a database of annual flows, from which we sample to obtain weekly
# and daily estimates of flows by mode of transport, commodity, and origin and
# destination. So we will need to sample for a given week and day in order to
# create trip lists by those time intervals. We assume that demand is flat 
# across the year, so the choice of week is somewhat arbitrary. However, we
# probably want to sample weekdays (days 2-6), so choose accordingly
RTP[["Target_Week"]] <- 23
RTP[["Target_Day"]] <- 4
RTP[["Weeks_Per_Year"]] <- 50  # Assumes holidays are off days
RTP[["Oregon_Regions"]] <- c(411, 419)   # FAF regions within Oregon

# The truckload equivalencies are national estimates, and never calibrated or
# validated at that, so we will need to adjust the total trucks crossing the
# Oregon border to match the counts there. This parameter does so. Note that it
# will need to be recalibrated if you change the cordon counts or use versions
# of FAF later than v3.5.
RTP[["FAF_external_scaling_factor"]] <- 2.41

# TRUCK TOUR MODEL(S) PARAMETERS
#--------------------------------
# These parameters apply to the microsimulation of truck tours.

# We will often calculate a total amount of activity for a firm or sector and
# then test to see how close the sum of the microsimulated values comes to it.
# We will replan the activity sequence if the two sums differ by more than user-
# specified tolerance, and stop if we replan a given number of times without
# falling within the specified tolerance.
RTP[["Replanning_Threshold"]] <- 2.0  # percent
RTP[["Maximum_Replanning_Attempts"]] <- 25   # Stop if exceeded

# OREGON PORTS
#--------------
# Define the alpha zones that correspond to air and marine ports within Oregon
RTP[["Port_of_Portland"]] <- 925
RTP[["Port_of_Coos_Bay"]] <- 2129
RTP[["PDX_Airport"]] <- 904

# SAVE THE RESULTS
#------------------
FN <- paste(RTP[["Working_Folder"]], "ct-runtime-parameters.RData", sep='')
save(RTP, file=FN)