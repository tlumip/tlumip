shrinkList <- list.files(path="Orig")



for(f in shrinkList){
   x <- read.csv(paste("Orig/",f,sep=""))
   if("dollars" %in% names(x)) x[,"dollars"] <- round(x[,"dollars"]/1000)
   if(f == "activity_forecast.csv" ) x[,c("employment","output")] <- round(x[,c("employment","output")]/1000)
   if(f ==  "population_forecast.csv" ) x[,"population"] <- round(x[,"population"]/1000)
   write.csv(x,f,row.names=F)
}