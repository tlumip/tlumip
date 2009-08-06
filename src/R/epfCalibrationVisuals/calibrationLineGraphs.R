# TODO: Add comment
# 
# Author: Carlee
###############################################################################

#Create line graphs function.
lineGraphs = function(fileName, outputFileName, graphTitle, legendSize){
	#Read the file in as a dataframe.
	Data <- read.table(fileName, header = TRUE, sep = ",")
	#Print the data frame to check that the file imported correctly.
	Data
	
	##Count the number of columns so they can be graphed correctly
	numberOfYears <- ncol(Data)-2
	numberOfYears	
	
	#Split the dataframe Data into new data frames based on the geographical regions.
	listOfDataFrames = by(Data, Data$Geog, function(x) x)
	#Print the new slpit data frames.
	listOfDataFrames 
		
	#Check the structure of the new data frames.
	str(listOfDataFrames)
	
	#Create the output file for the graphs. 
	pdf(outputFileName)
	
	#Loop through the new data frames and create graphs for each geographical region
	#i is a counter that allows us to move through the geographical regions
	
	for(i in 1:length(listOfDataFrames)){
		#matplot graphs a matrix.  
		matplot(t(listOfDataFrames [[i]][,3:ncol(Data)]), main = paste(names(listOfDataFrames )[i], graphTitle) , ylab = "Percent Difference", xlab = "Model Year", type="b",pch=1:nrow(listOfDataFrames), xaxt ="n")
		axis(1, at=1:numberOfYears, labels=colnames(listOfDataFrames [[i]])[3:ncol(listOfDataFrames [[i]])])        
	}
	
	#Graph, on a separte page in the pdf file the legend.
	plot(0,0, col="white", main="Legend", axes=F, xlab = "", ylab = "")
	legend("center", legend=as.character((listOfDataFrames [[i]])$Activity), 1:length((listOfDataFrames [[i]])$Activity), pch=1:nrow(listOfDataFrames), col=1:length((listOfDataFrames [[i]])$Activity), cex= legendSize)	
	
	#Turn off the pdf device.
	dev.off()	
}
#Calls the function that will produce the line graphs.
#These are the inputs the function will be looking for:
#fileName, outputFileName, graphTitle, legendSize

indLegend <- (0.60)
hhLegend <- (1.25)

lineGraphs("c:/models/TLUMIP/ED_PIFeedbackData/Base/Test/Rdata/t0/PercentChangeTableIndustry.csv", "c:/models/TLUMIP/ED_PIFeedbackData/Base/Test/Rdata/t0/PercentChangeIndustryGraphs.pdf",
		' - Employment', indLegend)
lineGraphs("c:/models/TLUMIP/ED_PIFeedbackData/Base/Test/Rdata/t0/PercentChangeTableHH.csv", "c:/models/TLUMIP/ED_PIFeedbackData/Base/Test/Rdata/t0/PercentChangeHHGraphs.pdf",
		' - HouseHolds', hhLegend)

