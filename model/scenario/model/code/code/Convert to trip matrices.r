# @tag{Convert trip lists to trip matrices in CSV format}
# @author{Rick Donnelly} @version{0.9} @date{17-Nov-2014}
# Create daily OD trip matrices in CSV format for reading into trip matrices.
# Use the existing script for conversion to Inro D311 format if that is desired
# instead.
# TO-DO: Huddle with Ben to decide on best format for him to handle these data
require(data.table)
require(dplyr)
require(stringr)

convert_trip_list_to_matrices <- function() {
    # Introduce yourself
    print(str_c("------- convert_trip_list_to_matrices -------"), quote=FALSE)
    
    # Read the local truck trips and build a daily trip matrix in list format
    load(str_c(RTP[["Working_Folder"]], "ct-internal-truck-trips.RData"))
    internal <- select(odpairs, truck_type, origin, destination)
    
    # Repeat the same process for long-distance trucks
    load(str_c(RTP[["Working_Folder"]], "ct-long-distance-trucks.RData"))
    external <- select(long_distance_trips, truck_type, origin, destination)
    
    # Merge them together into a single trip list, and then convert to i,j,trips
    # format
    combined <- rbind(internal, external) %>%
        group_by(truck_type, origin, destination) %>%
        summarise(trips = n())
    
    # Write each trip matrix to file in i,j,value format
    truck_types <- unique(combined$truck_type)
    for (t in truck_types) {
        FN <- str_c(RTP[["Working_Folder"]], "ct-", t, "-trip-matrix.csv")
        this_matrix <- filter(combined, truck_type==t)
        this_matrix$truck_type <- NULL   # select(-truck_type) doesn't work?
        write.table(this_matrix, file=FN, sep=',', quote=FALSE, row.names=FALSE)
        print(str_c("Writing ", nrow(this_matrix), " records to ", FN), quote=FALSE)
    }    
    print("", quote=FALSE) # Add whitespace before following report
}

CM <- convert_trip_list_to_matrices()