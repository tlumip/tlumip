# PopEmpPull.R
# Alex Bettinardi
# 9-29-17 - first draft
# 9-19-19 - re-developed / designed
# 7-27-20 - updated a small percent change color plotting error

##############################################################
# a script to pull population and employment data (and calucate growth rates) input regions
# - as specified with an inputfile - "selectedRegions.csv"
# This script produces two outputs:
# CompletePopEmpData.csv - a record of all the volumes pulled for the given regions 
# PopEmp_Growth.pdf - a plot of the total, population and employment and 
#                     growth rates over time for each scenario and each region
################################################################

# First define scenarios to be pulled
# build a list to the various completed SWIM databases to be compared
#####################################################################
dbs <- c("//xd6420thing1/d/swim2/Ref25_0_01/outputs/Ref25_0_01.db")

# Give each SWIM database pointer a short and meaningfull name 
##############################################################
names(dbs) <- c("Reference")          #,"Both_Toll_noBridge","I205_Toll","I205_Toll_noBridge","I5_Toll","I5_Toll_noBridge", "Lane_Added_Both", "Lane_Added_I205")
# Choose a color for each scenario for the produced plots
#########################################################
Col <- c("firebrick1")  #,"mediumblue","indianred1", "darkorange2","orange","violetred4","violet", "mediumblue","turquoise1")
names(Col) <- names(dbs)

# load in defined region list - selectedRegions.csv
sr <- read.csv("selectedRegions.csv", as.is=T)

######################
# Just code now, no more user inputs below
#########################################

# function to get land use data for zones of interest
rgPopEmp <- function(zns){
   zn.. <- azone[as.character(azone$AZONE) %in% zns,c("AZONE","POPULATION","EMPLOYMENT","TSTEP","SCEN")]  
   #cbind(POP=tapply(zn..$POPULATION, zn..$TSTEP,sum, na.rm=T),EMP=tapply(zn..$EMPLOYMENT, zn..$TSTEP,sum,na.rm=T)) # old logic for one scenario
   Pop <- tapply(zn..$POPULATION, list(zn..$TSTEP,zn..$SCEN),sum, na.rm=T)
   Emp <- tapply(zn..$EMPLOYMENT, list(zn..$TSTEP,zn..$SCEN),sum, na.rm=T)
   Labels <- expand.grid(dimnames(Pop),stringsAsFactors = FALSE)
   names(Labels) <- c("Year","Scen")
   cbind(Pop=as.vector(Pop),Emp=as.vector(Emp),Labels)
} 

# setup connection to SQLite
library(RSQLite)
m = dbDriver("SQLite")

# setup an empty table for all scenario data
azone <- c()

# run a for loop to append data for all scenarios
for(dbName in names(dbs)){

   # connect to the database
   DB = dbConnect(m, dbs[dbName])
   # Pull zone data
   az = dbGetQuery(DB, "SELECT * FROM AZONE")
   az$TSTEP <- as.numeric(az$TSTEP)+1990
   az$SCEN <- dbName
   
   aFields <-  dbGetQuery(DB, "SELECT * FROM ALLZONES")  
   
   # add data to all scenario table
   azone <- rbind(azone, az)

   # close connection to the database
   dbDisconnect(DB)
} # end for loop

rm(az,DB,dbName,m)

###############################
#  plot all the regional Pop / Emp data
###################################

pdf("PopEmp_Growth.pdf", width=10,height=8)
 
# make a 2x1 plot with auto, truck, total, and then growth rates
layout(matrix(1:2, byrow=T, ncol=2))
par(mar=c(2,4.1,4.1,2.1), oma=c(2,2,2,2))
   
    
# Create a keeper for all region data
Rg.. <- c() 
   
# run for loop to aggregate and plot all regions defined in selectedRegions.csv
for(rg in 1:nrow(sr)){
   
   # first determine if area or zones are defined
   if(tolower(sr[rg,"AggField"]) == "none"){
      Data <- rgPopEmp(unlist(strsplit(sr$Identifier[rg], " ")))
   } else {
      Data <- rgPopEmp(aFields[aFields[,sr[rg,"AggField"]]==sr[rg,"Identifier"],"Azone"])
   } 

   # Plotting functions  
   # Population  
   y <- tapply(Data[,"Pop"],list(Data$Year,Data$Scen),sum)    
   matplot(as.numeric(rownames(y)),y, col=Col[colnames(y)],type="l", lty=1, main="Population Growth", xlab="Year", ylab="Population")
   text(as.numeric(rownames(y))[nrow(y)],y[nrow(y),], paste(round(100*((y[nrow(y),]/y[1,]-1))/sum(diff(as.numeric(rownames(y)))),1),"%",sep=""),pos=2,col=Col[colnames(y)])
   text(as.numeric(rownames(y))[1], max(y), "Overall Annual Growth Rate", pos=4)

   # Employment
   y <- tapply(Data[,"Emp"],list(Data$Year,Data$Scen),sum)    
   matplot(as.numeric(rownames(y)),y, col=Col[colnames(y)],type="l", lty=1, main="Employment Growth", xlab="Year", ylab="Employment")
   legend("bottomright",names(dbs),col=Col[names(dbs)],lty=1)
   text(as.numeric(rownames(y))[nrow(y)],y[nrow(y),], paste(round(100*((y[nrow(y),]/y[1,]-1))/sum(diff(as.numeric(rownames(y)))),1),"%",sep=""),pos=2,col=Col[colnames(y)])
   text(as.numeric(rownames(y))[1], max(y), "Overall Annual Growth Rate", pos=4)

   # Final Title
   mtext(paste(sr[rg,"Region_Name"],"Area Growth"), side=3, line=0, outer=TRUE, cex=1.25) 

   # build a total list of regional values used in plotting
   Data$Region_Name <- sr[rg,"Region_Name"]
   Rg.. <- rbind(Rg..,Data)

}

rm(Data,rg, y)
 
 dev.off() 
 
# export "raw" data
write.csv(Rg.., "CompletePopEmpData.csv",row.names=F)
