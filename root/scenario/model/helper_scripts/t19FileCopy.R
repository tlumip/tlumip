# Alex Bettinardi
# 11-26-18

# T19 file copy from t20 loop

#Soure Directory
sourceDir <- "outputs/t20/"
#sourceDir <- "D:/swim2/RefCheck/outputs/t19/"


##########################
# Beta zone loop
#####################

# Matrix files to work on 
Files <- c(paste("beta", apply(expand.grid(c("op","pk"),c("auto","trk1"),c("fftime","dist","time","toll")),1,paste,collapse=""),".zmx", sep=""),
           paste(c("b4","b5","b8","c4","o4","s4","w1","w4","w7","w8"),"mcls_beta.zmx",sep=""))


for(f in Files) file.copy(paste(sourceDir,f,sep=""), paste("outputs/t19/",f,sep=""),overwrite = T)

##################
# Alpha zone loop
###############################

# Matrix files to work on 
Files <- c(paste(apply(expand.grid(c("op","pk"),c("auto","trk1"),c("fftime","dist","time","toll")),1,paste,collapse=""),".zmx", sep=""),
           paste(apply(expand.grid(c("op","pk"),c("dair"),c("drv","far","fwt","ivt")),1,paste,collapse=""),".zmx", sep=""),
           paste(apply(expand.grid(c("op","pk"),c("wicr"),c("awk","ewk","far","fwt","ivt","twt","xwk")),1,paste,collapse=""),".zmx", sep=""),
           paste(apply(expand.grid(c("op","pk"),c("wt"),c("awk","brd","ewk","far","fwt","ivt","twt","xwk")),1,paste,collapse=""),".zmx", sep=""),
           paste(apply(expand.grid(c("op","pk"),c("wltf"),c("ivt","ovt")),1,paste,collapse=""),".zmx", sep=""))


for(f in Files) file.copy(paste(sourceDir,f,sep=""), paste("outputs/t19/",f,sep=""),overwrite = T)

# Three "boot strap" alpha zone files that need to be updated inaddition to the alpha skims

f <- "Employment.csv"  
file.copy(paste(sourceDir,f,sep=""), paste("outputs/t19/",f,sep=""),overwrite = T)

f <- "Increments.csv"  
file.copy(paste(sourceDir,f,sep=""), paste("outputs/t19/",f,sep=""),overwrite = T)


# clean up some trash
rm(f,Files)


