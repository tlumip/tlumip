# VolumePull.R
# Alex Bettinardi
# 7-17-17
# 8-8-19 AB - reworked to use the selectLinks.csv file instead of a seperate file

##############################################################
# a script to pull volumes (and calucate growth rates) for a handful of locations
# - as specified with an inputfile - "selectLinks.csv"
# This script produces three outputs:
# CompleteVolumeData.csv - a record of all the volumes pulled for the given 
# VolumeTrends_byDirection.pdf - a plot of the total, auto and truck volume and growth rates over time for each scenario and each link
# VolumeTrends.pdf - a plot of the total, auto and truck volume and growth rates over time for each scenario 
#                    where both directions for a given set of links have been added together (as opposed to plotted seperately)
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

# load in defined link list - selectLink.csv
sl <- read.csv("selectLinks.csv", as.is=T)
rownames(sl) <- paste(sl$FROMNODE, sl$TONODE)

# User configuratble field name references.
# Fields for plotting and grouping links
###########################################
# Route and description are used for grouping links,
# so if these two fields are identical for a set of links, those links will be summed in VolumeTrends.pdf 
RouteField <- "STATIONNUMBER"
DescField <- "HWY" # if NA (meaning field doesn't exist in input, it will be filled with blanks, see next line
if(is.na(DescField)) { sl$Desc <- ""  ; DescField <- "Desc"}
# the last field is the direction field which should be different for each direction of the same link
# this field gets plotted in the title of "VolumeTrends_byDirection.pdf"
DirField <- "Direction"

# Defining Fields of interest from SWIM
# The user could potentially change these, but it's hard to imagine a case for that.
Fields <- c("DAILY_VOL_AUTO","DAILY_VOL_TRUCK","DAILY_VOL_TOTAL")

######################
# Just code now, no more user inputs below
#########################################

# setup connection to SQLite
library(RSQLite)
m = dbDriver("SQLite")

# setup an empty table for all scenario data
link.Sc <- c()

# run a for loop to append data for all scenarios
for(dbName in names(dbs)){

   # connect to the database
   DB = dbConnect(m, dbs[dbName])
   # Pull link data
   link = dbGetQuery(DB, "SELECT * FROM LINK_DATA")

   # a little table formatting
   link$Year <- as.numeric(link$TSTEP)+1990
   link$Name <- paste(link$ANODE, link$BNODE)
   
   # sub link table to only links we care about
   link <- link[link$Name %in% rownames(sl),c("Name","Year",Fields)]
   link$Scen <- dbName
   
   # add data to all scenario table
   link.Sc <- rbind(link.Sc, link)

   # close connection to the database
   dbDisconnect(DB)
} # end for loop

# add fields to complete link data
link.Sc$Route <- sl[link.Sc$Name,RouteField]
link.Sc$Dir <- sl[link.Sc$Name,DirField]
link.Sc$Desc <-sl[link.Sc$Name,DescField]


# export "raw" data
write.csv(link.Sc, "CompleteVolumeData.csv",row.names=F)

# plot the trends by link
pdf("VolumeTrends_byDirection.pdf", width=8,height=10)
for(i in rownames(sl)){
   Data <- link.Sc[link.Sc$Name ==i,]

   # make a 2x3 plot with auto, truck, total, and then growth rates
   layout(matrix(1:6, byrow=T, ncol=2))
   par(mar=c(2,4.1,4.1,2.1), oma=c(2,2,2,2))

   # plot Total Volume
   Field <- "DAILY_VOL_TOTAL"
   sc <- names(dbs)[1]
   plot(Data[Data$Scen==sc,"Year"],Data[Data$Scen==sc,Field], type="l", col=Col[sc], xlim=c(min(Data$Year),max(Data$Year)), ylim= c(min(Data[,Field]),max(Data[,Field])),
        main="Volume Growth for All Vehicles", xlab="year", ylab="Volume")
   for(sc in names(dbs)[2:length(dbs)]) lines(Data[Data$Scen==sc,"Year"],Data[Data$Scen==sc,Field], col=Col[sc])
   legend("bottomright",names(dbs),col=Col[names(dbs)],lty=1)    
   
   y <- tapply(Data[,Field],list(Data$Year,Data$Scen),sum)
   if(sum(y[1],na.rm=T) == 0) {
      plot(0,0,type="l",xlab="",ylab="No Data",main="Annual Growth Rate for Total Traffic")
   } else {
      y <- sweep(sweep(y,2,y[1,],"/")-1,1,(as.numeric(rownames(y))-as.numeric(rownames(y))[1]),"/")*100
      matplot(as.numeric(rownames(y)),y, col=Col[colnames(y)],pch=1, ylab="Growth Rate",main="Annual Growth Rate for Total Traffic")
   }
   abline(h=0)

   # plot Auto
   Field <- "DAILY_VOL_AUTO"
   sc <- names(dbs)[1]
   plot(Data[Data$Scen==sc,"Year"],Data[Data$Scen==sc,Field], type="l", col=Col[sc], xlim=c(min(Data$Year),max(Data$Year)), ylim= c(min(Data[,Field]),max(Data[,Field])),
        main="Volume Growth for Autos", xlab="year", ylab="Auto Volume")
   for(sc in names(dbs)[2:length(dbs)]) lines(Data[Data$Scen==sc,"Year"],Data[Data$Scen==sc,Field], col=Col[sc])
   legend("bottomright",names(dbs),col=Col[names(dbs)],lty=1) 
   
   y <- tapply(Data[,Field],list(Data$Year,Data$Scen),sum)
   if(sum(y[1],na.rm=T) == 0) {
      plot(0,0,type="l",xlab="",ylab="No Data",main="Annual Growth Rate for Auto Traffic")
   } else {
      y <- sweep(sweep(y,2,y[1,],"/")-1,1,(as.numeric(rownames(y))-as.numeric(rownames(y))[1]),"/")*100
      matplot(as.numeric(rownames(y)),y, col=Col[colnames(y)],pch=1, ylab="Growth Rate",main="Annual Growth Rate for Auto Traffic")
   }
   abline(h=0)
   
   # plot Truck
   Field <- "DAILY_VOL_TRUCK"
   sc <- names(dbs)[1]
   plot(Data[Data$Scen==sc,"Year"],Data[Data$Scen==sc,Field], type="l", col=Col[sc], xlim=c(min(Data$Year),max(Data$Year)), ylim= c(min(Data[,Field]),max(Data[,Field])),
        main="Volume Growth for Trucks", xlab="year", ylab="Truck Volume")
   for(sc in names(dbs)[2:length(dbs)]) lines(Data[Data$Scen==sc,"Year"],Data[Data$Scen==sc,Field], col=Col[sc])
   legend("bottomright",names(dbs),col=Col[names(dbs)],lty=1)     
   
   y <- tapply(Data[,Field],list(Data$Year,Data$Scen),sum)
   if(sum(y[1],na.rm=T) == 0) {
      plot(0,0,type="l",xlab="",ylab="No Data",main="Annual Growth Rate for Truck Traffic")
   } else {
      y <- sweep(sweep(y,2,y[1,],"/")-1,1,(as.numeric(rownames(y))-as.numeric(rownames(y))[1]),"/")*100
      matplot(as.numeric(rownames(y)),y, col=Col[colnames(y)],pch=1, ylab="Growth Rate",main="Annual Growth Rate for Truck Traffic")
   }
   abline(h=0)
   
  
   mtext(paste(Data[1,"Route"],Data[1,"Dir"],Data[1,"Desc"]," - From To:",i), side=3, line=0, outer=TRUE, cex=1.25)   
   
}

dev.off()


# plot the trends by section
pdf("VolumeTrends.pdf", width=8,height=10)
for(i in unique(paste(sl[,RouteField], sl[,DescField]))){
   Data <- link.Sc[paste(link.Sc$Route, link.Sc$Desc) ==i,]

   # make a 2x3 plot with auto, truck, total, and then growth rates
   layout(matrix(1:6, byrow=T, ncol=2))
   par(mar=c(2,4.1,4.1,2.1), oma=c(2,2,2,2))
   
   # plot Total Volume
   z <- tapply(Data[,"DAILY_VOL_TOTAL"],list(Data$Year,Data$Scen),sum)
   matplot(as.numeric(rownames(z)),z, col=Col[colnames(z)],type="l",lty=1, main="Volume Growth for All Vehicles", xlab="year", ylab="Volume")
   legend("bottomright",names(dbs),col=Col[names(dbs)],lty=1)
   if(sum(z[1],na.rm=T) == 0) {
      plot(0,0,type="l",xlab="",ylab="No Data",main="Annual Growth Rate for Total Traffic")
   } else {
      y <- sweep(sweep(z,2,z[1,],"/")-1,1,(as.numeric(rownames(z))-as.numeric(rownames(z))[1]),"/")*100
      matplot(as.numeric(rownames(y)),y, col=Col[colnames(y)],pch=1, ylab="Growth Rate",main="Annual Growth Rate for Total Traffic")
   }
   abline(h=0)
   
   # plot Auto
   y <- tapply(Data[,"DAILY_VOL_AUTO"],list(Data$Year,Data$Scen),sum)
   matplot(as.numeric(rownames(y)),y, col=Col[colnames(y)],type="l",lty=1, main="Volume Growth for Autos", xlab="year", ylab="Auto Volume")
   legend("bottomright",names(dbs),col=Col[names(dbs)],lty=1)
   if(sum(y[1],na.rm=T) == 0) {
      plot(0,0,type="l",xlab="",ylab="No Data",main="Annual Growth Rate for Auto Traffic")
   } else {
      y <- sweep(sweep(y,2,y[1,],"/")-1,1,(as.numeric(rownames(y))-as.numeric(rownames(y))[1]),"/")*100
      matplot(as.numeric(rownames(y)),y, col=Col[colnames(y)],pch=1, ylab="Growth Rate",main="Annual Growth Rate for Auto Traffic")
   }
   abline(h=0)
   
   # plot Truck
   y <- tapply(Data[,"DAILY_VOL_TRUCK"],list(Data$Year,Data$Scen),sum)
   matplot(as.numeric(rownames(y)),y, col=Col[colnames(y)],type="l", lty=1, main="Volume Growth for Trucks", xlab="year", ylab="Truck Volume")
   legend("bottomright",names(dbs),col=Col[names(dbs)],lty=1)
   text(as.numeric(rownames(y))[1],mean(c(min(y),max(y))), paste(round(100*y/z,1)[1,names(dbs)[1]],"%",sep=""),pos=4)
   text(as.numeric(rownames(y))[nrow(y)],y[nrow(y),], paste(round(100*y/z,1)[nrow(y),],"%",sep=""),pos=2)
   text(as.numeric(rownames(y))[1], max(y), "Change in Truck% from Base to Future", pos=4)
   if(sum(y[1], na.rm=T) == 0) {
      plot(0,0,type="l",xlab="",ylab="No Data",main="Annual Growth Rate for Truck Traffic")
   } else {
      y <- sweep(sweep(y,2,y[1,],"/")-1,1,(as.numeric(rownames(y))-as.numeric(rownames(y))[1]),"/")*100
      matplot(as.numeric(rownames(y)),y, col=Col[colnames(y)],pch=1, ylab="Growth Rate",main="Annual Growth Rate for Truck Traffic")
   }
   abline(h=0)
   
   mtext(i, side=3, line=0, outer=TRUE, cex=1.25)   
   
}

dev.off()
rm(Data, DB, dbName, Field, i, sc, y, z,link, Fields)