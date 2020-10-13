# Alex Bettinardi
# 3-11-20
# Updated by nagendra.dhakar@rsginc.com (10-12-2020)
# t{validationYear} file copy from previous t{validationYear} run

##User Inputs
#Soure Directory - the run with scenario until the validation year
dataDir <- "D:/Projects/Clients/OR_DOT/SWIM_WOC7/Models/BaseYear/Development/tlumip_ver26/root/scenario/outputs"
validationYear <- 29 #2019
aaBaseYear <- 19 #should not be changed unless "aa.base.year" in globalTemplate.properties is changed.

##Automatic Process
tValYear <- paste("t", toString(validationYear), sep='')
sourceDir <- paste(dataDir, tValYear, sep='/')
print(sourceDir)

prevYear <- validationYear-1 
tPrevYear <- paste("t", toString(prevYear), sep='')
print(tPrevYear)

tAABaseYear <- paste("t", toString(aaBaseYear), sep='')
print(tAABaseYear)

# create directories as needed
if(!dir.exists("outputs")) dir.create("outputs")
if(!dir.exists(paste("outputs", tAABaseYear, sep="/"))) dir.create(paste("outputs", tAABaseYear, sep="/"))
if(!dir.exists(paste("outputs",tValYear, sep="/"))) dir.create(paste("outputs",tValYear, sep="/"))
if(!dir.exists(paste("outputs",tPrevYear, sep="/"))) dir.create(paste("outputs",tPrevYear, sep="/"))

##########################
# Beta zone loop
#####################

# Matrix files to work on 
Files <- c(paste("beta", apply(expand.grid(c("op","pk"),c("auto","trk1"),c("fftime","dist","time","toll")),1,paste,collapse=""),".zmx", sep=""),
           paste(c("b4","b5","b8","c4","o4","s4","w1","w4","w7","w8"),"mcls_beta.zmx",sep=""))


for(f in Files) file.copy(paste(sourceDir, f, sep="/"), paste("outputs", tValYear, f, sep="/"), overwrite = T)

##################
# Alpha zone loop
###############################

# Matrix files to work on 
Files <- c(paste(apply(expand.grid(c("op","pk"),c("auto","trk1"),c("fftime","dist","time","toll")),1,paste,collapse=""),".zmx", sep=""),
           paste(apply(expand.grid(c("op","pk"),c("dair"),c("drv","far","fwt","ivt")),1,paste,collapse=""),".zmx", sep=""),
           paste(apply(expand.grid(c("op","pk"),c("wicr"),c("awk","ewk","far","fwt","ivt","twt","xwk")),1,paste,collapse=""),".zmx", sep=""),
           paste(apply(expand.grid(c("op","pk"),c("wt"),c("awk","brd","ewk","far","fwt","ivt","twt","xwk")),1,paste,collapse=""),".zmx", sep=""),
           paste(apply(expand.grid(c("op","pk"),c("wltf"),c("ivt","ovt")),1,paste,collapse=""),".zmx", sep=""))

for(f in Files) {
	print(f)
	file.copy(paste(sourceDir, f, sep="/"), paste("outputs", tValYear, f, sep="/"), overwrite = T)
}

##################
# "boot strap" alpha zone files that need to be updated inaddition to the alpha skims
##################

Files <- c("Employment.csv","Increments.csv","activity_forecast.csv","householdsByHHCategory.csv")     

for(f in Files) file.copy(paste(sourceDir, f, sep="/"), paste("outputs", tValYear, f, sep="/"), overwrite = T)

# other (NED, and ALD) files to work on 
Files <- c("ActivitySummary.csv","activity_forecast.csv","construction_forecast.csv","population_forecast.csv","trade_forecast.csv","government_forecast.csv",
			"ExchangeResults.csv", "ActivityLocations.csv", "MakeUse.csv", "FloorspaceInventory.csv", "ExchangeResultsTotals.csv")

for(f in Files) {
	print(f)
	file.copy(paste(sourceDir, f, sep="/"), paste("outputs", tValYear, f, sep="/"), overwrite = T)
}

##################
# copy previous year files
##################

Files <- c("ActivityLocations.csv","ExchangeResults.csv","MakeUse.csv")

for(f in Files) {
	print(f)
	print(paste(dataDir, tPrevYear, f, sep='/'))
	file.copy(paste(dataDir, tPrevYear, f, sep='/'), paste("outputs", tPrevYear, f, sep="/"), overwrite = T)
}
          
# AA files (t19 is hard coded as the AA base year)
Files <- c("ActivityTotalsW.csv","MakeUse.csv")
for(f in Files) {
	print(f)
	file.copy(paste(dataDir, tAABaseYear, f,sep="/"), paste("outputs", tAABaseYear, f, sep="/"), overwrite = T)
}

# clean up some trash
rm(f,Files,sourceDir)


