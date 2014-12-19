# Convert FAF3 tons to trucks using Oak Ridge methdology in place of older 
# Oregon-specific factors
require(dplyr)
require(stringr)
require(data.table)
require(reshape2)
require(doParallel)

create_FAF_annual_truckloads <- function() {
    # Announce yourself and start. No random seed is passed, as everything here
    # is deteministic.
    print(str_c("------- create_FAF_annual_truckloads -------"), quote=FALSE)
	
	# Find the closest year in the data associated with the SWIM2 target year,
    # but only up to a certain point. If further away than allowed from one of
    # the FAF forecast years then stop and complain.
    getClosestYear <- function(this_year, years_in_data, max_difference=6) {
        # Find out how far away from the years in the FAF database this year is
        differences <- abs(years_in_data-this_year)
        closest_year <- which.min(differences)
        
        # If further apart than we can stomach then stop the simulation
        if (differences[closest_year]>max_difference) {
            stop(str_c("Error: the closest year in the FAF database to the ",
                "simulation year of ", this_year, " is ",
                years_in_data[closest_year]))
        } else {
            print(str_c("Simulation year of ", this_year, " mapped to FAF year ",
                years_in_data[closest_year]), quote=FALSE)
        }
        
        # Otherwise return the FAF year closest to the current year
        years_in_data[closest_year]
    }
    
    # DEFINE TRUCK ALLOCATION FACTORS
    # Read the truck allocation factors (Table 3-3) into memory and define a
    # helper function to extract values for the appropriate distance range.
    FN <- str_c(RTP[["ct.properties.folder"]], "/faf35-truck-allocation-factors.csv")
    truck_allocation_factors <- fread(FN)
    # Normalize the allocation factors by vehicle type within the chosen
    # distance on the fly
    getTruckAllocationFactors <- function(distance) {
        truck_allocation_factors %>%
            filter(distance>=minimum_range & distance<=maximum_range) %>%
            mutate(alloc_factor=allocation_factor/sum(allocation_factor)) %>%
            select(vehicle_type, alloc_factor)
    }
    
    # DEFINE TRUCK EQUIVALENCY FACTORS
    # Read the truck equivalency factors found in Appendix A of the FAF3 Freight
    # Traffic Analysis report, which we'll need to convert from wide to tall
    # format. The factors are used for converting kilotons into truckload
    # equivalents. Since we are dealing with tons we need to scale the factors
    # to account for the differences.
    FN <- str_c(RTP[["ct.properties.folder"]], "/faf35-truck-equivalency-factors.csv")
    raw <- fread(FN)
    truck_equivalency_factors <- melt(raw, id.vars=c("vehicle_type", "sctg2"),
        variable.name="body_type", value.name="equiv_factor") 
    # We will also define a helper function to pull the appropriate values by
    # commodity and vehicle type.
    getTruckEquivalencyFactors <- function(commodity) {
        filter(truck_equivalency_factors, sctg2==commodity, equiv_factor>0.0)
    }
    
    # DEFINE EMPTY TRUCK FACTORS
    # Finally, read empty truck factors from Table 3-5 and define a helper
    # function to grab the appropriate ones by body type and flow direction.
    # Again, convert from the original wide to tall format on the fly.
    FN <- str_c(RTP[["ct.properties.folder"]], "/faf35-empty-truck-factors.csv")
    raw <- fread(FN)
    empty_truck_factors <- melt(raw, id.vars=c("body_type", "crossing_type"),
        variable.name="vehicle_type", value.name="empty_factor")
    getEmptyTruckFactors <- function(flow_direction) {
        filter(empty_truck_factors, crossing_type==flow_direction)
    }
    
    # FUNCTION TO CALCULATE TRUCK EQUIVALENCIES
    # Apply this function to each FAF flow record by passing it to the function.
    # The resulting data table will have flows by each vehicle (truck) type and
    # status (loaded, empty).
    calcTruckloadEquivalencies <- function(x) {
        # Grab record x from the data frame we are working on
        replicant <- flows[x,]
        
        # Get the factors appropriate for the current distance between domestic
        # FAF regions, trade type, and commodity
        taf <- getTruckAllocationFactors(replicant$distance)
        tef <- getTruckEquivalencyFactors(replicant$sctg2)
        trip_type <- ifelse(replicant$tradeType=="Domestic", "domestic", "border")
        etf <- getEmptyTruckFactors(trip_type)
        
        # Allocate the tonnage to each vehicle (as in Table 3-7)
        taf$tons <- replicant$tons*taf$alloc_factor
        
        # Merge the tonnage by vehicle type with the truck equivalency factors
        # to get tonnage by vehicle type and body type. 
        loaded <- merge(tef, taf, by="vehicle_type", all.x=TRUE) %>%
            mutate(annual_trucks = tons*equiv_factor) %>%
            select(-equiv_factor, -alloc_factor, -tons)
        # It is possible that we wind up with less than one truck using the FAF
        # factors, which we will always round up to one truck. If that happens
        # pick the vehicle and body type that has the highest number of them.
        zed <- sum(loaded$annual_trucks)
        if (zed<1.0) {
            loaded <- loaded[which.max(loaded$annual_trucks)] %>%
                mutate(annual_trucks=1.0)
        }
        
        # Calculate the number of empty trucks. If we obtain zero trucks we will
        # accept that.
        empty <- merge(loaded, etf, by=c("body_type", "vehicle_type")) %>%
            mutate(empty_trucks=annual_trucks*empty_factor) %>%
            select(-crossing_type, -empty_factor)
        
        # We needed to retain the body types to calculate empties, but now we
        # can collapse each group to vehicle types
        loaded <- loaded %>% group_by(vehicle_type) %>%
            summarise(annual_trucks=round(sum(annual_trucks),0)) %>%
            mutate(status="Loaded")
        empty <- empty %>% group_by(vehicle_type) %>%
            summarise(annual_trucks=round(sum(empty_trucks),0)) %>%
            mutate(status="Empty")
        
        # dplyr's mutate cannot access variables outside of the table passed to
        # it, so append the original value and tons for it to operate upon
        loaded$tons <- replicant$tons
        loaded$value <- replicant$value
        
        # Create a separate record for the combined groups and merge with the
        # data from the flow record. Note that we drop the original value and
        # tons, as they are now split among the various loaded truck types.
        loaded <- loaded %>% mutate(percent = annual_trucks/sum(annual_trucks),
            tons = percent*tons, value = percent*value) %>% select(-percent)
        empty <- mutate(empty, tons=0.0, value=0.0)
        together <- rbind(loaded, empty) %>% filter(annual_trucks>0)
        # Add the SCTG code back in, as we will use that to merge the two tables
        together$sctg2 <- replicant$sctg2
        result <- merge(select(replicant, -tons, -value), together, by="sctg2")
        result$zed <- zed
        result
    }
    
    # RUN THE ALLOCATION
    # Read the FAF data and run it through the simulation
    faf35_data <- str_c(RTP[["ct.properties.folder"]], "/faf35-oregon.csv")
    raw <- fread(faf35_data)
	# Associate the current year in the simulation to the closest FAF year
    faf_years <- sort(as.integer(unique(raw$year)))
    use_faf_year <- getClosestYear(strtoi(RTP[["base.year"]])+strtoi(RTP[["t.year"]]), faf_years)
    # Keep only non-zero truck flows
    flows <- filter(raw, dmsMode=="Truck", tons>0.0, year==use_faf_year)
    
    # Run the simulation
    simulation.start <- proc.time()
    numberOfCores <- detectCores()
    print(paste("Number of cores detected=", numberOfCores), quote=FALSE)
    cluster <- makeCluster(numberOfCores)
    registerDoParallel(cluster)
    combined <- foreach(i=1:nrow(flows), .combine="rbind",
        .packages=c("data.table", "dplyr")) %dopar% { calcTruckloadEquivalencies(i) }
    stopCluster(cluster)
    simulation.stop <- proc.time()
    elapsed_seconds <- round((simulation.stop-simulation.start)[["elapsed"]], 1)
    print(paste("Simulation time=", elapsed_seconds, "seconds"), quote=FALSE)
    
    # Save the results for later processing. Save to binary by default, but also
    # save to CSV if extended tracing is set in CT's runtime parameters.
    FN <- str_c(RTP[["Working_Folder"]], "faf35-oregon-annual-trucks.RData")
    save(combined, file=FN)
    print(str_c(nrow(combined), " flow records written to ", FN), quote=FALSE)
    if (RTP[["ct.extended.trace"]]=="TRUE") {
        FN <- str_c(RTP[["Working_Folder"]], "faf35-oregon-annual-trucks.csv")
        write.table(combined, file=FN, sep=',', quote=FALSE, row.names=FALSE)
    }
    
    print("", quote=FALSE)  # Add whitespace after this and before next report
}

AF <- create_FAF_annual_truckloads()