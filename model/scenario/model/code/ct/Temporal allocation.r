# Carry out temporal allocation for both intercity and local truck trips
# @author{Rick Donnelly} @version{0.9} @date{18-Oct-2014}
require(data.table)
require(dplyr)
require(stringr)

truck_trip_temporal_allocation <- function(random_number_seed) {
    # Introduce yourself and set the random seed
    set.seed(random_number_seed)
    print(str_c("------- truck_trip_temporal_allocation -------"), quote=FALSE)
    print(str_c("Random seed=", random_number_seed), quote=FALSE)
    
    # GET TEMPORAL ALLOCATION FACTORS
    # Read the temporal allocation factors, which denote the hour the truck
    # will start its trip. Factors are defined for each truck type in the
    # simulation.
    FN <- str_c(RTP[["CT_Core_Data"]], "ct-truck-temporal-distributions.csv")
    temporal <- fread(FN)
    
    # PROCESS LOCAL TRUCKS
    # Append the starting time to each local truck record for truck type. Start
    # by reading the truck data.
    FN <- str_c(RTP[["Working_Folder"]], "ct-internal-truck-trips.RData")
    load(FN)
    truck_types <- unique(odpairs$truck_type)
    odpairs$dep_time <- NA
    
    # Process each truck type in turn
    for (t in truck_types) {
        N <- nrow(odpairs[odpairs$truck_type==t])
        print(str_c("Sampling departure times for ", N, " local ", t, " trucks"),
            quote=FALSE)
        hour <- sample(temporal$hour[temporal$truck_type==t], N,
            prob=temporal$share[temporal$truck_type==t], replace=TRUE)
        minute <- sample(0:59, N, replace=TRUE)
        odpairs$dep_time[odpairs$truck_type==t] <- (hour*100)+minute        
    }
    
    # Replace the existing data with these
    save(odpairs, file=FN)
    print(str_c("Writing ", nrow(odpairs), " records to ", FN), quote=FALSE)
    
    # PROCESS LONG-DISTANCE TRIPS
    # We will use the same process for the long-distance trucks, which should 
    # be classified using the same truck types. Start by reading the data and 
    # repeating the same process above.
    FN <- str_c(RTP[["Working_Folder"]], "ct-long-distance-trucks.RData")
    load(FN)
    truck_types <- unique(long_distance_trips$truck_type)
    long_distance_trips$dep_time <- NA
    
    # Process each truck type in turn
    for (t in truck_types) {
        N <- nrow(long_distance_trips[long_distance_trips$truck_type==t])
        print(str_c("Sampling departure times for ", N, " long-distance ", t,
            " trucks"), quote=FALSE)
        hour <- sample(temporal$hour[temporal$truck_type==t], N,
            prob=temporal$share[temporal$truck_type==t], replace=TRUE)
        minute <- sample(0:59, N, replace=TRUE)
        long_distance_trips$dep_time[long_distance_trips$truck_type==t] <-
            (hour*100)+minute        
    }
    
    # Replace the existing data with these
    save(long_distance_trips, file=FN)
    print(str_c("Writing ", nrow(long_distance_trips), " records to ", FN),
        quote=FALSE)
    
    print("", quote=FALSE) # Add whitespace before following report
}

TA <- truck_trip_temporal_allocation(630730)