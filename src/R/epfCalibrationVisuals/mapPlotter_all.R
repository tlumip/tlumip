#=========================
#matPlotte.R
#============
 
#**Author:** Alex Bettinardi  
#**Contact:** alexander.o.bettinardi@odot.state.or.us  
#**Date:** 12/08/08  
#**Revisions:** Third Draft  
#**License:** GPL2  

#Description of Plotter
#=========

#This script is designed to compare scenarios against the reference scenario.
#Multiple inputs need to be set up at the beginning of this script for specific
#new scenarios.  This script should be copied to the scenario Analysis folder
#for the specific scenario being compared.  After the script is copied, 
#the users upfront need to be updated as approriate.

#In the middel of this script are the functions that create the comparison plots.

#At the end of the script are the calls to these functions that specify which
#data to plot.  If more or less plots are desired, function calls can be removed
#or added at the end of this script.
 
#Define User Inputs / pointers
#====================

#Directory Pointers
#------

     # All the analysis summary tables for all scenarios are located in this dir
     baseDir <- "C:/models/TLUMIP/ED_PIFeedbackData/Base/"

     # Refernce Directory, which will be compared to
     refDir <- "Reference"
     refDir <- paste(baseDir, refDir, sep="")
     
     # Directory to comapare to the reference
     scenDir <- "Test"
     scenDir <- paste(baseDir, scenDir, sep="")

#Inputs to customize plots
#----     

     # Main Title for the plots
     HeadTitle <- "Test:\n"
     
     # if you want to highlight a scenario/project area
     hi <- F

     # if you want Summary Statistics by subArea in bottom left corner
     summaryText <- T
     
     # Statisicts summerized by subArea field found in Beta shapefile
     GeoArea <- "MPOmodeled"
     
     # Zoom level
     Zoom <- list()
     # Shapefile set
     shapeGroup <- "HighDetail_All"
     # Page dimentions
     WH <- list(w=8, h=8)
     
#Set up directories need to save the map plots
#---
     
     mapFolder <- paste(scenDir, "/", GeoArea, "Plots",sep="")
     if(!file.exists(mapFolder)) dir.create(mapFolder)    
     mapFolder <- paste(mapFolder, "/All_Maps", sep="")
     if(!file.exists(mapFolder)) dir.create(mapFolder)     

#Source in Map Plotting Functions
#====

     source("C:/models/TLUMIP/ED_PIFeedbackData/Base/resultScripts/DoNotUsemapPlotte.R")
     
#Load shapefiles needed
#====
                  
     library("maptools")
     options(warn=-1)
     if(hi) {
        loadShapefiles(c("zonesShape", "regShape", "roadShape", "stShape", "exRoadShape", "eraseShape", "hiLight")  , shapeGroup)
     } else {
        loadShapefiles(c("zonesShape", "regShape", "roadShape", "stShape", "exRoadShape", "eraseShape")  , shapeGroup)
     }
     
     # extract the zone attribute data
     zonesData <- zonesShape$att.data

     # convert to polygon file for mapping
     zonesPoly <- Map2poly(zonesShape, zonesShape$att.data$ZONE)
     regPoly <- Map2poly(regShape)
     if(exists("exRoadShape")){
        roadLines <- c(Map2lines(roadShape), Map2lines(exRoadShape))
     } else {
        roadLines <- Map2lines(roadShape)
     }
     stLines <- Map2lines(stShape)
     whitePoly <- Map2poly(eraseShape)
     if(hi) hiLines <- Map2lines(hiLight)

     # make an index vector to the taz
     zonesIndex <- as.character(zonesData$BETATAZ)
     
     # clean up files
     rm(zonesShape, regShape, roadShape, stShape, exRoadShape, eraseShape)
     if(hi) rm(hiLight)
     options(warn=0)
     
#Update GeoOrder
#---          
     if(!exists("GeoOrder")) GeoOrder <- sort(unique(zonesData[,GeoArea]))    
     GeoOrder <- GeoOrder[GeoOrder %in% unique(zonesData[,GeoArea])]
       
#Calls to the loadPM function to make plots by Measure
#===
 
    	loadPM("DotMapsTableIndustryYr", paste(HeadTitle,"Employment", sep=""), tot=T)
	loadPM("DotMapsTableHHYr", paste(HeadTitle,"House Holds", sep=""), tot=T, Unit = "House Holds")
	
     # clean up
     if(hi) rm(hiLines)
     rm(baseDir, GeoArea, GeoOrder, HeadTitle, hi, linePlotter, loadPM, mapFolder, plotter, refDir, regPoly,
        roadLines, scenDir, simpleDotsInPoly, stLines, summaryText, WH, whitePoly, zonesData, zonesIndex, zonesPoly, Zoom)

        