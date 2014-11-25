# @tag{doParallel+foreach version of CT destination choice}
# @author{Rick Donnelly} @version{0.8}  @date{24-Nov-2014}
# This is an adaptation of the original process, which was tested with R's
# parallel package. Unfortunately doParallel is nowhere close to parallel in
# implementation, requiring a different approach.
library(data.table)
library(dplyr)
library(stringr)
library(reshape2)
library(doParallel)

sample_local_truck_destinations <- function(random_number_seed) {
    # Announce yourself
    set.seed(random_number_seed)
    print(str_c("------- sample_local_truck_destinations -------"), quote=FALSE)
    print(str_c("Random seed=", random_number_seed), quote=FALSE)
    simulation.start <- proc.time()
    
    # First load the truck origins and sum them by alpha zone and truck type.
    # Since we are assuming symmetry of flows these become our attractors for
    # destination choice.
    FN <- str_c(RTP[["Working_Folder"]], "ct-internal-truck-origins.RData")
    load(FN)
    truck_origins$destination <- NA   # Filling this in is goal of this exercise
    # Sum the attractors by alpha zone
    zdata <- truck_origins %>% group_by(Azone, truck_type) %>%
        summarise(attractors = n())
    zdata$destination <- as.integer(zdata$Azone)   # FUR
    truck_types <- unique(zdata$truck_type)
    
    # Function to read zmx matrices into R matrices, graciously provided by Ben
    readZipMat = function(FileName) {
        #Make a temporary directory to put unzipped files into
        dir.create( "TempZip" )
        
        #Read matrix attributes
        NumRows = as.integer( scan( unzip( FileName, "_rows", exdir="TempZip" ), what="", quiet=T ) )
        NumCols = as.integer( scan( unzip( FileName, "_columns", exdir="TempZip" ), what="", quiet=T ) )
        RowNames = strsplit( scan( unzip( FileName, "_external row numbers", exdir="TempZip" ), what="", quiet=T ),"," )[[1]]
        ColNames = strsplit( scan( unzip( FileName, "_external column numbers", exdir="TempZip" ), what="", quiet=T ),"," )[[1]]
        
        #Initialize matrix to hold values
        Result.ZnZn = matrix( 0, NumRows, NumCols )
        rownames( Result.ZnZn ) = RowNames
        colnames( Result.ZnZn ) = ColNames
        
        #Read matrix data by row and place in initialized matrix
        RowDataEntries. = paste("row_", 1:NumRows, sep="")
        for(i in 1:NumRows) {
            Result.ZnZn[ i, ] = readBin( unzip( FileName, RowDataEntries.[i], exdir="TempZip" ),
                what=double(), n=NumCols, size=4, endian="big" )
        }
        
        #Remove the temporary directory
        unlink( "TempZip", recursive=TRUE )
        
        #Return the matrix
        Result.ZnZn
    }

    # Use that function to read the skim distance matrix
    FN <- str_c(RTP[["Working_Folder"]], "pkautodist.zmx")
    print(str_c("Reading distance matrix from ", FN), quote=FALSE)
    skim_distances <- readZipMat(FN)
    skim_distances_size <- object.size(skim_distances)
    print(str_c("Distance matrix size=", skim_distances_size, " (",
        round(skim_distances_size/(1024^2), 1), " MB)"), quote=FALSE)
    dzones <- as.integer(colnames(skim_distances))
    
    # Read the ideal trip length probabilities by truck type. These data are in
    # wide format, and we need to first append rows for distances in the skim
    # matrix but not included in the ideal distribution data. Then we'll convert
    # it to tall format to make process it easier.
    FN <- str_c(RTP[["CT_Core_Data"]], "ct-idealized-trip-length-distribution.csv")
    ideal <- fread(FN)
    max_skim_distance <- round(max(skim_distances), 0)
    x <- data.table(distance = 0:max_skim_distance)
    ideal <- merge(ideal, x, by="distance", all.y=TRUE)
    ideal[is.na(ideal)] <- 0.0   # Replace missing values with zeros
    ideal <- melt(ideal, id.vars="distance", variable.name="truck_type",
        value.name="p_distance")
    
    # Read the utility parameters by truck type
    FN <- str_c(RTP[["CT_Core_Data"]], "ct-destination-utility-parameters.csv")
    alphas <- fread(FN)
    
    # Define the function that will choose destination alpha zone
    getDestination <- function(this_origin) {
        # Build a data table with the appropriate skim distances
        W <- data.table(origins = this_origin, destination = dzones,
            distance = round(skim_distances[as.character(this_origin), ], 0))
        
        # Merge the ideal probabilities by distance
        W <- merge(W, t_ideal, by="distance", all.x=TRUE)
        
        # Add the attractors by destination
        W <- merge(W, t_zdata, by=c("destination", "truck_type"), all.x=TRUE)
        
        # Calculate the weighted probability for each destination zone
        W$weighted_p <- (W$p_distance*t_alphas$alpha1)*(W$attractors*t_alphas$alpha2)
        W <- W[!is.na(W$weighted_p) & W$weighted_p!=0,]
        
        # Finally, sample the destination
        sample(W$destination, 1, prob=W$weighted_p)
    }
    
    # Start the cluster now (before looping through by truck_type)
    clusterSize <- detectCores()-1    # Leave room for OS processes
    cluster <- makeCluster(clusterSize)
    registerDoParallel(cluster)
    print(str_c("doParallel cores registered=", getDoParWorkers()), quote=FALSE)
    
    # Now I will sample destinations by truck type, as the utility parameters
    # and ideal distribution change for each.
    daily_trips <- data.table()    # Container to hold the completed records
    for (t in truck_types) {
        #for (t in c("TT", "DBL", "TPT")) {
        # Select utility parameters and ideal distributions for this truck type
        t_alphas <- alphas[alphas$truck_type==t,]
        t_ideal <- ideal[ideal$truck_type==t,]
        
        # We only need to copy the attractors for this trip purpose once, which
        # we will multiply by alpha1 to scale them
        t_zdata <- zdata[zdata$truck_type==t, ]
        t_zdata$attractors <- t_zdata$attractors*t_alphas$alpha1
        
        # Now let's find the destinations
        t_origins <- truck_origins[truck_origins$truck_type==t,]
        print(str_c("Selecting destinations for ", nrow(t_origins), " ", t,
            " trip origins"), quote=FALSE)
        t_origins$origin <- t_origins$Azone
        
        # Sample the destinations. This variant is slightly faster than breaking
        # the data into chunks and feeding them to foreach, as well as being to
        # simpler to understand.
        results <- foreach(i=t_origins$origin, .combine='c',
            .packages=c("data.table")) %dopar% { getDestination(i) }
        # Separately assign results to destination, as in some parallel setups
        # we might want to return a list instead, and will need to unlist that
        # before assigning the results to the destinations...
        t_origins$destination <- results
        
        # When finished with this truck type we will write t_origins to the trip
        # records, dropping fields that we are no longer interested in.
        daily_trips <- rbind(daily_trips, t_origins)
    }
    
    # Write the final results to disk
    FN <- str_c(RTP[["Working_Folder"]], "ct-internal-truck-trips.RData")
    print(str_c("Saving  ", nrow(daily_trips), "daily truck trips to ", FN), quote=FALSE)
    save(daily_trips, file=FN)
    if (RTP[["Extended_Trace"]]==TRUE) {
        FN <- str_c(RTP[["Working_Folder"]], "ct-daily-truck-trips.csv")
        write.table(daily_trips, file=FN, sep=',', row.names=FALSE, quote=FALSE)
    }
    
    # TO-DO: Shut down the cluster and exit stage left
    stopCluster(cluster)
    simulation.stop <- proc.time()
    elapsed_seconds <- round((simulation.stop-simulation.start)[["elapsed"]], 1)
    print(str_c("Simulation time=", elapsed_seconds, " seconds"), quote=FALSE)
}
 
TD <- sample_local_truck_destinations(630730)
 

 

 

 

