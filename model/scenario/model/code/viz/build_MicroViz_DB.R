
#script to build the swim micro vizualization db
#R --no-save < ../../build_microviz_db.R > logMicroDB.txt INDCODEFILE="z:/models/oregon2tm/parameters/IndustryOccupationSplitIndustryCorrespondence.csv"
#Ben Stabler, stabler@pbworld.com, 072210

#######################################################################

library(RSQLite)

#########################################################################
#parameters
#########################################################################

INDCODEFILE = Sys.getenv("INDCODEFILE")

#########################################################################
#calculated parameters
#########################################################################

#scenario name and time step
wkrDir = getwd()
sn = basename(dirname(dirname(wkrDir)))
tstep = gsub("t","",basename(wkrDir))

#is this a TS year
isTSYear = file.exists("ampeakAssignmentResults.csv")

#########################################################################
#Exit if NOT a TS Year
#########################################################################
  
#exit if NOT TS year
if(!isTSYear) {
  cat(paste("No database created since not a TS year ", Sys.time(), "\n"))
  quit("no")
}

#########################################################################
#create database
#########################################################################

#create scenario database
m = dbDriver("SQLite")
databaseFileName = paste(sn, "_t", tstep, "_micro.db", sep="") #micro
cat(paste("Create SWIM MICRO VIZ DB for", databaseFileName, "at", Sys.time(), "\n"))
if(file.exists(databaseFileName)) { 
  file.remove(databaseFileName) 
}
file.create(databaseFileName)

db = dbConnect(m, dbname = databaseFileName)

#########################################################################
#Read HH and Person Tables
#########################################################################

#do not need to read SynPopP.csv, SynPopH.csv, and Patterns_SDT.csv
#since all their data are also in householdData.csv and personData.csv

hh = read.csv("householdData.csv")
per = read.csv("personData.csv")

#read industry recoding table
indCodes = read.csv(INDCODEFILE)

#########################################################################
#Clean Up HH and PERSON Database Schema
#########################################################################

LDT_Tour_Patterns = c("COMPLETE_TOUR","BEGIN_TOUR","END_TOUR","AWAY","NO_TOUR")

hh = hh[,!(colnames(hh) %in% c("PERSONS"))]
hh$LD_HOUSEHOLD_PATTERN = factor(hh$LD_HOUSEHOLD_PATTERN, labels=LDT_Tour_Patterns)

per$MEMBER_ID = per$memberID 
per = per[,!(colnames(per) %in% c("memberID","home_taz","LD_INDICATOR_HOUSEHOLD","LD_TOUR_PATTERN_HOUSEHOLD"))]
per$LD_TOUR_PATTERN_WORKRELATED = factor(per$LD_TOUR_PATTERN_WORKRELATED, labels=LDT_Tour_Patterns)
per$LD_TOUR_PATTERN_OTHER = factor(per$LD_TOUR_PATTERN_OTHER, labels=LDT_Tour_Patterns)

#recode with commodity label from com.pb.tlumip.pt.ptoccupation.java
levels(per$SW_OCCUP) = 
  c("1a_Health","1_ManPro","7_NonOfc","0_NoOccupation","6_OthR&C","3_OthTchr","2_PstSec","4_OthP&T","5_RetSls")

#recode with split industry codes
per$SW_SPLIT_IND = indCodes$SplitIndustry[match(per$SW_SPLIT_IND, indCodes$SplitIndustryIndex)]
per$SW_SPLIT_IND = gsub(" |&","_",as.character(per$SW_SPLIT_IND)) #replace " " & "&"

#Calculate HH age of head MEMBER_ID==1
hh$AGEOFHEAD = per$AGE[per$MEMBER_ID==1][match(hh$HH_ID, per$HH_ID[per$MEMBER_ID==1])]

#Write tables to DB
colnames(hh) = toupper(colnames(hh))
dbWriteTable(db, "HH", hh, row.names=F)
dbGetQuery(db, "CREATE INDEX HHIndex ON HH (HH_ID)")

colnames(per) = toupper(colnames(per))
dbWriteTable(db, "PER", per[,c(1,21,2:20)], row.names=F)
dbGetQuery(db, "CREATE INDEX PERIndex ON PER (HH_ID, MEMBER_ID)")

#########################################################################
#Read LDT Tour and Trip Tables
#########################################################################

#Trips_LDTPerson.csv contains non-vehicle trips as well.  For example, 
#mode AIR trips are converted to "drive to/from the airport" trips in Trips_LDTVehicle.csv.

tours_ldt = read.csv("Tours_LDT.csv")
tours_ldt$HH_ID = tours_ldt$hhID 
tours_ldt$MEMBER_ID = tours_ldt$memberID 
tours_ldt = tours_ldt[,!(colnames(tours_ldt) %in% 
  c("hhID","memberID","destinationType","tripMode","income","home"))]

trips_ldt_vehicle = read.csv("Trips_LDTVehicle.csv")
trips_ldt_vehicle$HH_ID = trips_ldt_vehicle$hhID 
trips_ldt_vehicle$MEMBER_ID = trips_ldt_vehicle$memberID 
trips_ldt_vehicle = trips_ldt_vehicle[,!(colnames(trips_ldt_vehicle) %in% 
  c("hhID","memberID","income","tourPurpose","tourMode","vehicleTrip"))]

#Write tables to DB
colnames(tours_ldt) = toupper(colnames(tours_ldt))
dbWriteTable(db, "TOUR_LDT_MICRO", tours_ldt[,c(13:14,1:12)], row.names=F)
colnames(trips_ldt_vehicle) = toupper(colnames(trips_ldt_vehicle))
dbWriteTable(db, "TRIP_LDT_MICRO", trips_ldt_vehicle[,c(9:10,1:8)], row.names=F)

#########################################################################
#Read SDT Tour and Trip Tables
#########################################################################

tours_sdt = read.csv("Tours_SDT.csv")
tours_sdt$HH_ID = tours_sdt$hhID 
tours_sdt$MEMBER_ID = tours_sdt$memberID 
tours_sdt$TOURID = tours_sdt$"tour."
tours_sdt$TOURPURPOSE = tours_sdt$activityPurpose.2
tours_sdt$PRIMARYDESTINATION = tours_sdt$location.2
tours_sdt$STARTTIME = tours_sdt$endTime
tours_sdt$ENDTIME = tours_sdt$endTime.4
tours_sdt = tours_sdt[,!(colnames(tours_sdt) %in% 
  c("hhID","memberID","personAge","tour.","weekdayTour.yes.no.","tripMode","departDist"))]
tours_sdt = tours_sdt[,!(colnames(tours_sdt) %in% 
  c("activityPurpose","startTime","endTime","timeToActivity","distanceToActivity","location",
    "activityPurpose.1","startTime.1","endTime.1","timeToActivity.1","distanceToActivity.1",
    "tripMode.1","location.1","activityPurpose.2","startTime.2","endTime.2","timeToActivity.2",
    "distanceToActivity.2","tripMode.2","location.2","activityPurpose.3","startTime.3",
    "endTime.3","timeToActivity.3","distanceToActivity.3","tripMode.3","location.3",
    "activityPurpose.4","startTime.4","endTime.4","timeToActivity.4","distanceToActivity.4",
    "tripMode.4","location.4"))]

trips_sdt = read.csv("Trips_SDTPerson.csv")
trips_sdt$HH_ID = trips_sdt$hhID 
trips_sdt$MEMBER_ID = trips_sdt$memberID 
trips_sdt$TOURID = trips_sdt$"tour."
trips_sdt$ISSUBTOUR = trips_sdt$"subTour.yes.no"
trips_sdt = trips_sdt[,!(colnames(trips_sdt) %in% 
  c("hhID","memberID","tour.","subTour.yes.no.","weekdayTour.yes.no.",
  "tourPurpose","tourSegment","tourMode","income","age","enroll","esr"))]

#Write tables to DB
colnames(tours_sdt) = toupper(colnames(tours_sdt))
dbWriteTable(db, "TOUR_SDT_MICRO", tours_sdt[,c(4:6,1:3,7:10)], row.names=F)
colnames(trips_sdt) = toupper(colnames(trips_sdt))
dbWriteTable(db, "TRIP_SDT_MICRO", trips_sdt[,c(9:12,1:8)], row.names=F)

#########################################################################
#Read CT Trip Tables
#########################################################################

#truck trips
trips_ct = read.csv("Trips_CTTruck.csv")
trips_ct$commodity = sprintf("SCTG%02i",trips_ct$commodity) 

#add production alpha zone
homeZone = tapply(trips_ct$origin,trips_ct$truckID,function(x)x[1]) #assumes trips written in order
trips_ct$ProdZone = homeZone[match(trips_ct$truckID,names(homeZone))]

#drop useless fields
trips_ct = trips_ct[,!(colnames(trips_ct) %in% c("tourMode","tripMode"))]
 
#Write tables to DB
colnames(trips_ct) = toupper(colnames(trips_ct))
colOrder = c("TRUCKID","ORIGIN","TRIPSTARTTIME","DESTINATION","TRUCKTYPE","CARRIERTYPE","COMMODITY","WEIGHT","DISTANCE","PRODZONE")
dbWriteTable(db, "TRIP_CT_MICRO", trips_ct[,colOrder], row.names=F)

#########################################################################
#Close and compact database
#########################################################################

#Close and compact database
dbGetQuery(db, "VACUUM")
sqliteCloseConnection(db)
cat(paste("SWIM MICRO VIZ DB for", databaseFileName, "at", Sys.time(), "Created \n"))
quit("no")


