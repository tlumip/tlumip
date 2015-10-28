# This is the final step in allocation of the FAF to alpha zone "pseudo-firms"
# for use by CT. The allocation to external stations was modified in the
# previous version, while separate handling of domestic versus foreign trade
# flows are handled in this one. 
library(data.table)
library(dplyr)
library(stringr)

allocate_daily_FAF_to_firms <- function() {
    # Report in and start working
    set.seed(as.integer(RTP[["ct.random.seed"]]))
    print(str_c("------- allocate_daily_FAF_to_firms -------"), quote=FALSE)
    
    # LOAD THE INPUTS
    # Get the daily truck trips and add fields we will be filling
    load(str_c(RTP[["Working_Folder"]], "faf35-oregon-daily-trucks.RData"))
    # Also add synthetic firms
    load(str_c(RTP[["Working_Folder"]], "ct-alpha-synthetic-firms.RData"))
    oregon_faf_regions <- sort(unique(firms$faf_region))
    # Get the external station equivalencies for trucks
    FN <- str_c(RTP[["ct.properties.folder"]], "/ct-external-station-equivalencies.csv")
    externals <- fread(FN)
    
    # SEPARATE THE FLOWS INTO DOMESTIC VERSUS FOREIGN TRADE FLOWS
    # The allocation process for each works differently.
    domestic <- filter(combined, tradeType=="Domestic")
    foreign <- filter(combined, tradeType!="Domestic")
    
    # ASSOCIATE DISTAL FAF REGIONS WITH SWIM2 EXTERNAL STATIONS
    # In this case we associate each FAF region with a single external gateway
    # into or out of Oregon based on shortest time route defined by Google Maps.
    # Create a matrix where the subscript is the FAF region and its value is the
    # associated SWIM2 external station alpha zone.
    external_equiv <- matrix(NA, ncol=max(externals$fafregion))
    for (e in 1:nrow(externals)) {
        external_equiv[externals$fafregion[e]] <- externals$external_station[e]
    }
    getExternalStation <- function(region) external_equiv[region]
    
    # FLOW ALLOCATION BASED UPON ZONAL EMPLOYMENT
    # This variant is based upon "ct-inline-sampling-pattern", which is much
    # faster than explicitly sampling for each record as we've done up until now.
    oregonRegions <- c(as.integer(unlist(str_split(RTP[["ct.oregon.regions"]], ","))))
    simpleDomesticAllocation <- function(firms, truck_trips) {
        # Calculate total zonal employment
        ZE <- firms %>% group_by(Azone, faf_region) %>%
            summarise(Employees = sum(Employees, na.rm=TRUE))
        
        # Add the fields we want for the truck trip records
        truck_trips <- mutate(truck_trips, origin = NA, destination = NA)
        
        # Sample Oregon origins and destinations based upon total employment
        for (r in unique(ZE$faf_region)) {
            # Start by pulling only those alpha zones within this region (r)
            thisZE <- filter(ZE, faf_region==r)
            
            # Code origins within this region to alpha zones within it
            N <- nrow(truck_trips[truck_trips$dms_orig==r])
            origins <- sample(thisZE$Azone, N, prob=thisZE$Employees, replace=TRUE)
            truck_trips$origin[truck_trips$dms_orig==r] <- origins
            
            # Code destinations the same way
            N <- nrow(truck_trips[truck_trips$dms_dest==r])
            destinations <- sample(thisZE$Azone, N, prob=thisZE$Employees, replace=TRUE)
            truck_trips$destination[truck_trips$dms_dest==r] <- destinations
        }
        
        # Finally, handle the case where the domestic origin or destination is
        # outside of Oregon by coding it to a SWIM2 external station
        truck_trips$origin <- ifelse(is.na(truck_trips$origin), 
            getExternalStation(truck_trips$dms_orig), truck_trips$origin)
        truck_trips$destination <- ifelse(is.na(truck_trips$destination),
            getExternalStation(truck_trips$dms_dest), truck_trips$destination)
        # Return the finished list
        truck_trips
    }
    
    # ALLOCATION TO AND FROM OREGON PORTS
    # Foreign trade flows through Oregon are easier to handle, as they only move
    # through marine and air ports with known locations. Thus, the allocation
    # process is straight-forward and simple.
    portAllocation <- function(truck_trips) {
        # The input data table should have already been run through domestic
        # allocation, which will have appended the origin and destination fields.
        # Check that they exist before we get started, just in case the user
        # doesn't do that first.
        if(!"origin" %in% colnames(truck_trips)) {
            stop("Error: data table passed to portAllocation() is missing origin field")
        }
        
        # Handle inbound (import) traffic first
        truck_trips$origin[truck_trips$dms_orig==411 & truck_trips$frInmode=="Water"] <-
            as.integer(RTP[["ct.port.of.portland"]])
        truck_trips$origin[truck_trips$dms_orig==419 & truck_trips$frInmode=="Water"] <-
            as.integer(RTP[["ct.port.of.coos.bay"]])
        truck_trips$origin[truck_trips$dms_orig %in% RTP[["Oregon_Regions"]] &
            truck_trips$frInmode=="Air"] <- as.integer(RTP[["ct.pdx.airport"]])
        
        # Handle outbound (export traffic) the same way
        truck_trips$destination[truck_trips$dms_dest==411 & truck_trips$frOutmode=="Water"] <-
            as.integer(RTP[["ct.port.of.portland"]])
        truck_trips$destination[truck_trips$dms_dest==419 & truck_trips$frOutmode=="Water"] <-
            as.integer(RTP[["ct.port.of.coos.bay"]])
        truck_trips$destination[truck_trips$dms_dest %in% RTP[["Oregon_Regions"]] &
            truck_trips$frOutmode=="Air"] <- as.integer(RTP[["ct.pdx.airport"]])
        
        # Return the results
        truck_trips
    }
    
    # RUN THE ALLOCATION PROCESS
    # We are using the simple allocation in this case, which operates on the
    # truck trip list in place. We will start with domestic trips, which are
    # allocated to alpha zones based upon their share of employment in the
    # target sector within the FAF region.
    simulation.start <- proc.time()
    domestic_redux <- simpleDomesticAllocation(firms, domestic)
    
    # Since imports or exports could have domestic trip ends, including within
    # Oregon, we will code the domestic ends first. We'll then handle the case
    # where the traffic is going through an Oregon port.
    foreign_redux <- portAllocation(simpleDomesticAllocation(firms, foreign))
    
    # Combine the results
    long_distance_trips <- rbind(domestic_redux, foreign_redux)
    simulation.stop <- proc.time()
    elapsed_seconds <- round((simulation.stop-simulation.start)[["elapsed"]], 1)
    print(str_c("Simulation time=", elapsed_seconds, " seconds"), quote=FALSE)
    
    # Make sure that everyone is coded to alpha origin and destination by
    # checking for existence of missing values. If one or more are found then
    # stop the simulation.
    problem_children <- long_distance_trips[is.na(long_distance_trips$origin) |
        is.na(long_distance_trips$destination),]
    if (nrow(problem_children)>0) {
        FN <- str_c(RTP[["Working_Folder"]], "ct-unmatched-tripends.csv")
        write.table(problem_children, file=FN, row.names=FALSE, sep=',',
            quote=FALSE)
        stop(paste("Unmatched trips ends written to", FN))
    }
    
    # Save the final trip list, which should be ready for assignment. We can
    # easily convert it to matrix format if required, but at this point we don't
    # have time-of-day information associated with each trip (but could easily
    # synthesize it). We'll use the ct prefix to signify that the OD patterns
    # are now at the alpha zone or synthetic zone level (depending upon which
    # allocation function is used.)
    FN <- str_c(RTP[["Working_Folder"]], "ct-long-distance-trucks.RData")
    save(long_distance_trips, file=FN)
    print(str_c(nrow(long_distance_trips), " records written to ", FN), quote=FALSE)
    # If extended tracing mode is set in CT's runtime parameters write the same
    # data to CSV format as well.
    if (RTP[["ct.extended.trace"]]=="TRUE") {
        FN <- str_c(RTP[["Working_Folder"]], "ct-long-distance-trucks.csv")
        write.table(long_distance_trips, file=FN, quote=FALSE, sep=',',
            row.names=FALSE)
    }
    
    print("", quote=FALSE)   # Add whitespace between this and next report
}

DF <- allocate_daily_FAF_to_firms()
