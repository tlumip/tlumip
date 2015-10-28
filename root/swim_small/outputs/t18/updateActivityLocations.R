x <- read.csv("ActivityLocations - Copy.csv",as.is=T, colClasses=c(rep("character",2),"numeric",rep("character",4),"numeric",rep("character",3)))
 x[x=="-Inf"]="-Infintiy"
 x$Quantity= x$Quantity /1000 
x$ConstraintValue = x$ConstraintValue/1000
write.csv(x,"ActivityLocations.csv",quote=F,row.names=F)