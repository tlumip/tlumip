# @tag{doParallel+foreach matrix solution for CT destination choice}
# @author{Rick Donnelly} @version{0.8}  @date{24-Nov-2014}
# This variant of destination choice uses matrix manipulations to get at the 
# answer, which will hopefully be substantially quicker solution.
library(data.table)
library(dplyr)
library(stringr)

sample_local_truck_destinations <- function() {
    # Announce yourself
    set.seed(as.integer(RTP[["ct.random.seed"]]))
    print(str_c("------- sample_local_truck_destinations -------"), quote=FALSE)
    simulation.start <- proc.time()
    
    # READ THE SKIM MATRIX
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
    
    # GET REQUIRED TRIP DATA AND MODEL PROPERTIES
    # First load the truck origins and sum them by alpha zone and truck type.
    # Since we are assuming symmetry of flows these become our attractors for
    # destination choice.
    FN <- str_c(RTP[["Working_Folder"]], "ct-internal-truck-origins.RData")
    load(FN)
    truck_types <- unique(truck_origins$truck_type)
    
    # Check to make sure that we don't have trips originating from alpha zones
    # that are not in the skim matrix
    CTO <- sort(unique(truck_origins$Azone))
    problem_children <- list()
    for (c in CTO) {
        if (!c %in% dzones) problem_children <- c(problem_children, c)
    }
    if (length(problem_children>0)) stop(paste("Alpha zones in trip list",
        "with no corresponding skim:", problem_children))
    
    # Sum the attractors by alpha zone. We will need to have data defined for 
    # each of the zones defined in the skim matrix, even if some have zero truck
    # trips associated with it. Thus, we'll substitute any missing values with
    # zeros.
    attractors <- truck_origins %>% group_by(Azone, truck_type) %>%
        summarise(attractors = n())
    attractors$destination <- attractors$Azone  # Bug in dplyr's rename() 

    # Read the utility parameters by truck type
    FN <- str_c(RTP[["ct.properties.folder"]], "/ct-destination-utility-parameters.csv")
    alphas <- fread(FN)
    
    # Read the ideal trip length probabilities by truck type. These data are in
    # wide format, and we need to first append rows for distances in the skim
    # matrix but not included in the ideal distribution data. Then we'll convert
    # it to tall format to make process it easier.
    FN <- str_c(RTP[["ct.properties.folder"]], "/ct-idealized-trip-length-distribution.csv")
    ideal <- fread(FN)
    max_skim_distance <- round(max(skim_distances), 0)
    # Add 1 because we'll use offset referencing because R doesn't have zero-
    # based matrix indexing
    x <- data.table(distance = 0:max_skim_distance+1)
    ideal <- merge(ideal, x, by="distance", all.y=TRUE)
    ideal[is.na(ideal)] <- 0.0   # Replace missing values with zeros
    
    # RUN THE MODEL
    # The ideal distributions and alpha parameters differ by truck type, so we
    # will handle each one differently. At the end of handling each truck type
    # we will add those results to a final data table that will have OD flows.
    daily_trips <- data.table()   # Container to store the final results
    for (t in truck_types) {
        print(str_c("Sampling destinations for ", nrow(filter(truck_origins,
            truck_type==t)), " ", t, " origins"), quote=FALSE)
        # We first need to multiply the skim matrix by ideal propbabilities,
        # which is harder in R than it should be. We'll convert the matrix into
        # vector format, morph skims into probabilities, and put it back into a
        # matrix format.
        t_ideal <- ideal[[t]]
        X <- round(as.vector(skim_distances), 0)+1
        # Convert to ideal probabilities and optionally scale them
        X <- t_ideal[X]
        X <- X*alphas$alpha1[alphas$truck_type==t]
        # Finally, write the results back to matrix format so that we can 
        # multiply them by the attractors, which of course varies by zone
        t_skims <- matrix(X, nrow=length(dzones), ncol=length(dzones),
            byrow=FALSE, dimnames=list(dzones, dzones))
        
        # Next construct a matrix of attractors. Since not all zones have
        # attractors we'll need to include missing destination zones and set 
        # their attractors to zero.
        A <- merge(data.table(destination = dzones),
            filter(attractors, truck_type==t), by="destination", all.x=TRUE)
        A$attractors[is.na(A$attractors)] <- 0
        # Apply the scaling factor
        A$attractors <- A$attractors*alphas$alpha2[alphas$truck_type==t]
        
        # Calculate the weighted probabilities. A nifty feature of sample() is
        # that it is too lame to ignore zero values, so when it finds instances
        # of zero probabilities it throws an error. So we'll have to replace
        # zeroes with really low probabilities.        
        W <- sweep(t_skims, 2, as.vector(A$attractors), '*')
        W[W==0] <- 1e-9
        
        # Finally, now sample the destinations for the origins. Start by summing
        # total origins by zone, which will be the number of samples we will 
        # draw for destinations.
        t_origins <- truck_origins %>% filter(truck_type==t) %>% 
            group_by(Azone) %>% summarise(total_origins = n())
        t_origins <- rename(t_origins, origin = Azone)
        results <- lapply(1:nrow(t_origins), 
            function(i) {
                this_origin <- t_origins$origin[i]
                these_trips <- t_origins$total_origins[i]
                sample(dzones, these_trips, prob=W[as.character(this_origin),], replace=TRUE)
            }
        )
                
        # Now we just need to append the destinations to the t_origins records
        # and then merge in the final dataset. No one can explain why the
        # following code doesn't work, so we follow the rich R tradition of
        # doing things the hard and slower way instead.
        #for (i in nonzero_origins) {
        #   trip_list$destination[trip_list$origin==i,] <- unlist(results[[i]])
        #}
        merged_results <- data.table()
        for (i in 1:nrow(t_origins)) {
            this_origin <- t_origins$origin[i]
            ti <- filter(truck_origins, Azone==this_origin, truck_type==t)
            ti$destination <- results[[i]]
            merged_results <- rbind(merged_results, ti)
        }
        # Maybe something I could do differently would be to use a different
        # looping variable, where instead I might say:
        #    for (ozone in 1:nrow(t_origins$origin)) { ... }
                
        daily_trips <- rbind(daily_trips, merged_results)         
    }
        
    # Write the final results to disk
    FN <- str_c(RTP[["Working_Folder"]], "ct-internal-truck-trips.RData")
    print(str_c("Saving ", nrow(daily_trips), " daily truck trips to ", FN), quote=FALSE)
    save(daily_trips, file=FN)
    if (RTP[["ct.extended.trace"]]=="TRUE") {
        FN <- str_c(RTP[["Working_Folder"]], "ct-daily-truck-trips.csv")
        write.table(daily_trips, file=FN, sep=',', row.names=FALSE, quote=FALSE)
    }
    
    # Shut down
    simulation.stop <- proc.time()
    elapsed_seconds <- round((simulation.stop-simulation.start)[["elapsed"]], 1)
    print(str_c("Simulation time=", elapsed_seconds, " seconds"), quote=FALSE)
}
 
TD <- sample_local_truck_destinations()
 

 

 

 

