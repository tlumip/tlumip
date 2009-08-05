#First the scenario directory needs to be set  
#Choose the main directory for the desired scenario 
 
    	baseDir <- "C:/models/TLUMIP/ED_PIFeedbackData/Base/"
	calibScenario <- "Test/Rdata/"
	refScenario <- "Reference/Rdata/"

   	yrList <- paste("t", seq(1,6, by=1), sep="")

#singleYr, function to create Rdata file of the existing csv file output by our java script.
	singleYr <- function(fileLocation, fileName){
		for(i in yrList){
			data = read.csv(paste(baseDir, fileLocation, i, "/", fileName, ".csv", sep = "")) 
			newArray = as.matrix(data[,2:ncol(data)]) 
			rownames(newArray) = data[,1] 
			str(newArray)
			assign(paste(fileName, i, sep = ""), newArray)
                 	save(list = paste(fileName, i, sep = ""), file=paste(baseDir, fileLocation, i, "/", fileName, ".RData",  sep = ""))
		}
	}

singleYr (calibScenario , "DotMapsTableIndustry")
singleYr (calibScenario , "DotMapsTableHH")
singleYr (refScenario , "DotMapsTableIndustry")
singleYr (refScenario , "DotMapsTableHH")


#- allYrs, function to tally up annual data into full set for all years
         allYrs <- function(Array, ArrayName, scenario, yr){            
            #assign(ArrayName, Array)
		str(ArrayName)
		str(Array)
#		if(length(dim(Array))<3) write.csv(get(ArrayName), paste(baseDir, scenario, ArrayName,".csv", sep=""))
#           save(list=ArrayName, file=paste(baseDir, scenario, ArrayName,".RData", sep=""))
            arrayPath <- paste(baseDir, scenario, ArrayName,"Yr.RData",sep="")
            if(file.exists(arrayPath)){
               load(arrayPath)
               Array.Yr <- get(paste(ArrayName, "Yr", sep="")) 
		   str(Array.Yr)              
               if(yr %in% dimnames(Array.Yr)[[length(dim(Array.Yr))]] ){
                 eval(parse(text=paste("Array.Yr[", paste(rep(",", 
                                 length(dim(Array.Yr))-1),collapse=""),
                                 "\"",yr,"\"] <- Array",sep="")))
               } else {
                 library(abind)
		     str(Array.Yr)
                 Array.Yr <- abind(Array.Yr, Array)
                 dimnames(Array.Yr)[[length(dim(Array.Yr))]][dim(Array.Yr)[length(dim(Array.Yr))]] <- yr
                 eval(parse(text=paste("Array.Yr <- Array.Yr[", paste(rep(",", 
                                 length(dim(Array.Yr))-1),collapse=""), 
                                 "sort(dimnames(Array.Yr)[[length(dim(Array.Yr))]])]",sep="")))               
               }               
            } else {
               Array.Yr <- array(Array, dim=c(dim(Array), 1), dimnames=c(dimnames(Array),yr))
		     str(Array.Yr)
            }
            assign(paste(ArrayName, "Yr", sep=""), Array.Yr)
            save(list=paste(ArrayName, "Yr", sep=""), file=arrayPath)
         } 

for(yr in yrList){

	# create Rdata summary folder by year for analysis storage 	
	if(!file.exists(paste(baseDir, calibScenario, yr, sep="/"))) dir.create(paste(baseDir, calibScenario, yr, sep="/")) 
	if(!file.exists(paste(baseDir, refScenario, yr, sep="/"))) dir.create(paste(baseDir, refScenario, yr, sep="/")) 

	load(paste(baseDir, calibScenario , yr, "/", "DotMapsTableIndustry.RData", sep = ""))
	allYrs(get(paste("DotMapsTableIndustry", yr, sep = "")), "DotMapsTableIndustry", calibScenario , yr) 
	
	load(paste(baseDir, refScenario, yr, "/", "DotMapsTableIndustry.RData", sep = ""))
 	allYrs(get(paste("DotMapsTableIndustry", yr, sep = "")), "DotMapsTableIndustry", refScenario, yr)
	
	load(paste(baseDir, calibScenario , yr, "/", "DotMapsTableHH.RData", sep = ""))
 	allYrs(get(paste("DotMapsTableHH", yr, sep = "")), "DotMapsTableHH", calibScenario , yr)

	load(paste(baseDir, refScenario, yr, "/", "DotMapsTableHH.RData", sep = ""))
 	allYrs(get(paste("DotMapsTableHH", yr, sep = "")), "DotMapsTableHH", refScenario, yr)
   
}


 