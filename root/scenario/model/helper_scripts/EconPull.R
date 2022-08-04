# EconPull.R
# Alex Bettinardi
# 2-11-22 - first draft (actually a second draft attempt at a simplification - original work was completed under the seismic analysis ~2013)
# 8-4-22 - re-developed / designed

 ##############################################################
# A script to compare econoimc activity data to a reference 
# User inputs regions - as specified with an inputfile - "BetaZoneAggregation.csv"
# This script produces one output:
#  - GDP_RegionalResults.csv - a summary of change in GDP
################################################################

# First Define the Reference scenario to compare all other scenarios to.
#####################################################################
Ref <- "//xd6420thing3/d/swim2/Ref25_ClosureXTest/outputs/Ref25_ClosureXTest.db"

# Second define scenarios to be pulled and compared
# build a list to the various completed SWIM databases to be compared to the reference
#####################################################################
dbs <- c("//xd6420thing1/d/swim2/Ref25_Closure12/outputs/Ref25_Closure12.db",
         "//xd6420thing3/d/swim2/Ref25_Closure19/outputs/Ref25_Closure19.db","//xd6420thing1/d/swim2/Ref25_Closure26/outputs/Ref25_Closure26.db",
         "//xd6420thing3/d/swim2/Ref25_Closure40/outputs/Ref25_Closure40.db","//xd6420thing3/d/swim2/Ref25_Closure48/outputs/Ref25_Closure48.db",
         "//xd6420thing3/d/swim2/Ref25_ClosureUS101/outputs/Ref25_ClosureUS101.db","//xd6420thing2/d/swim2/Ref25_ClosureUS101_20/outputs/Ref25_ClosureUS101_20.db")

# Give each SWIM database pointer a short and meaningfull name 
##############################################################
names(dbs) <- c("Closure12","Closure19","Closure26","Closure40","Closure48","ClosureUS101","ClosureUS101_20Yr") 

# What Years Should GDP be totalled:
####################################
Yrs <- "All" # Ex. 2023:2032

# load in pre-defined beta zone list - BetaZoneAggregation.csv
##############################################################
bzCW <- read.csv("BetaZoneAggregation.csv", as.is=T)
rownames(bzCW) <- bzCW$BETA
# Which fields in the Beta Zone listing need to be aggregated
AggFields <- c("RRA","Agg","StAgg","COUNTY")
# If any of the cells (zones) in the AggFields need to be ignored, use the following "Ignore" identifier:
Ignore <- "Ignore"

##########################################
# Just code now, no more user inputs below
##########################################

# setup connection to SQLite
library(RSQLite)
m = dbDriver("SQLite")

###########################################
# Pull the data from the reference  
###################################

az.. = dbGetQuery(dbConnect(m, Ref), "SELECT AZONE, POPULATION, EMPLOYMENT, TOTALHHS, TSTEP FROM AZONE")
az..$POPULATION <- as.numeric(az..$POPULATION)
az..$Sc <- "Reference"
az..$TSTEP <- as.numeric(az..$TSTEP)+1990

bz.. <- dbGetQuery(dbConnect(m, Ref), "SELECT * FROM ActivityLocations")
bz..$TSTEP <- as.numeric(bz..$TSTEP)+1990
act.BzYr <- tapply(bz..$QUANTITY, list(bz..$BZONE, bz..$TSTEP), sum)

act.. <- as.data.frame(cbind(as.vector(act.BzYr), expand.grid(dimnames(act.BzYr), stringsAsFactors = F)), stringsAsFactors=F)
names(act..) <- c("Act", "Bz", "Yr")
act..$Sc <- "Reference"

cw = dbGetQuery(dbConnect(m, dbs[1]), "SELECT * FROM ALLZONES")
rownames(cw) <- cw$Azone

# run a for loop to append data for all scenarios
for(dbName in names(dbs)){

   # connect to the database
   DB = dbConnect(m, dbs[dbName])
   # Pull Azone data
   Azone.. = dbGetQuery(DB, "SELECT AZONE, POPULATION, EMPLOYMENT, TOTALHHS, TSTEP FROM AZONE")
   Azone..$TSTEP <- as.numeric(Azone..$TSTEP)+1990
   Azone..$POPULATION <- as.numeric(Azone..$POPULATION)
   Azone..$Sc <- dbName
   
   # add data to all scenario table
   az.. <- rbind(az.., Azone..)
   
   # Pull Activity data
   bz.. <- dbGetQuery(DB, "SELECT * FROM ActivityLocations")
   bz..$TSTEP <- as.numeric(bz..$TSTEP)+1990
   act.BzYr <- tapply(bz..$QUANTITY, list(bz..$BZONE, bz..$TSTEP), sum)
   bz.. <- as.data.frame(cbind(as.vector(act.BzYr), expand.grid(dimnames(act.BzYr), stringsAsFactors = F)), stringsAsFactors=F)
   names(bz..) <- c("Act", "Bz", "Yr")
   bz..$Sc <- dbName

   # add data to all scenario table
   act.. <- rbind(act.., bz..)

   # close connection to the database
   dbDisconnect(DB)
   

} # end for loop

rm(Azone..,bz..,DB,dbName,m)

# add the bz to all azone data
az..$Bz <- cw[as.character(az..$AZONE), "BZONE"]

# Add aggregation tags and expand tables as needed
# First add first field
az..$sArea <- bzCW[as.character(az..$Bz), AggFields[1]] 
act..$sArea <- bzCW[as.character(act..$Bz), AggFields[1]]

# Then, if there's more than one AggField
if(length(AggFields)>1){
# Expanding tables is based on overlapping aggregation needs
# run for loop for Each Agg Field listed
for(Field in AggFields[2:length(AggFields)]){
    temp <- az..
    temp$sArea <- bzCW[as.character(temp$Bz), Field]
    az.. <- rbind(az..,temp)
    temp <- act.. 
    temp$sArea <- bzCW[as.character(temp$Bz), Field]
    act.. <- rbind(act..,temp)
} # end Field for loop 
} # end length if statement
# clean up objects
rm(Field,temp)

# remove "ignore fields"
az.. <- az..[!is.na(az..$sArea),]
act.. <- act..[!is.na(act..$sArea),]
az.. <- az..[az..$sArea != Ignore,]
act.. <- act..[act..$sArea != Ignore,]

# sum up GDP by area
act.YrScAr <- tapply(act..$Act, list(act..$Yr, act..$Sc, act..$sArea), sum)

# determine Years to Review
if(Yrs == "All") Yrs = rownames(act.YrScAr)[!is.na(rowSums(act.YrScAr))]
# ensure Yrs is a character string
Yrs <- as.character(Yrs)

Out <- round(apply(sweep(act.YrScAr[Yrs,colnames(act.YrScAr) != "Reference",], c(1,3), act.YrScAr[Yrs,"Reference",], "-")/1000000000,2:3,sum))
write.csv(t(Out), "_GDPRegionalResults.csv")
