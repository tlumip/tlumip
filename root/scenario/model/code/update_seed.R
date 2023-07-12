# Update SPG seed
library(tidyverse);

args <- commandArgs(trailingOnly=FALSE)
print("Hello World")
if (length(args) < 8){
	stop(str_c("Error: SWIM properties file not passed to the update seed script"))
} else {
	swim_property_file <- substr(args[8],2,nchar(args[8]))
	print(paste("Property file -", swim_property_file))
}

# Carry out the seed update here

quit(save = "no")

