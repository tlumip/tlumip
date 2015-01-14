# Run the CT model
library(dplyr)
library(data.table)
library(stringr)
library(reshape2)
library(doParallel)


###DEBUG RUN FROM CMD###
#C:/projects/development/projects/tlumip/model/base_scenario/model/lib/R-3.1.2/bin/x64/R.exe CMD BATCH -C:/projects/development/projects/tlumip/model/base_scenario/model/code/ct -C:/projects/development/projects/tlumip/model/base_scenario/outputs/t20/ct.properties -20 C:/projects/development/projects/tlumip/model/base_scenario/model/code/ct/CTx4.R C:/projects/development/projects/tlumip/model/base_scenario/outputs/t20/zzCT.Rout
#########################

# READ AND PROCESS RUNTIME PROPERTIES
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
    swim_properties_FN <- substr(args[9],2,nchar(args[9]))
	print(swim_properties_FN)
    ct_code_folder <- substr(args[8],2,nchar(args[8]))
	print(ct_code_folder)
}

# If the user didn't include a trailing slash in the CT code folder path add one
n <- nchar(ct_code_folder)
if (substr(ct_code_folder, nchar(n), nchar(n))!="/") 
    ct_code_folder <- str_c(ct_code_folder, "/")

# Now that we know where they are let's set up the properties
source(str_c(ct_code_folder, "Get runtime properties.r"))
RTP <- set_CT_runtime_parameters(swim_properties_FN)


# RUN THE MODELS SEQUENTIALLY
# Once the model is accepted we'll move the various modules into a R package and
# pass the resulting data tables from each as return values, obviating the need
# to store intermediate files unless the user wants access to them (in which
# case we'll leave the CSV versions behind). But while still in dev mode we will
# run each module as a separate R script, given how often I'm swapping out 
# different variants.

# Write run header
si <- Sys.info()
header <- paste("CT ran on", si["nodename"], "by", si["user"], "on", date())
print(header, quote=FALSE)

# Create synthetic firms
source(str_c(ct_code_folder, "Create synthetic firms3.r"))

# Run the commodity flow portion of the model
source(str_c(ct_code_folder, "Create truckload equivalencies4.r"))
source(str_c(ct_code_folder, "Sample weekly trucks3.r"))
source(str_c(ct_code_folder, "Allocate to alpha zones5.r"))

# Run the truck tour microsimulation part of the model
source(str_c(ct_code_folder, "Trip generation5.r"))
source(str_c(ct_code_folder, "Sample local destinations8.r"))
source(str_c(ct_code_folder, "Temporal allocation2.r"))
source(str_c(ct_code_folder, "Combine truck tours.r"))

#Run ET (or placeholder)
source(str_c(ct_code_folder, "ET_placeholder.r"))

#To save the .Rdata file to the directory for the simulation year
#Comment this out if working in Rgui mode
setwd(RTP[["Working_Folder"]]) 
