# Run the CT3E program

# Start by loading the dependencies. Some of the scripts written to assess
# intermediate and final outputs also use ggplot2, but it is not required in
# order to run the model.
library(dplyr)
library(data.table)
library(stringr)
library(reshape2)
library(doParallel)

# TO-DO: Get the run year and folder

# Grab the CT runtime parameters from the working directory. CT will not work
# without the parameters, but R will crash if it cannot find them in any case.
#@ MANUALLY SET THE LOCATION OF THE RUNTIME PARAMETERS
load("/Users/rick/Models/swim2/ctdev/outputs/t20/ct-runtime-parameters.RData")

# TO-DO: Start the logger

# Write run header
si <- Sys.info()
header <- paste("CTx3 ran on", si["nodename"], "by", si["user"], "on", date())
print(header, quote=FALSE)

# Run the commodity flow portion of the model
#@ library(CT3E)
LIB <- RTP[["Code_Depository"]]
source(str_c(LIB, "Create truckload equivalencies2.r"))
source(str_c(LIB, "Sample weekly trucks2.r"))
source(str_c(LIB, "Allocate to alpha zones4.r"))

# Run the truck tour microsimulation part of the model
source(str_c(LIB, "Create synthetic firms.r"))
source(str_c(LIB, "Trip generation4.r"))
source(str_c(LIB, "Sample local destinations.r"))