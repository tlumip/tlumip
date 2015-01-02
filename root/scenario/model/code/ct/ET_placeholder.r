# ET placeholder that writes zero trips: use this version of ET so that the 
# expected ET output is written as before, but with zero flows. We do this
# because the revised CT includes through truck flows, whereas the original 
# version did not. We write the same output file in the target directory. This
# function assumes that you will pass the destination folder as the sole 
# parameter. If you stitch this into the CT runner you can use the working 
# folder in its runtime parameters:
#       ET(RTP[["ct.working.folder"]])
# Otherwise simply pass the full path name of the output folder for the current
# simulation year.
# Rick Donnelly <donnellyr@pbworld.com>  31-Dec-2014

ET <- function() {
    # Define the truck types currently in use, for we'll need to write one zero
    # trip record for each truck type. Replace these with current truck types if
    # needed.
    old_truck_types <- c("TRK3", "TRK4")
    
    # Create a table with the zero volumes between a single OD pair, for each
    # truck type. Use external stations that are not likely to be revised or
    # eliminated (based on discussion in 30-Dec-2014 teleconference).
    zeros <- data.frame(origin = 5001, destination = 5002, tripStartTime = 1300,
        truckClass = old_truck_types, truckVolume = 0)
    
    # Write the result
    FN <- file.path(RTP[["Working_Folder"]], "Trips_ETTruck.csv")
    write.table(zeros, file=FN, sep=',', row.names=FALSE, quote=FALSE)
}

ET()
