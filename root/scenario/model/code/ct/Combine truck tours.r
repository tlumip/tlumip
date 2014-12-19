# Combine the local and long-distance trucks into a single truck tour list for 
# use in assignment
# @author{Rick Donnelly} @version{0.1} @date{30-Nov-2014}
require(data.table)
require(dplyr)
require(stringr)

combine_truck_tour_lists <- function() {
    print(str_c("------- combine_truck_tour_lists -------"), quote=FALSE)
    
    # Create the final truck tour list that we'll write at the end
    combined_tours <- data.table()
    
    # Read the local truck trips, strip out the fields we don't need, and rename
    # the fields if necessary
    load(str_c(RTP[["Working_Folder"]], "ct-internal-truck-trips.RData"))
    daily_trips <- daily_trips %>%
        select(-firmID, -Industry, -Employees, -faf_region) %>%
        rename(origin = Azone, tripStartTime = dep_time, truckType = truck_type) %>%
        mutate(status = "Loaded", value = NA, tons = NA, sctg2 = 99)
    
    # Do the same thing for long-distance trucks
    load(str_c(RTP[["Working_Folder"]], "ct-long-distance-trucks.RData"))
    long_distance_trips <- long_distance_trips %>%
        select(sctg2, vehicle_type, status, value, tons, origin, destination, dep_time) %>%
        rename(truckType = vehicle_type, tripStartTime = dep_time)
    
    # Write the combined trips to the final truck tour list
    combined <- rbind(daily_trips, long_distance_trips)
    FN <- RTP[["ct.truck.trips"]]
	combined$tourMode = rep(9, length(combined$origin))
	combined$tripMode = rep(9, length(combined$origin))
	combined$truckID = seq(1, length(combined$origin), 1)
	
	
	#order matters to SL
	options(scipen=10)
    write.table(data.frame(combined)[,c(2,4,3,9,10,11,1)], file=FN, sep=",", quote=FALSE, row.names=FALSE)
    print(str_c(nrow(combined), " combined truck tours written to ", FN), quote=FALSE)
}

CT <- combine_truck_tour_lists()