# Alex Bettinardi
# 3-11-20
# Still a working script - updating this header to test ODOT Git issues 

# T29 file copy from previous t29 run

#Soure Directory
sourceDir <- "D:/swim2/Ref25_0_03_77sdt/outputs/t29/"

# create directories as needed
if(!dir.exists("outputs")) dir.create("outputs")
if(!dir.exists("outputs/t29")) dir.create("outputs/t29")
if(!dir.exists("outputs/t28")) dir.create("outputs/t28")


##########################
# Beta zone loop
#####################

# Matrix files to work on 
Files <- c(paste("beta", apply(expand.grid(c("op","pk"),c("auto","trk1"),c("fftime","dist","time","toll")),1,paste,collapse=""),".zmx", sep=""),
           paste(c("b4","b5","b8","c4","o4","s4","w1","w4","w7","w8"),"mcls_beta.zmx",sep=""))


for(f in Files) file.copy(paste(sourceDir,f,sep=""), paste("outputs/t29/",f,sep=""),overwrite = T)

##################
# Alpha zone loop
###############################

# Matrix files to work on 
Files <- c(paste(apply(expand.grid(c("op","pk"),c("auto","trk1"),c("fftime","dist","time","toll")),1,paste,collapse=""),".zmx", sep=""),
           paste(apply(expand.grid(c("op","pk"),c("dair"),c("drv","far","fwt","ivt")),1,paste,collapse=""),".zmx", sep=""),
           paste(apply(expand.grid(c("op","pk"),c("wicr"),c("awk","ewk","far","fwt","ivt","twt","xwk")),1,paste,collapse=""),".zmx", sep=""),
           paste(apply(expand.grid(c("op","pk"),c("wt"),c("awk","brd","ewk","far","fwt","ivt","twt","xwk")),1,paste,collapse=""),".zmx", sep=""),
           paste(apply(expand.grid(c("op","pk"),c("wltf"),c("ivt","ovt")),1,paste,collapse=""),".zmx", sep=""))


for(f in Files) file.copy(paste(sourceDir,f,sep=""), paste("outputs/t29/",f,sep=""),overwrite = T)

##################
# "boot strap" alpha zone files that need to be updated inaddition to the alpha skims
##################

Files <- c("Employment.csv","Increments.csv","activity_forecast.csv","householdsByHHCategory.csv")     

for(f in Files) file.copy(paste(sourceDir,f,sep=""), paste("outputs/t29/",f,sep=""),overwrite = T)

##################
# copy previous year files
##################

Files <- c("ActivityLocations.csv","ExchangeResults.csv","MakeUse.csv")

for(f in Files)  file.copy(paste(paste0(substring(sourceDir,1,nchar(sourceDir)-2),
          as.numeric(substring(sourceDir,nchar(sourceDir)-1,nchar(sourceDir)-1))-1,"/"),
          f,sep=""), paste("outputs/t28/",f,sep=""),overwrite = T)
          

# clean up some trash
rm(f,Files,sourceDir)


