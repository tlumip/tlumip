# Run the CT model
library(tidyverse); library(doParallel); library(swimctr)

# [1] MODEL SETUP =================
# Set up the runtime environment and populate it with data from user-supplied
# parameter file(s)

run_start <- proc.time()   # Start the timer for the overall simulation

# We will inherit the systemwide properties set for this SWIM run, the filename
# for which must be supplied as the first command-line parameter and folder
# containing the CT scripts as the second parameter. From the former we will
# extract and store the properties that CT will maintain as global variables
# throughout the simulation.
args <- commandArgs(trailingOnly=FALSE)

if (length(args)==0) {
	stop(str_c("Error: SWIM properties filename and CT ", "folders must be specified when running CT"))
} else {
    # Assume that the user has passed valid filenames
    # TO-DO: Add code that checks for existence of these files?
    if (length(args) < 7) stop(str_c("Error: SWIM properties filename and CT ",
        "folders must be specified when running CT"))
    swim_properties_FN <- substr(args[8],2,nchar(args[8]))
	print(swim_properties_FN)
    ct_code_folder <- substr(args[7],2,nchar(args[7]))
	print(ct_code_folder)
}

# If the user didn't include a trailing slash in the CT code folder path add one
n <- nchar(ct_code_folder)
if (substr(ct_code_folder, nchar(n), nchar(n))!="/") 
    ct_code_folder <- str_c(ct_code_folder, "/")
    
# We do not require two parameter files, but we will use that many so that we
# can separate static from dynamic parameters, which is how things will likely
# work when running SWIM
swimctr::get_runtime_parameters("tests/t0/default.properties")
swimctr::get_runtime_parameters("tests/t5/t5.properties")

# Now that we've set the runtime parameters we can finish up with calls that use
# them
setwd(RTP[["scenario.outputs"]])
sink(file = RTP[["ct.logfile"]], append = FALSE, split = TRUE)
if (!exists(RTP[["ct.cluster.logfile"]])) {
  RTP[["ct.cluster.logfile"]] <- ""  # Since several functions depend upon it
}

# Import make and use coefficients from PECAS and morph into format we can use
makeuse <- swimctr::create_makeuse_coefficients(RTP[["pecas.makeuse"]],
  RTP[["faf.flow.data"]])

# [2] RUN FIRM SYNTHESIS =================
# Run the synthesis with employment data from the current simulation year, which
# creates pseudo-firms that are sum of employment within each sector in each
# alpha zone
firms <- swimctr::create_synthetic_firms(RTP[["pecas.zonal.employment"]],
  RTP[["ct.sector.equivalencies"]], RTP[["alpha2beta.file"]])

# [3] PROCESS LOCAL TRUCK TRIPS ================
# Simulate local truck trips (those with both ends within the modeled area, to
# include the halo), using typical sequence of model components. Start by
# generating the trucks and their attributes.
daily_local_origins <- swimctr::local_truck_generation(firms,
  RTP[["ct.generation.probabilities"]])

# We will need to read skim matrices for the alpha zone system, and then run our
# destination choice model
skim_matrices <- swimctr::read_skim_matrices(RTP[["ct.distance.skims"]],
  RTP[["ct.travel.time.skims"]])
daily_local_trips <- swimctr::sample_local_truck_destinations(
  daily_local_origins, skim_matrices, RTP[["ct.trip.length.targets"]],
  RTP[["ct.destination.utilities"]])

# Finally, append a departure time for each local trip
hourly_local_trips <- swimctr::temporal_allocation(daily_local_trips,
  RTP[["ct.temporal.factors"]])

# [4] PROCESS INTER-REGIONAL TRUCK TRIPS ============
# Start by creating annual truckload equivalencies from the FAF data, which
# depends upon a large number of parameter tables from the FAF Freight Traffic
# Analysis report.
annual_faf_trucks <- swimctr::create_annual_faf_truckloads(
	RTP[["faf.flow.data"]], RTP[["faf.truck.allocation.factors"]], 
	RTP[["faf.truck.equivalency.factors"]], RTP[["faf.empty.truck.factors"]], 
	as.integer(RTP[["t.year"]]))

# Next we sample daily trucks, which is accomplished by first reducing annual to
# weekly flows, and then sampling a day of the week for each discrete truck.
daily_faf_trucks <- swimctr::sample_daily_faf_truckloads(annual_faf_trucks,
  external_scaling_factor = as.numeric(RTP[["faf.external.scaling.factor"]]))

# Allocate them to zones within the modeled area
allocated_faf_trips <- swimctr::allocate_faf_to_zones(daily_faf_trucks, firms,
  makeuse, RTP[["ct.intermodal.connectors"]], RTP[["ct.external.gateways"]])

# Read the temporal allocation factors and apply them to the daily FAF trips
hourly_faf_trips <- swimctr::temporal_allocation(allocated_faf_trips,
  RTP[["ct.temporal.factors"]])

# [5] EXPORT TRUCK TRIP LIST
# Write a combined trip list with all of the attributes that Ben had formerly
# exported, as well as a few new ones (including distance and travel time for
# each trip)
swimctr::export_list_list(hourly_faf_trips, hourly_local_trips,
  "ct_exported_trips.csv")

# Exit stage left
run_stop <- proc.time()
elapsed_seconds <- round((run_stop-run_start)[["elapsed"]], 1)
print(paste("Total model run time=", elapsed_seconds, "seconds"), quote=FALSE)
sink()

#To save the .Rdata file to the directory for the simulation year
#Comment this out if working in Rgui mode
setwd(RTP[["Working_Folder"]]) 