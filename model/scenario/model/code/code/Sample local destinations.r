# @tag{Run destination choice for CT synthetic firms at the alpha zone level}
# @author{Rick Donnelly} @version{0.9}  @date{04-Nov-2014}
library(data.table)
library(dplyr)
library(stringr)
library(reshape2)
library(parallel)

# Create a helper function to read matrix data from a text file in list format
# (i.e., the file is written by rows, then columns, with a single entry -- the
# value -- on each record. This is *MUCH* faster than reading data in i,j,value
# format with an arbitary number of zonal interchanges defined.
readStreamMatrix <- function(matrixFilename, numberOfZones, rowWise=TRUE) {
    # Read the stream data and calculated the dimensions
    raw <- scan(matrixFilename)
    calculatedNumberOfZones <- sqrt(length(raw))
    
    # If the calculated size of the matrix differs from what the user specified
    # then stop the simulation
    if (calculatedNumberOfZones!=numberOfZones) {
        stop(str_c("Number of zones passed to readStreamMatrix (", numberOfZones,
            " differs from calculated size of ", calculatedNumberOfZones))
    }
    
    # Read the stream into matrix format and return it to the calling function
    m <- matrix(raw, ncol=numberOfZones, byrow=rowWise)
    percent_zeros <- round(( length(raw[raw==0])/length(raw) )*100.0, 1)
    print(paste(matrixFilename, ": length=", length(raw), " total=",
        sum(raw, na.rm=TRUE), " mean=", round(mean(raw, na.rm=TRUE), 1),
        " zeros=", length(raw[raw==0]), " (", percent_zeros, "%)", sep=''),
        quote=FALSE)
    m
}

# IPF from ODOT's version of JEMnR (via Ben Stabler 16-Jun-2014). We will use it
# to generate an initial guess at trip length frequency distribution that we
# will sample from later on.
balance <- function(seed, rowcontrol, colcontrol, closure=0.005, maxiter=90,
    echo=FALSE) {
    # Check that sum of marginal totals equal and no zeros in marginal totals
    if (any(rowcontrol==0)) {
        numzero <- sum(rowcontrol==0)
        rowcontrol[rowcontrol==0] <- 0.001
    }
    if (any(colcontrol==0)) {
        numzero <- sum(colcontrol==0)
        colcontrol[colcontrol==0] <- 0.001
    }

    # Set initial values
    result <- seed
    rowcheck <- 1
    colcheck <- 1
    iter <- 0
    
    # Successively proportion rows and columns until closure or iteration
    # criteria are met
    while((rowcheck > closure) & (colcheck > closure) & (iter < maxiter)) {
        rowtotal <- rowSums(result)
        rowfactor <- rowcontrol/rowtotal
        rowfactor[is.infinite(rowfactor)]<-0
        result <- sweep(result, 1, rowfactor, "*")
        coltotal <- colSums(result)
        colfactor <- colcontrol/coltotal
        colfactor[is.infinite(colfactor)]<-0
        result <- sweep(result, 2, colfactor, "*")
        rowcheck <- sum(abs(1-rowfactor))
        colcheck <- sum(abs(1-colfactor))
        iter <- iter + 1
    }
    
    print(str_c("balance() total=", sum(rowcontrol), " iterations=", iter,
        " error=", max(rowcheck, colcheck)), quote=FALSE)
    result
}

sample_local_truck_destinations <- function(random_number_seed) {
    # Introduce yourself and set the random seed
    set.seed(random_number_seed)
    print(str_c("------- sample_local_truck_destinations -------"), quote=FALSE)
    print(str_c("Random seed=", random_number_seed), quote=FALSE)
    simulation.start <- proc.time()
    
    # Read the skim distance matrix, which we'll use as a seed for dest choice
    FN <- str_c(RTP[["Working_Folder"]], "pkautodist.txt")
    skim_matrix <- readStreamMatrix(FN, 5012)
    max_skim_distance <- max(skim_matrix)
    
    # Read the microsimulated origins, which we'll append the destinations to
    FN <- str_c(RTP[["Working_Folder"]], "ct-internal-truck-origins.RData")
    load(FN)
    highest_alpha_zone <- max(origins$Azone)
    
    # Read the idealized trip length frequency distribution, which we'll scale
    # to create a constraint matrix that will favor closer destinations than 
    # further ones.
    FN <- str_c(RTP[["CT_Core_Data"]], "ct-idealized-trip-length-distribution.csv")
    x <- fread(FN)
    # For distances beyond the edge of the idealized curve we will set the
    # probability to zero.
    ideal <- matrix(0, ncol=max_skim_distance)
    for (i in 1:nrow(x)) ideal[x$distance[i]] <- x$share[i]
    
    
    # Sample from destinations, where the weight of each location is the sum
    # of the trip origins (which equal destinations) times ideal location
    odpairs <- data.table()
    truck_types <- unique(origins$truck_type)
    for (t in truck_types) {
        # Grab the trip origins corresponding to this truck type
        these_results <- data.table()
        these_origins <- filter(origins, truck_type==t)
        print(str_c("Allocating ", nrow(these_origins), " ", t, " trip origins",
            " to destinations"), quote=FALSE)
        
        # Sum the number of trip destinations, which should be equal to origins
        w <- these_origins %>% group_by(Azone) %>% summarise(n = n())
        attractors <- matrix(0, ncol=highest_alpha_zone)
        for (i in 1:nrow(w)) attractors[w$Azone[i]] <- w$n[i]
        
        # Now set up a distribution of destination probabilities based upon the
        # ideal constraints.
        for (i in 1:highest_alpha_zone) {
            # Grab the trips from this origin
            this_origin <- filter(these_origins, Azone==i)
            if (nrow(this_origin)==0) next
            
            # Calculate the probability for each destination. We can do so by
            # pulling the skim for that origin and replacing its values by those
            # from the ideal distribution for each distance.
            constraints <- skim_matrix[i, 1:highest_alpha_zone]
            for (j in 1:highest_alpha_zone) {
                dx <- round(constraints[j], 0)
                constraints[j] <- ifelse(dx==0, 0, ideal[dx])
            }
            
            # Now weight the attractors by size of activity at the destination.
            # We need to remove zero probabilities before sampling, as sample()
            # crashes with them included instead of just ignoring them.
            zed <- constraints*attractors
            zeta <- data.table(Azone = 1:length(zed))
            zeta$pvalue <- zed[zeta$Azone]
            zeta <- filter(zeta, pvalue>0)  # Remove zero probabilities
            this_origin$destination <- sample(zeta$Azone, nrow(this_origin),
                prob=zeta$pvalue, replace=TRUE)
            this_origin <- rename(this_origin, origin = Azone)
            
            # Finally, add this set of OD pairs to the output
            these_results <- rbind(these_results, this_origin)
        }
        
        # Merge the intermediate results by truck type into final database
        odpairs <- rbind(odpairs, these_results)
    }
    
    # We're done
    simulation.stop <- proc.time()
    elapsed_seconds <- round((simulation.stop-simulation.start)[["elapsed"]], 1)
    print(str_c("Simulation time=", elapsed_seconds, " seconds"), quote=FALSE)
    
    # Finish by writing the combined database (for all truck types)
    FN <- str_c(RTP[["Working_Folder"]], "ct-internal-truck-trips.RData")
    save(odpairs, file=FN)
    print(str_c("Writing ", nrow(odpairs), " trips to ", FN))
    if (RTP[["Extended_Trace"]]==TRUE) {
        FN <- str_c(RTP[["Working_Folder"]], "ct-internal-truck-trips.csv")
        write.table(odpairs, file=FN, sep=',', quote=FALSE, row.names=FALSE)
    }
    print("", quote=FALSE)   # Add whitespace before next report
}

TD <- sample_local_truck_destinations(630730)